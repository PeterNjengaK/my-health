package com.peter.myhealth;

import android.annotation.TargetApi;
import android.content.*;
import android.content.pm.PackageManager;
import android.health.connect.*;
import android.health.connect.datatypes.*;
import android.os.OutcomeReceiver;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

@TargetApi(34)
final class HealthSync {
  static final String[] PERMISSIONS = {
    "android.permission.health.READ_STEPS",
    "android.permission.health.READ_SLEEP",
    "android.permission.health.READ_EXERCISE",
    "android.permission.health.READ_WEIGHT",
    "android.permission.health.READ_HEART_RATE",
    "android.permission.health.READ_BLOOD_PRESSURE",
    "android.permission.health.READ_BLOOD_GLUCOSE",
    "android.permission.health.READ_BODY_TEMPERATURE"
  };
  private static final ExecutorService executor = Executors.newSingleThreadExecutor();
  private static boolean running;
  private final Context c;
  private final HealthConnectManager manager;
  private int imported, failures;
  private final ZoneId zone = ZoneId.systemDefault();
  private final LocalDate start = LocalDate.now().minusDays(29);
  private final Class<?>[] types = {
    SleepSessionRecord.class,
    ExerciseSessionRecord.class,
    WeightRecord.class,
    HeartRateRecord.class,
    BloodPressureRecord.class,
    BloodGlucoseRecord.class,
    BodyTemperatureRecord.class
  };

  private HealthSync(Context c) {
    this.c = c.getApplicationContext();
    manager = c.getSystemService(HealthConnectManager.class);
  }

  static synchronized void sync(Context c) {
    if (running) return;
    boolean allowed = false;
    for (String p : PERMISSIONS)
      if (c.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED) allowed = true;
    if (!allowed) {
      Store.prefs(c)
          .edit()
          .putString("syncStatus", "No access granted. Connect to choose data permissions.")
          .apply();
      return;
    }
    HealthSync task = new HealthSync(c);
    if (task.manager == null) {
      Store.prefs(c)
          .edit()
          .putString("syncStatus", "Health Connect is unavailable on this device")
          .apply();
      return;
    }
    running = true;
    Store.prefs(c)
        .edit()
        .putBoolean("healthEnabled", true)
        .putString("syncStatus", "Syncing the last 30 days…")
        .apply();
    task.steps(0);
  }

  private boolean allowed(int permission) {
    return c.checkSelfPermission(PERMISSIONS[permission]) == PackageManager.PERMISSION_GRANTED;
  }

  private TimeInstantRangeFilter range(Instant start, Instant end) {
    return new TimeInstantRangeFilter.Builder().setStartTime(start).setEndTime(end).build();
  }

