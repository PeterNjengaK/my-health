package com.peter.myhealth;

import android.app.*;
import android.content.*;
import android.os.*;
import java.util.*;
import org.json.*;

/** On-device tests with no JUnit or test-framework runtime dependency. */
public class IntegrationChecks extends Instrumentation {
  private int checks;

  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  private void check(boolean condition, String label) {
    if (!condition) throw new AssertionError(label);
    checks++;
  }

  public void onStart() {
    Bundle result = new Bundle();
    try {
      Context c = getTargetContext();
      Activity screen =
          startActivitySync(
              new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      Thread.sleep(600);
      if (c.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
          == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        check(StepService.running, "Tracking starts automatically without an app toggle");
        runOnMainSync(
            () -> {
              c.startForegroundService(new Intent(c, StepService.class));
              c.startForegroundService(new Intent(c, StepService.class));
            });
        Thread.sleep(400);
        check(StepService.running, "Repeated starts keep the existing sensor listener");
      }
      JSONObject before = Store.snapshot(c);
      check(StepMath.delta(-1, 9000, true) == 0, "First sensor baseline");
      check(StepMath.delta(9000, 9030, true) == 30, "Sensor delta");
      check(StepMath.delta(9030, 15, false) == 15, "Reboot recovery");
      check(Reports.cell("=SUM(A1)").equals("\"'=SUM(A1)\""), "CSV formula protection");
      check(Reports.cell("\t=SUM(A1)").startsWith("\"'"), "CSV tab-prefixed formula protection");
      check(Reports.cell("  +SUM(A1)").startsWith("\"'"), "CSV space-prefixed formula protection");
      check(Reports.cell("a\"b").equals("\"a\"\"b\""), "CSV quotes");
      String csv = Reports.csv(c);
      check(csv.startsWith("date,type,label"), "CSV headers");
      check(csv.contains("\r\n"), "CSV record separators");
      byte[] pdf = Reports.pdf(c);
      check(
          new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF"),
          "Real PDF output");
      check(pdf.length > 1000, "PDF content");
      JSONObject water =
          new JSONObject().put("type", "water").put("date", Store.today()).put("value", "250");
      check(RecordValidation.check(water).isEmpty(), "Valid measurement");
      water.put("value", "-250");
      check(!RecordValidation.check(water).isEmpty(), "Reject negative measurement");
      water.put("value", "NaN");
      check(!RecordValidation.check(water).isEmpty(), "Reject non-finite measurement");
      water.put("value", "250").put("date", "2026-99-99");
      check(!RecordValidation.check(water).isEmpty(), "Reject invalid date");
      JSONObject sleep =
          new JSONObject().put("type", "sleep").put("date", Store.today()).put("value", "25");
      check(!RecordValidation.check(sleep).isEmpty(), "Sleep range");
      JSONObject med =
          new JSONObject()
              .put("type", "medication")
              .put("date", Store.today())
              .put("value", "1 tablet")
              .put("label", "Test")
              .put("time", "09:00");
      check(RecordValidation.check(med).isEmpty(), "Medication time valid");
      med.put("time", "99:00");
      check(!RecordValidation.check(med).isEmpty(), "Reject invalid medication time");
      JSONObject w = new JSONObject().put("paused", true).put("accumulated", 12345);
      check(WorkoutService.elapsed(w) == 12345, "Paused duration remains stable");
      w.put("paused", false).put("segmentStart", System.currentTimeMillis() - 5000);
      check(WorkoutService.elapsed(w) >= 17345, "Live elapsed duration");
      // Decode an actual product barcode using the same decoder shipped in the scanner.
      com.google.zxing.common.BitMatrix matrix =
          new com.google.zxing.MultiFormatWriter()
              .encode("3017620422003", com.google.zxing.BarcodeFormat.EAN_13, 600, 160);
      int[] pixels = new int[matrix.getWidth() * matrix.getHeight()];
      for (int y = 0; y < matrix.getHeight(); y++)
        for (int x = 0; x < matrix.getWidth(); x++)
          pixels[y * matrix.getWidth() + x] = matrix.get(x, y) ? 0xff000000 : 0xffffffff;
      com.google.zxing.BinaryBitmap bitmap =
          new com.google.zxing.BinaryBitmap(
              new com.google.zxing.common.HybridBinarizer(
                  new com.google.zxing.RGBLuminanceSource(
                      matrix.getWidth(), matrix.getHeight(), pixels)));
      check(
          new com.google.zxing.MultiFormatReader().decode(bitmap).getText().equals("3017620422003"),
          "Barcode decoding");
      java.time.ZoneId zone = java.time.ZoneId.of("America/Los_Angeles");
      java.time.ZonedDateTime
          morning = java.time.LocalDate.of(2026, 10, 11).atTime(6, 0).atZone(zone),
          evening = morning.withHour(20).withMinute(45);
      check(
          ReminderReceiver.nextWater(morning, 30).getHour() == 8,
          "No early morning hydration reminder");
      check(
          ReminderReceiver.nextWater(evening, 30)
              .toLocalDate()
              .equals(java.time.LocalDate.of(2026, 10, 12)),
          "Quiet hours move reminder to next day");
      check(
          ReminderReceiver.nextWater(evening, 30).getHour() == 8, "Evening reminder moves to 8 am");
      check(
          ReminderReceiver.nextWater(morning.withHour(10), 120).getHour() == 12,
          "Hydration interval");
      if (Build.VERSION.SDK_INT >= 34) healthImports(c);
      check(
          Store.snapshot(c).getJSONArray("entries").length()
              == before.getJSONArray("entries").length(),
          "Tests preserve journal");
      for (java.util.Iterator<String> keys = before.getJSONObject("steps").keys();
          keys.hasNext(); ) {
        String day = keys.next();
        check(
            Store.step(c, day) >= before.getJSONObject("steps").getInt(day),
            "Historical steps remain intact");
      }
      result.putString(
          "stream",
          "PASS: "
              + checks
              + " native checks, including PDF/CSV, measurement validation, workout timing and real"
              + " barcode decoding.\n");
      finish(Activity.RESULT_OK, result);
    } catch (Throwable e) {
      result.putString("stream", "FAIL: " + e.toString() + "\n");
      finish(Activity.RESULT_CANCELED, result);
    }
  }

  @android.annotation.TargetApi(34)
  private void healthImports(Context c) throws Exception {
    JSONObject original = Store.data(c);
    Set<String> tombstones =
        new HashSet<>(Store.prefs(c).getStringSet("deletedHealth", Collections.emptySet()));
    String sourceId = UUID.randomUUID().toString(),
        id =
            UUID.nameUUIDFromBytes(
                    ("hc:" + sourceId).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .toString();
    java.lang.reflect.Constructor<HealthSync> constructor =
        HealthSync.class.getDeclaredConstructor(Context.class);
    constructor.setAccessible(true);
    HealthSync sync = constructor.newInstance(c);
    java.lang.reflect.Method upsert =
        HealthSync.class.getDeclaredMethod("upsert", android.health.connect.datatypes.Record.class);
    upsert.setAccessible(true);
    android.health.connect.datatypes.Metadata metadata =
        new android.health.connect.datatypes.Metadata.Builder()
            .setId(sourceId)
            .setDataOrigin(
                new android.health.connect.datatypes.DataOrigin.Builder()
                    .setPackageName("com.peter.testsource")
                    .build())
            .build();
    android.health.connect.datatypes.WeightRecord record =
        new android.health.connect.datatypes.WeightRecord.Builder(
                metadata,
                java.time.Instant.now(),
                android.health.connect.datatypes.units.Mass.fromGrams(70000))
            .build();
    try {
      upsert.invoke(sync, record);
      JSONArray records = Store.data(c).getJSONArray("entries");
      check(
          records.length() == original.getJSONArray("entries").length() + 1,
          "Health import adds a record");
      check(
          records.getJSONObject(records.length() - 1).getDouble("value") == 70,
          "Health weight unit conversion");
      upsert.invoke(sync, record);
      check(
          Store.data(c).getJSONArray("entries").length() == records.length(),
          "Repeated health sync is idempotent");
      JSONObject edited = Store.data(c);
      JSONArray a = edited.getJSONArray("entries");
      JSONObject last = a.getJSONObject(a.length() - 1);
      last.put("value", 71).put("edited", true);
      Store.save(c, edited);
      upsert.invoke(sync, record);
      check(
          Store.data(c).getJSONArray("entries").getJSONObject(a.length() - 1).getDouble("value")
              == 71,
          "Sync preserves local edits");
      Set<String> deleted = new HashSet<>(tombstones);
      deleted.add(id);
      Store.prefs(c).edit().putStringSet("deletedHealth", deleted).commit();
      Store.save(c, original);
      upsert.invoke(sync, record);
      check(
          Store.data(c).getJSONArray("entries").length()
              == original.getJSONArray("entries").length(),
          "Deleted import is not recreated");
    } finally {
      Store.save(c, original);
      Store.prefs(c).edit().putStringSet("deletedHealth", tombstones).commit();
    }
  }
}
