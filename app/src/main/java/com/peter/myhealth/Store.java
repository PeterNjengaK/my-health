package com.peter.myhealth;

import android.content.*;
import java.time.*;
import org.json.*;

final class Store {
  static SharedPreferences prefs(Context c) {
    return c.getSharedPreferences("health", Context.MODE_PRIVATE);
  }

  static String today() {
    return LocalDate.now().toString();
  }

  static synchronized JSONObject data(Context c) {
    try {
      return new JSONObject(
          prefs(c)
              .getString(
                  "data",
                  "{\"entries\":[],\"goals\":{\"steps\":8000,\"water\":2000,\"sleep\":8},\"name\":\"\",\"stride\":0.7}"));
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
  }

  static synchronized void save(Context c, JSONObject d) {
    prefs(c).edit().putString("data", d.toString()).commit();
  }

  static int step(Context c, String day) {
    return Math.max(prefs(c).getInt("steps:" + day, 0), prefs(c).getInt("importSteps:" + day, 0));
  }

  static synchronized JSONObject snapshot(Context c) throws JSONException {
    JSONObject d = data(c), steps = new JSONObject();
    for (java.util.Map.Entry<String, ?> e : prefs(c).getAll().entrySet())
      if (e.getKey().startsWith("steps:") || e.getKey().startsWith("importSteps:")) {
        String day = e.getKey().substring(e.getKey().indexOf(':') + 1);
        steps.put(day, step(c, day));
      }
    d.put("steps", steps);
    d.put("today", today());
    d.put("tracking", prefs(c).getBoolean("tracking", false));
    d.put("automaticSteps", true);
    d.put("version", BuildConfig.VERSION_NAME);
    d.put("pro", ProBilling.state(c));
    d.put(
        "permission",
        c.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED);
    d.put(
        "sensor",
        ((android.hardware.SensorManager) c.getSystemService(Context.SENSOR_SERVICE))
                .getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER)
            != null);
    d.put(
        "notifications",
        ((android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE))
            .areNotificationsEnabled());
    d.put("appLock", prefs(c).getBoolean("appLock", false));
    d.put("theme", prefs(c).getString("theme", "system"));
    d.put(
        "systemDark",
        (c.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
            == android.content.res.Configuration.UI_MODE_NIGHT_YES);
    d.put("hydration", prefs(c).getBoolean("hydration", false));
    d.put("hydrationMinutes", prefs(c).getInt("hydrationMinutes", 120));
    d.put("healthConnect", android.os.Build.VERSION.SDK_INT >= 34);
    d.put("syncStatus", prefs(c).getString("syncStatus", "Not synced yet"));
    d.put("lastSync", prefs(c).getLong("lastSync", 0));
    d.put("workout", WorkoutService.snapshot(c));
    d.put("gpsStatus", prefs(c).getString("gpsStatus", "Waiting for a GPS fix — best outdoors"));
    return d;
  }
}
