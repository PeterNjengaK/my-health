package com.peter.myhealth;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.os.*;

public class StepService extends Service implements SensorEventListener {
  static volatile boolean running;
  private SensorManager manager;
  private final java.util.ArrayList<Long> detected = new java.util.ArrayList<>();
  private long lastTimestamp = -1;
  private BroadcastReceiver boundary;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable refresh =
      new Runnable() {
        public void run() {
          update();
          handler.postDelayed(this, 60000);
        }
      };

  public void onCreate() {
    super.onCreate();
    manager = (SensorManager) getSystemService(SENSOR_SERVICE);
    int boot =
        android.provider.Settings.Global.getInt(
            getContentResolver(), android.provider.Settings.Global.BOOT_COUNT, 0);
    if (Store.prefs(this).getInt("boot", -1) == boot)
      lastTimestamp = Store.prefs(this).getLong("counterTimestamp", -1);
    boundary = new DailyBoundaryReceiver();
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_DATE_CHANGED);
    filter.addAction(Intent.ACTION_USER_PRESENT);
    if (Build.VERSION.SDK_INT >= 33)
      registerReceiver(boundary, filter, Context.RECEIVER_NOT_EXPORTED);
    else registerReceiver(boundary, filter);
  }

  public int onStartCommand(Intent intent, int flags, int id) {
    if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
        != PackageManager.PERMISSION_GRANTED) {
      Store.prefs(this).edit().putBoolean("tracking", false).apply();
      stopSelf();
      return START_NOT_STICKY;
    }
    // Activity creation/resume and package replacement can request the same service.
    // Registering an already registered sensor can return false; keep the existing listener.
    if (running) {
      update();
      return START_STICKY;
    }
    NotificationManager nm = getSystemService(NotificationManager.class);
    nm.createNotificationChannel(
        new NotificationChannel("steps", "Daily step counter", NotificationManager.IMPORTANCE_LOW));
    startForeground(1, notification(this));
    Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    if (sensor == null
        || !manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)) {
      Store.prefs(this).edit().putBoolean("tracking", false).apply();
      stopSelf();
      return START_NOT_STICKY;
    }
    running = true;
    Sensor detector = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    if (detector != null)
      manager.registerListener(this, detector, SensorManager.SENSOR_DELAY_NORMAL);
    Store.prefs(this).edit().putBoolean("tracking", true).apply();
    DailyBoundaryReceiver.refresh(this);
    handler.removeCallbacks(refresh);
    handler.post(refresh);
    return START_STICKY;
  }

  static Notification notification(Context c) {
    int steps = Store.step(c, Store.today());
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            0,
            new Intent(c, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    return new Notification.Builder(c, "steps")
        .setSmallIcon(com.peter.myhealth.R.drawable.ic_notification)
        .setContentTitle(
            Store.prefs(c).getBoolean("appLock", false)
                ? "Automatic step tracking"
                : String.format(java.util.Locale.getDefault(), "%,d steps today", steps))
        .setContentText("My Health · Daily activity tracking")
        .setContentIntent(open)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build();
  }

  private void update() {
    getSystemService(NotificationManager.class).notify(1, notification(this));
    StepWidget.update(this);
  }

  public void onSensorChanged(SensorEvent event) {
    if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
      if (event.timestamp > lastTimestamp && detected.size() < 10000) detected.add(event.timestamp);
      return;
    }
    long current = (long) event.values[0], timestamp = event.timestamp;
    // Give the low-latency detector time to deliver matching per-step timestamps.
    handler.postDelayed(() -> recordCounter(current, timestamp), 2200);
  }

  private void recordCounter(long current, long timestamp) {
    SharedPreferences p = Store.prefs(this);
    long last = p.getLong("counter", -1);
    int boot =
        android.provider.Settings.Global.getInt(
            getContentResolver(), android.provider.Settings.Global.BOOT_COUNT, 0);
    boolean sameBoot = p.getInt("boot", -1) == boot;
    if (sameBoot && last >= 0 && current < last) return;
    long delta = StepMath.delta(last, current, sameBoot);
    // Sensor events are stamped with elapsed time: delayed events retain their actual day.
    java.util.Map<String, Long> totals =
        StepMath.splitDailySteps(
            delta,
            detected,
            timestamp,
            System.currentTimeMillis(),
            SystemClock.elapsedRealtimeNanos(),
            java.time.ZoneId.systemDefault());
    SharedPreferences.Editor edit =
        p.edit()
            .putLong("counter", current)
            .putInt("boot", boot)
            .putLong("counterTimestamp", timestamp);
    for (java.util.Map.Entry<String, Long> entry : totals.entrySet())
      edit.putInt(
          "steps:" + entry.getKey(),
          (int)
              Math.min(
                  Integer.MAX_VALUE, p.getInt("steps:" + entry.getKey(), 0) + entry.getValue()));
    edit.commit();
    lastTimestamp = timestamp;
    detected.removeIf(t -> t <= timestamp);
    update();
  }

  public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  public IBinder onBind(Intent i) {
    return null;
  }

  public void onDestroy() {
    running = false;
    Store.prefs(this).edit().putBoolean("tracking", false).apply();
    manager.unregisterListener(this);
    unregisterReceiver(boundary);
    handler.removeCallbacksAndMessages(null);
    StepWidget.update(this);
    super.onDestroy();
  }
}
