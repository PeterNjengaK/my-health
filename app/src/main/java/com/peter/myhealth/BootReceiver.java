package com.peter.myhealth;

import android.content.*;

public class BootReceiver extends BroadcastReceiver {
  public void onReceive(Context c, Intent i) {
    if (!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) return;
    ReminderReceiver.schedule(c);
    StepWidget.update(c);
    try {
      org.json.JSONObject w = WorkoutService.stored(c);
      if (w.optBoolean("active")) {
        w.put("paused", true)
            .put("accumulated", Math.max(w.optLong("lastElapsed"), w.optLong("accumulated")));
        WorkoutService.put(c, w);
      }
    } catch (Exception ignored) {
    }
    if (Store.prefs(c).getBoolean("tracking", false)
        && c.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED)
      c.startForegroundService(new Intent(c, StepService.class));
  }
}
