package com.peter.myhealth;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;

public class StepWidget extends AppWidgetProvider {
  public void onUpdate(Context c, AppWidgetManager manager, int[] ids) {
    update(c);
  }

  static void update(Context c) {
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    int[] ids = manager.getAppWidgetIds(new ComponentName(c, StepWidget.class));
    if (ids.length == 0) return;
    int count = Store.step(c, Store.today()),
        goal = Store.data(c).optJSONObject("goals").optInt("steps", 8000);
    boolean privateView = Store.prefs(c).getBoolean("appLock", false);
    RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.step_widget);
    views.setTextViewText(
        R.id.widget_steps,
        privateView ? "Locked" : String.format(java.util.Locale.getDefault(), "%,d steps", count));
    views.setTextViewText(
        R.id.widget_goal,
        privateView
            ? "Unlock My Health to view your activity"
            : "of "
                + goal
                + " · "
                + (c.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ? "Automatic · resets 00:00"
                    : "Activity permission needed"));
    views.setProgressBar(
        R.id.widget_progress,
        100,
        privateView ? 0 : (int) Math.min(100, count * 100L / goal),
        false);
    views.setOnClickPendingIntent(
        R.id.widget_root,
        PendingIntent.getActivity(
            c,
            0,
            new Intent(c, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
    manager.updateAppWidget(ids, views);
  }

  static void pin(Activity a) {
    AppWidgetManager m = AppWidgetManager.getInstance(a);
    if (m.isRequestPinAppWidgetSupported())
      m.requestPinAppWidget(new ComponentName(a, StepWidget.class), null, null);
    else
      android.widget.Toast.makeText(
              a,
              "Long-press your home screen → Widgets → My Health",
              android.widget.Toast.LENGTH_LONG)
          .show();
  }
}
