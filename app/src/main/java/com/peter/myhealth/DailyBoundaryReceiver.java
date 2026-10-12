package com.peter.myhealth;

import android.app.*;
import android.content.*;
import java.time.*;

/** Daily buckets change with the phone's calendar; historical totals are never erased. */
public class DailyBoundaryReceiver extends BroadcastReceiver {
  private static final String ROLLOVER = "com.peter.myhealth.DAILY_ROLLOVER";

  static void schedule(Context c) {
    PendingIntent next =
        PendingIntent.getBroadcast(
            c,
            41,
            new Intent(c, DailyBoundaryReceiver.class).setAction(ROLLOVER),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    c.getSystemService(AlarmManager.class)
        .set(
            AlarmManager.RTC_WAKEUP,
            StepMath.nextMidnight(System.currentTimeMillis(), ZoneId.systemDefault()),
            next);
  }

  static void refresh(Context c) {
    String day = Store.today();
    if (!Store.prefs(c).contains("steps:" + day))
      Store.prefs(c).edit().putInt("steps:" + day, 0).apply();
    StepWidget.update(c);
    if (StepService.running)
      c.getSystemService(android.app.NotificationManager.class)
          .notify(1, StepService.notification(c));
    schedule(c);
  }

  public void onReceive(Context c, Intent i) {
    String action = i.getAction();
    if (!java.util.Arrays.asList(
            ROLLOVER,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_USER_PRESENT)
        .contains(action)) return;
    refresh(c);
    if (!Intent.ACTION_USER_PRESENT.equals(action)) ReminderReceiver.schedule(c);
  }
}
