package com.peter.myhealth;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import org.json.*;

public class WorkoutService extends Service implements LocationListener {
  static volatile boolean running;
  private LocationManager locations;
  private Location previous;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable ticker =
      new Runnable() {
        public void run() {
          persistDuration();
          notifyProgress();
          handler.postDelayed(this, 10000);
        }
      };

  static JSONObject stored(Context c) {
    try {
      return new JSONObject(Store.prefs(c).getString("workout", "{}"));
    } catch (Exception e) {
      return new JSONObject();
    }
  }

  static JSONObject snapshot(Context c) {
    JSONObject w = stored(c);
    try {
      if (w.optBoolean("active")) w.put("elapsed", elapsed(w));
    } catch (Exception ignored) {
    }
    return w;
  }

  static long elapsed(JSONObject w) {
    return w.optLong("accumulated", 0)
        + (w.optBoolean("paused")
            ? 0
            : Math.max(
                0,
                System.currentTimeMillis()
                    - w.optLong("segmentStart", System.currentTimeMillis())));
  }

  static void put(Context c, JSONObject w) {
    Store.prefs(c).edit().putString("workout", w.toString()).commit();
  }

  public void onCreate() {
    super.onCreate();
    running = true;
    locations = getSystemService(LocationManager.class);
  }

  public int onStartCommand(Intent intent, int flags, int id) {
    String action = intent == null ? "resume" : intent.getAction();
    if (!"finish".equals(action) && !"pause".equals(action) && !ProBilling.access(this)) {
      pauseStored();
      stopSelf();
      return START_NOT_STICKY;
    }
    if ("finish".equals(action)) {
      try {
        finish(stored(this));
      } catch (Exception ignored) {
      }
      stopSelf();
      return START_NOT_STICKY;
    }
    if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      pauseStored();
      stopSelf();
      return START_NOT_STICKY;
    }
    try {
      JSONObject w = stored(this);
      if ("start".equals(action) && !w.optBoolean("active")) {
        w =
            new JSONObject()
                .put("active", true)
                .put("paused", false)
                .put("id", java.util.UUID.randomUUID().toString())
                .put("label", intent.getStringExtra("label"))
                .put("started", System.currentTimeMillis())
                .put("segmentStart", System.currentTimeMillis())
                .put("accumulated", 0)
                .put("distance", 0)
                .put("segment", 0)
                .put("route", new JSONArray());
        put(this, w);
        previous = null;
      }
      if (!w.optBoolean("active")) {
        stopSelf();
        return START_NOT_STICKY;
      }
      NotificationManager nm = getSystemService(NotificationManager.class);
      nm.createNotificationChannel(
          new NotificationChannel("workout", "GPS workouts", NotificationManager.IMPORTANCE_LOW));
      startForeground(2, notification());
      if ("pause".equals(action)) {
        pauseStored();
        locations.removeUpdates(this);
        previous = null;
      } else {
        if (w.optBoolean("paused")) {
          w.put("paused", false)
              .put("segmentStart", System.currentTimeMillis())
              .put("segment", w.optInt("segment") + 1);
          put(this, w);
          previous = null;
        }
        if (!locations.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
          Store.prefs(this)
              .edit()
              .putString("gpsStatus", "GPS is off. Enable location in Android settings.")
              .apply();
        } else {
          locations.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2, this);
          Store.prefs(this)
              .edit()
              .putString("gpsStatus", "Waiting for a GPS fix — best outdoors")
              .apply();
        }
      }
      handler.removeCallbacks(ticker);
      handler.post(ticker);
      return START_STICKY;
    } catch (Exception e) {
      Store.prefs(this)
          .edit()
          .putString("gpsStatus", "Location unavailable. Check GPS and precise location access.")
          .apply();
      pauseStored();
      stopSelf();
      return START_NOT_STICKY;
    }
  }

  private void pauseStored() {
    try {
      JSONObject w = stored(this);
      if (w.optBoolean("active") && !w.optBoolean("paused")) {
        w.put("accumulated", elapsed(w)).put("paused", true);
        put(this, w);
      }
    } catch (Exception ignored) {
    }
  }

  private void persistDuration() {
    try {
      JSONObject w = stored(this);
      if (w.optBoolean("active")) {
        w.put("lastElapsed", elapsed(w));
        put(this, w);
      }
    } catch (Exception ignored) {
    }
  }

  private void finish(JSONObject w) throws JSONException {
    if (!w.optBoolean("active")) return;
    long ms = elapsed(w);
    JSONObject e =
        new JSONObject()
            .put("id", w.getString("id"))
            .put("type", "workout")
            .put("label", w.optString("label", "Walking"))
            .put(
                "date",
                java.time.Instant.ofEpochMilli(w.getLong("started"))
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .toString())
            .put("value", Math.round(ms / 600.0) / 100.0)
            .put("distance", Math.round(w.optDouble("distance") / 10) / 100.0)
            .put("route", w.getJSONArray("route"))
            .put("created", System.currentTimeMillis())
            .put("source", "gps")
            .put("note", "GPS workout · " + w.getJSONArray("route").length() + " route points");
    synchronized (Store.class) {
      JSONObject d = Store.data(this);
      d.getJSONArray("entries").put(e);
      Store.save(this, d);
    }
    Store.prefs(this).edit().remove("workout").remove("gpsStatus").commit();
    locations.removeUpdates(this);
    stopForeground(STOP_FOREGROUND_REMOVE);
  }

  public void onLocationChanged(Location l) {
    JSONObject w = stored(this);
    if (!w.optBoolean("active")
        || w.optBoolean("paused")
        || !l.hasAccuracy()
        || l.getAccuracy() > 35
        || Math.abs(System.currentTimeMillis() - l.getTime()) > 30000) return;
    try {
      if (previous != null) {
        double metres = previous.distanceTo(l);
        double seconds = (l.getElapsedRealtimeNanos() - previous.getElapsedRealtimeNanos()) / 1e9;
        if (seconds <= 0 || metres / seconds > 45) return;
        if (metres < Math.max(5, l.getAccuracy() * .5)) return;
        w.put("distance", w.optDouble("distance") + metres);
      }
      JSONArray route = w.getJSONArray("route");
      if (route.length() < 15000)
        route.put(
            new JSONObject()
                .put("lat", l.getLatitude())
                .put("lon", l.getLongitude())
                .put("time", l.getTime())
                .put("segment", w.optInt("segment"))
                .put("accuracy", Math.round(l.getAccuracy())));
      put(this, w);
      previous = new Location(l);
      Store.prefs(this)
          .edit()
          .putString("gpsStatus", "GPS connected · ±" + Math.round(l.getAccuracy()) + " m")
          .apply();
      notifyProgress();
    } catch (Exception ignored) {
    }
  }

  private Notification notification() {
    JSONObject w = snapshot(this);
    PendingIntent open =
        PendingIntent.getActivity(
            this,
            0,
            new Intent(this, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    PendingIntent stop =
        PendingIntent.getService(
            this,
            20,
            new Intent(this, WorkoutService.class).setAction("finish"),
            PendingIntent.FLAG_IMMUTABLE);
    return new Notification.Builder(this, "workout")
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(
            w.optString("label", "Workout")
                + (w.optBoolean("paused") ? " · Paused" : " · Recording"))
        .setContentText(
            String.format(
                java.util.Locale.getDefault(),
                "%.2f %s · %d min",
                w.optDouble("distance") / (Store.data(this).optString("distanceUnit").equals("mi") ? 1609.344 : 1000),
                Store.data(this).optString("distanceUnit").equals("mi") ? "mi" : "km",
                w.optLong("elapsed") / 60000))
        .setContentIntent(open)
        .addAction(new Notification.Action.Builder(null, "Finish", stop).build())
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setVisibility(Notification.VISIBILITY_PRIVATE)
        .build();
  }

  private void notifyProgress() {
    getSystemService(NotificationManager.class).notify(2, notification());
  }

  public void onProviderDisabled(String provider) {
    Store.prefs(this)
        .edit()
        .putString("gpsStatus", "GPS is off. Enable location to continue.")
        .apply();
  }

  public void onProviderEnabled(String provider) {
    Store.prefs(this).edit().putString("gpsStatus", "Waiting for a GPS fix").apply();
  }

  public void onStatusChanged(String provider, int status, Bundle extras) {}

  public IBinder onBind(Intent i) {
    return null;
  }

  public void onDestroy() {
    running = false;
    handler.removeCallbacks(ticker);
    locations.removeUpdates(this);
    super.onDestroy();
  }
}