  private void steps(int day) {
    if (!allowed(0) || day == 30) {
      records(0, -1);
      return;
    }
    LocalDate date = start.plusDays(day);
    Instant begin = date.atStartOfDay(zone).toInstant(),
        end = date.plusDays(1).atStartOfDay(zone).toInstant();
    if (end.isAfter(Instant.now())) end = Instant.now();
    AggregateRecordsRequest<Long> request =
        new AggregateRecordsRequest.Builder<Long>(range(begin, end))
            .addAggregationType(StepsRecord.STEPS_COUNT_TOTAL)
            .build();
    try {
      manager.aggregate(
          request,
          executor,
          new OutcomeReceiver<AggregateRecordsResponse<Long>, HealthConnectException>() {
            public void onResult(AggregateRecordsResponse<Long> result) {
              Long n = result.get(StepsRecord.STEPS_COUNT_TOTAL);
              Store.prefs(c)
                  .edit()
                  .putInt(
                      "importSteps:" + date, (int) Math.min(Integer.MAX_VALUE, n == null ? 0 : n))
                  .apply();
              steps(day + 1);
            }

            public void onError(HealthConnectException e) {
              failures++;
              steps(day + 1);
            }
          });
    } catch (Exception e) {
      failures++;
      records(0, -1);
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends android.health.connect.datatypes.Record> void records(
      int typeIndex, long token) {
    if (typeIndex >= types.length) {
      done();
      return;
    }
    if (!allowed(typeIndex + 1)) {
      records(typeIndex + 1, -1);
      return;
    }
    Class<T> type = (Class<T>) types[typeIndex];
    ReadRecordsRequestUsingFilters.Builder<T> b =
        new ReadRecordsRequestUsingFilters.Builder<>(type)
            .setTimeRangeFilter(range(start.atStartOfDay(zone).toInstant(), Instant.now()))
            .setPageSize(1000);
    if (token >= 0) b.setPageToken(token);
    try {
      manager.readRecords(
          b.build(),
          executor,
          new OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException>() {
            public void onResult(ReadRecordsResponse<T> result) {
              for (T record : result.getRecords())
                try {
                  upsert(record);
                } catch (Exception e) {
                  failures++;
                }
              if (result.getNextPageToken() != -1) records(typeIndex, result.getNextPageToken());
              else records(typeIndex + 1, -1);
            }

            public void onError(HealthConnectException e) {
              failures++;
              records(typeIndex + 1, -1);
            }
          });
    } catch (Exception e) {
      failures++;
      records(typeIndex + 1, -1);
    }
  }

  private double round(double v) {
    return Math.round(v * 100) / 100.0;
  }

  private void upsert(android.health.connect.datatypes.Record record) throws Exception {
    String sourceId = record.getMetadata().getId(),
        id =
            UUID.nameUUIDFromBytes(
                    ("hc:" + sourceId).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .toString();
    if (Store.prefs(c).getStringSet("deletedHealth", Collections.emptySet()).contains(id)) return;
    Instant date =
        record instanceof InstantRecord
            ? ((InstantRecord) record).getTime()
            : ((IntervalRecord) record).getStartTime();
    JSONObject e =
        new JSONObject()
            .put("id", id)
            .put("source", "healthConnect")
            .put("origin", record.getMetadata().getDataOrigin().getPackageName())
            .put("healthId", sourceId)
            .put("created", date.toEpochMilli())
            .put("date", date.atZone(zone).toLocalDate().toString())
            .put("note", "Imported from Health Connect");
    if (record instanceof SleepSessionRecord r) {
      long ms = Duration.between(r.getStartTime(), r.getEndTime()).toMillis();
      for (SleepSessionRecord.Stage s : r.getStages())
        if (s.getType() == SleepSessionRecord.StageType.STAGE_TYPE_AWAKE
            || s.getType() == SleepSessionRecord.StageType.STAGE_TYPE_AWAKE_IN_BED
            || s.getType() == SleepSessionRecord.StageType.STAGE_TYPE_AWAKE_OUT_OF_BED)
          ms -= Duration.between(s.getStartTime(), s.getEndTime()).toMillis();
      e.put("type", "sleep")
          .put("value", round(Math.max(0, ms) / 3600000.0))
          .put("unit", "hrs")
          .put("label", "Wearable sleep")
          .put("date", r.getEndTime().atZone(zone).toLocalDate().toString());
    } else if (record instanceof ExerciseSessionRecord r)
      e.put("type", "workout")
          .put("label", r.getTitle() == null ? "Imported workout" : r.getTitle().toString())
          .put(
              "value",
              round(Duration.between(r.getStartTime(), r.getEndTime()).toMillis() / 60000.0))
          .put("unit", "min");
    else if (record instanceof WeightRecord r)
      e.put("type", "weight")
          .put("value", round(r.getWeight().getInGrams() / 1000))
          .put("unit", "kg");
    else if (record instanceof HeartRateRecord r) {
      if (r.getSamples().isEmpty()) return;
      double avg =
          r.getSamples().stream()
              .mapToLong(HeartRateRecord.HeartRateSample::getBeatsPerMinute)
              .average()
              .orElse(0);
      e.put("type", "heartrate")
          .put("value", round(avg))
          .put("unit", "bpm")
          .put("label", "Session average heart rate");
    } else if (record instanceof BloodPressureRecord r)
      e.put("type", "bloodpressure")
          .put("value", round(r.getSystolic().getInMillimetersOfMercury()))
          .put("diastolic", round(r.getDiastolic().getInMillimetersOfMercury()))
          .put("unit", "mmHg");
    else if (record instanceof BloodGlucoseRecord r)
      e.put("type", "glucose")
          .put("value", round(r.getLevel().getInMillimolesPerLiter()))
          .put("unit", "mmol/L");
    else if (record instanceof BodyTemperatureRecord r)
      e.put("type", "temperature")
          .put("value", round(r.getTemperature().getInCelsius()))
          .put("unit", "°C");
    else return;
    synchronized (Store.class) {
      JSONObject d = Store.data(c);
      JSONArray a = d.getJSONArray("entries");
      int index = -1;
      for (int n = 0; n < a.length(); n++)
        if (a.getJSONObject(n).getString("id").equals(id)) {
          index = n;
          break;
        }
      if (index >= 0) {
        if (a.getJSONObject(index).optBoolean("edited")) return;
        a.put(index, e);
      } else {
        a.put(e);
        imported++;
      }
      Store.save(c, d);
    }
  }

  private void done() {
    Store.prefs(c)
        .edit()
        .putLong("lastSync", System.currentTimeMillis())
        .putString(
            "syncStatus",
            "Synced · "
                + imported
                + " new records"
                + (failures > 0 ? " · Some data could not be read" : ""))
        .apply();
    StepWidget.update(c);
    synchronized (HealthSync.class) {
      running = false;
    }
  }
}
