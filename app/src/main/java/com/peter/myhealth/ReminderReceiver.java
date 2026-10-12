package com.peter.myhealth;

import android.app.*;
import android.content.*;
import org.json.*;
import java.time.*;

public class ReminderReceiver extends BroadcastReceiver {
    static void schedule(Context c) {
        try {
            JSONArray entries = Store.data(c).getJSONArray("entries");
            for (int i=0; i<entries.length(); i++) {
                JSONObject e = entries.getJSONObject(i);
                if (!e.optString("type").equals("medication") || e.optString("time").isEmpty()) continue;
                ZonedDateTime now = ZonedDateTime.now(), next = now.toLocalDate().atTime(LocalTime.parse(e.getString("time"))).atZone(now.getZone());
                if (!next.isAfter(now)) next = next.plusDays(1);
                c.getSystemService(AlarmManager.class).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pending(c, e.getString("id")));
            }
        } catch (Exception ignored) {}
    }
    static PendingIntent pending(Context c, String id) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction("med:" + id).putExtra("id", id);
        return PendingIntent.getBroadcast(c, id.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static void cancel(Context c, String id) { c.getSystemService(AlarmManager.class).cancel(pending(c, id)); }
    public void onReceive(Context c, Intent intent) {
        try {
            JSONArray entries = Store.data(c).getJSONArray("entries");
            for (int i=0; i<entries.length(); i++) {
                JSONObject e = entries.getJSONObject(i);
                if (!e.getString("id").equals(intent.getStringExtra("id"))) continue;
                NotificationManager nm = c.getSystemService(NotificationManager.class);
                nm.createNotificationChannel(new NotificationChannel("reminders", "Medication reminders", NotificationManager.IMPORTANCE_DEFAULT));
                PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
                nm.notify(e.getString("id").hashCode(), new Notification.Builder(c, "reminders").setSmallIcon(R.drawable.ic_health).setContentTitle("Medication reminder").setContentText(e.optString("label") + " · " + e.optString("value")).setContentIntent(open).setAutoCancel(true).build());
            }
        } catch (Exception ignored) {} finally { schedule(c); }
    }
}
