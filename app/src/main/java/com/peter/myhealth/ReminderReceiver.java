package com.peter.myhealth;

import android.app.*;
import android.content.*;
import java.time.*;
import org.json.*;

public class ReminderReceiver extends BroadcastReceiver {
  static void schedule(Context c) {
    scheduleWater(c);
    try {
      JSONArray entries = Store.data(c).getJSONArray("entries");
      for (int i = 0; i < entries.length(); i++) {
        JSONObject e = entries.getJSONObject(i);
        if (!e.optString("type").equals("medication") || e.optString("time").isEmpty()) continue;
        ZonedDateTime now = ZonedDateTime.now(),
            next =
                now.toLocalDate()
                    .atTime(LocalTime.parse(e.getString("time")))
                    .atZone(now.getZone());
        if (!next.isAfter(now)) next = next.plusDays(1);
        c.getSystemService(AlarmManager.class)
            .setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.toInstant().toEpochMilli(),
                pending(c, e.getString("id")));
      }
    } catch (Exception ignored) {
    }
  }

  static PendingIntent pending(Context c, String id) {
    Intent i = new Intent(c, ReminderReceiver.class).setAction("med:" + id).putExtra("id", id);
    return PendingIntent.getBroadcast(
        c, id.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  static void cancel(Context c, String id) {
    c.getSystemService(AlarmManager.class).cancel(pending(c, id));
  }

  static PendingIntent waterIntent(Context c) {
    return PendingIntent.getBroadcast(
        c,
        31,
        new Intent(c, ReminderReceiver.class).setAction("water"),
        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
  }

  static ZonedDateTime nextWater(ZonedDateTime now, int minutes) {
    ZonedDateTime next = now.plusMinutes(minutes);
    if (next.getHour() >= 21)
      return next.plusDays(1).toLocalDate().atTime(8, 0).atZone(now.getZone());
    if (next.getHour() < 8) return next.toLocalDate().atTime(8, 0).atZone(now.getZone());
    return next;
  }

  static void scheduleWater(Context c) {
    AlarmManager alarms = c.getSystemService(AlarmManager.class);
    alarms.cancel(waterIntent(c));
    if (!Store.prefs(c).getBoolean("hydration", false)) return;
    ZonedDateTime next =
        nextWater(ZonedDateTime.now(), Store.prefs(c).getInt("hydrationMinutes", 120));
    alarms.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), waterIntent(c));
  }

  public void onReceive(Context c, Intent intent) {
    if ("water".equals(intent.getAction())) {
      if (!Store.prefs(c).getBoolean("hydration", false)) return;
      double logged = 0;
      JSONArray a = Store.data(c).optJSONArray("entries");
      for (int i = 0; i < a.length(); i++) {
        JSONObject e = a.optJSONObject(i);
        if (e != null
            && "water".equals(e.optString("type"))
            && Store.today().equals(e.optString("date"))) logged += e.optDouble("value");
      }
      int hour = LocalTime.now().getHour();
      if (hour >= 8
          && hour < 21
          && logged < Store.data(c).optJSONObject("goals").optInt("water", 2000)) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(
            new NotificationChannel(
                "water", "Hydration reminders", NotificationManager.IMPORTANCE_DEFAULT));
        nm.notify(
            31,
            new Notification.Builder(c, "water")
                .setSmallIcon(R.drawable.ic_health)
                .setContentTitle("Time for a water break")
                .setContentText("A small sip, a little reset. Log your drink in My Health.")
                .setContentIntent(
                    PendingIntent.getActivity(
                        c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(true)
                .build());
      }
      scheduleWater(c);
      return;
    }
    try {
      JSONArray entries = Store.data(c).getJSONArray("entries");
      for (int i = 0; i < entries.length(); i++) {
        JSONObject e = entries.getJSONObject(i);
        if (!e.getString("id").equals(intent.getStringExtra("id"))) continue;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(
            new NotificationChannel(
                "reminders", "Medication reminders", NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open =
            PendingIntent.getActivity(
                c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        nm.notify(
            e.getString("id").hashCode(),
            new Notification.Builder(c, "reminders")
                .setSmallIcon(R.drawable.ic_health)
                .setContentTitle("Medication reminder")
                .setContentText(e.optString("label") + " · " + e.optString("value"))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build());
      }
    } catch (Exception ignored) {
    } finally {
      schedule(c);
    }
  }
}
