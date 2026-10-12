package com.peter.myhealth;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.os.*;

public class StepService extends Service implements SensorEventListener {
    private SensorManager manager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() { public void run() { update(); handler.postDelayed(this, 60000); } };
    public void onCreate() { super.onCreate(); manager = (SensorManager)getSystemService(SENSOR_SERVICE); }
    public int onStartCommand(Intent intent, int flags, int id) {
        if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED || !Store.prefs(this).getBoolean("tracking", false)) { stopSelf(); return START_NOT_STICKY; }
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("steps", "Daily step counter", NotificationManager.IMPORTANCE_LOW));
        startForeground(1, notification());
        Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (sensor == null || !manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)) {
            Store.prefs(this).edit().putBoolean("tracking", false).apply(); stopSelf(); return START_NOT_STICKY;
        }
        handler.removeCallbacks(refresh); handler.post(refresh);
        return START_STICKY;
    }
    private Notification notification() {
        int steps = Store.prefs(this).getInt("steps:" + Store.today(), 0);
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, "steps").setSmallIcon(com.peter.myhealth.R.drawable.ic_health).setContentTitle(String.format(java.util.Locale.getDefault(), "%,d steps today", steps)).setContentText("My Health · Daily activity tracking").setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).build();
    }
    private void update() { getSystemService(NotificationManager.class).notify(1, notification()); }
    public void onSensorChanged(SensorEvent event) {
        SharedPreferences p = Store.prefs(this);
        long current = (long)event.values[0], last = p.getLong("counter", -1);
        int boot = android.provider.Settings.Global.getInt(getContentResolver(), android.provider.Settings.Global.BOOT_COUNT, 0);
        boolean sameBoot = p.getInt("boot", -1) == boot;
        long delta = StepMath.delta(last, current, sameBoot);
        // Sensor events are stamped with elapsed time: delayed events retain their actual day.
        String day = StepMath.eventDay(System.currentTimeMillis(), SystemClock.elapsedRealtimeNanos(), event.timestamp, java.time.ZoneId.systemDefault());
        int total = p.getInt("steps:" + day, 0);
        p.edit().putLong("counter", current).putInt("boot", boot).putInt("steps:" + day, (int)Math.min(Integer.MAX_VALUE, total + delta)).commit();
        update();
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    public IBinder onBind(Intent i) { return null; }
    public void onDestroy() { manager.unregisterListener(this); handler.removeCallbacks(refresh); super.onDestroy(); }
}
