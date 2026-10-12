package com.peter.myhealth;

import android.content.*;
import org.json.*;
import java.time.*;

final class Store {
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("health", Context.MODE_PRIVATE); }
    static String today() { return LocalDate.now().toString(); }
    static synchronized JSONObject data(Context c) {
        try { return new JSONObject(prefs(c).getString("data", "{\"entries\":[],\"goals\":{\"steps\":8000,\"water\":2000,\"sleep\":8},\"name\":\"Peter\",\"stride\":0.7}")); }
        catch (JSONException e) { throw new IllegalStateException(e); }
    }
    static synchronized void save(Context c, JSONObject d) { prefs(c).edit().putString("data", d.toString()).commit(); }
    static synchronized JSONObject snapshot(Context c) throws JSONException {
        JSONObject d = data(c), steps = new JSONObject();
        for (java.util.Map.Entry<String,?> e : prefs(c).getAll().entrySet())
            if (e.getKey().startsWith("steps:")) steps.put(e.getKey().substring(6), e.getValue());
        d.put("steps", steps); d.put("today", today());
        d.put("tracking", prefs(c).getBoolean("tracking", false));
        d.put("permission", c.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == android.content.pm.PackageManager.PERMISSION_GRANTED);
        d.put("sensor", ((android.hardware.SensorManager)c.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER) != null);
        d.put("notifications", ((android.app.NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).areNotificationsEnabled());
        return d;
    }
}
