package com.peter.myhealth;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.webkit.*;
import android.widget.Toast;
import java.nio.charset.StandardCharsets;
import org.json.*;

public class MainActivity extends Activity {
  private WebView web;
  private String exportData;
  private byte[] documentBytes;
  private ProBilling billing;
  private volatile boolean unlocked;
  private boolean authenticating, externalFlow;
  private android.view.View lockCover;
  private android.widget.FrameLayout root;
  private String pendingWorkout = "Walking";
  private final java.util.ArrayList<String> pendingScripts = new java.util.ArrayList<>();
  private final java.util.concurrent.ExecutorService worker =
      java.util.concurrent.Executors.newSingleThreadExecutor();

  public void onCreate(Bundle b) {
    super.onCreate(b);
    billing = new ProBilling(this);
    billing.start();
    unlocked = !Store.prefs(this).getBoolean("appLock", false);
    try {
      JSONObject w = WorkoutService.stored(this);
      if (w.optBoolean("active") && !WorkoutService.running && !w.optBoolean("paused")) {
        w.put("paused", true)
            .put("accumulated", Math.max(w.optLong("lastElapsed"), w.optLong("accumulated")));
        WorkoutService.put(this, w);
      }
    } catch (Exception ignored) {
    }
    if (!unlocked) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    getWindow().setStatusBarColor(android.graphics.Color.rgb(246, 247, 242));
    getWindow().setNavigationBarColor(android.graphics.Color.rgb(246, 247, 242));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    web = new WebView(this);
    if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
      WebView.setWebContentsDebuggingEnabled(true);
    web.setBackgroundColor(android.graphics.Color.rgb(246, 247, 242));
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setAllowFileAccess(false);
    web.getSettings().setAllowContentAccess(false);
    web.getSettings().setBlockNetworkLoads(true);
    web.addJavascriptInterface(new Bridge(), "Health");
    web.setWebViewClient(
        new WebViewClient() {
          public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
            return true;
          }

          public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
            String path = r.getUrl().getPath();
            if ("app.health.local".equals(r.getUrl().getHost())
                && path != null
                && !path.contains("..")) {
              try {
                String mime =
                    path.endsWith(".js")
                        ? "application/javascript"
                        : path.endsWith(".css") ? "text/css" : "text/html";
                return new WebResourceResponse(mime, "UTF-8", getAssets().open(path.substring(1)));
              } catch (Exception ignored) {
              }
            }
            return new WebResourceResponse(
                "text/plain", "UTF-8", new java.io.ByteArrayInputStream(new byte[0]));
          }
        });
    root = new android.widget.FrameLayout(this);
    root.addView(web, new android.widget.FrameLayout.LayoutParams(-1, -1));
    android.widget.LinearLayout cover = new android.widget.LinearLayout(this);
    cover.setOrientation(android.widget.LinearLayout.VERTICAL);
    cover.setGravity(android.view.Gravity.CENTER);
    cover.setBackgroundColor(android.graphics.Color.rgb(246, 247, 242));
    android.widget.TextView title = new android.widget.TextView(this);
    title.setText("My Health\nYour journal is locked");
    title.setTextSize(24);
    title.setGravity(android.view.Gravity.CENTER);
    cover.addView(title);
    android.widget.Button unlockButton = new android.widget.Button(this);
    unlockButton.setText("Unlock");
    unlockButton.setOnClickListener(v -> authenticate(false));
    cover.addView(unlockButton);
    lockCover = cover;
    root.addView(cover, new android.widget.FrameLayout.LayoutParams(-1, -1));
    cover.setVisibility(unlocked ? android.view.View.GONE : android.view.View.VISIBLE);
    web.setVisibility(unlocked ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
    setContentView(root);
    // Keep the interface inside system bars, including Android's enforced edge-to-edge layout.
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets bars =
                insets.getInsets(
                    android.view.WindowInsets.Type.systemBars()
                        | android.view.WindowInsets.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          } else
            v.setPadding(
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom());
          return insets;
        });
    root.requestApplyInsets();
    web.loadUrl("https://app.health.local/index.html");
    if (Build.VERSION.SDK_INT >= 33)
      getOnBackInvokedDispatcher()
          .registerOnBackInvokedCallback(
              android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
              () -> web.evaluateJavascript("back()", null));
    startSteps();
    DailyBoundaryReceiver.refresh(this);
    ReminderReceiver.schedule(this);
    applySystemTheme(Store.prefs(this).getString("theme", "system"));
  }

  public void onResume() {
    super.onResume();
    if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
        != PackageManager.PERMISSION_GRANTED)
      Store.prefs(this)
          .edit()
          .remove("counter")
          .remove("counterTimestamp")
          .putBoolean("tracking", false)
          .apply();
    if (billing != null) billing.start();
    startSteps();
    if (!Store.prefs(this).getBoolean("activityPrompted", false)
        && ((android.hardware.SensorManager) getSystemService(SENSOR_SERVICE))
                .getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER)
            != null
        && checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
      Store.prefs(this).edit().putBoolean("activityPrompted", true).apply();
      requestPermissions(
          Build.VERSION.SDK_INT >= 33
              ? new String[] {
                android.Manifest.permission.ACTIVITY_RECOGNITION,
                android.Manifest.permission.POST_NOTIFICATIONS
              }
              : new String[] {android.Manifest.permission.ACTIVITY_RECOGNITION},
          1);
    }
    if (!unlocked) authenticate(false);
    else {
      js("refresh()");
      autoSync();
    }
    externalFlow = false;
  }

  protected void onStop() {
    super.onStop();
    if (Store.prefs(this).getBoolean("appLock", false)) {
      unlocked = false;
      web.setVisibility(android.view.View.INVISIBLE);
      lockCover.setVisibility(android.view.View.VISIBLE);
    }
  }

  private void autoSync() {
    if (Build.VERSION.SDK_INT >= 34
        && Store.prefs(this).getBoolean("healthEnabled", false)
        && System.currentTimeMillis() - Store.prefs(this).getLong("lastSync", 0) > 900000)
      HealthSync.sync(this);
  }

  private void js(String script) {
    runOnUiThread(
        () -> {
          if (unlocked)
            web.evaluateJavascript(
                script.equals("refresh()") ? "if(typeof refresh==='function')refresh()" : script,
                null);
          else pendingScripts.add(script);
        });
  }

  private void authenticate(boolean enable) {
    if (authenticating) return;
    if (!getSystemService(KeyguardManager.class).isDeviceSecure()) {
      toast("Set up a screen lock or fingerprint in Android settings first");
      return;
    }
    authenticating = true;
    android.hardware.biometrics.BiometricPrompt.Builder builder =
        new android.hardware.biometrics.BiometricPrompt.Builder(this)
            .setTitle(enable ? "Protect My Health" : "Unlock My Health")
            .setSubtitle("Use your fingerprint or device screen lock");
    if (Build.VERSION.SDK_INT >= 30)
      builder.setAllowedAuthenticators(
          android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
              | android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
    else builder.setDeviceCredentialAllowed(true);
    builder
        .build()
        .authenticate(
            new CancellationSignal(),
            getMainExecutor(),
            new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
              public void onAuthenticationSucceeded(
                  android.hardware.biometrics.BiometricPrompt.AuthenticationResult result) {
                authenticating = false;
                unlocked = true;
                if (enable) {
                  Store.prefs(MainActivity.this).edit().putBoolean("appLock", true).apply();
                  getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
                  StepWidget.update(MainActivity.this);
                }
                lockCover.setVisibility(android.view.View.GONE);
                web.setVisibility(android.view.View.VISIBLE);
                web.evaluateJavascript("refresh()", null);
                for (String s : pendingScripts) web.evaluateJavascript(s, null);
                pendingScripts.clear();
                autoSync();
              }

              public void onAuthenticationError(int code, CharSequence error) {
                authenticating = false;
                toast(error.toString());
              }
            });
  }

  private void startWorkout(String action, String label) {
    if (!unlocked) return;
    if ((action.equals("start") || action.equals("resume")) && !ProBilling.access(this)) {
      js("showPro()");
      return;
    }
    if (!java.util.Arrays.asList("start", "resume", "pause", "finish").contains(action)) return;
    if (action.equals("finish")) {
      startService(new Intent(this, WorkoutService.class).setAction(action));
      return;
    }
    if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      if (action.equals("start") || action.equals("resume")) {
        pendingWorkout = label;
        requestPermissions(
            new String[] {
              android.Manifest.permission.ACCESS_FINE_LOCATION,
              android.Manifest.permission.ACCESS_COARSE_LOCATION
            },
            3);
      } else toast("Allow location access to manage the active workout");
      return;
    }
    if ((action.equals("start") || action.equals("resume"))
        && !getSystemService(android.location.LocationManager.class)
            .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
      toast("Enable GPS to record your route");
      externalFlow = true;
      startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
      return;
    }
    startForegroundService(
        new Intent(this, WorkoutService.class).setAction(action).putExtra("label", label));
  }

  private void report(String format) {
    if (!unlocked) return;
    if (format.equals("pdf") && !ProBilling.access(this)) {
      js("showPro()");
      return;
    }
    if (!java.util.Arrays.asList("csv", "pdf").contains(format)) return;
    toast("Preparing your " + format.toUpperCase() + " report…");
    worker.execute(
        () -> {
          try {
            byte[] bytes =
                format.equals("pdf")
                    ? Reports.pdf(this)
                    : Reports.csv(this).getBytes(StandardCharsets.UTF_8);
            runOnUiThread(
                () -> {
                  if (!unlocked) return;
                  documentBytes = bytes;
                  externalFlow = true;
                  startActivityForResult(
                      new Intent(Intent.ACTION_CREATE_DOCUMENT)
                          .addCategory(Intent.CATEGORY_OPENABLE)
                          .setType(format.equals("pdf") ? "application/pdf" : "text/csv")
                          .putExtra(
                              Intent.EXTRA_TITLE, "my-health-" + Store.today() + "." + format),
                      12);
                });
          } catch (Exception e) {
            toast("Could not prepare report");
          }
        });
  }

  void toast(String s) {
    runOnUiThread(() -> Toast.makeText(this, s, Toast.LENGTH_LONG).show());
  }

  void startSteps() {
    if (StepService.running) return;
    if (((android.hardware.SensorManager) getSystemService(SENSOR_SERVICE))
            .getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER)
        == null) return;
    if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
        == PackageManager.PERMISSION_GRANTED)
      startForegroundService(new Intent(this, StepService.class));
  }

  public void onRequestPermissionsResult(int req, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(req, permissions, results);
    if (req == 1
        && checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
            == PackageManager.PERMISSION_GRANTED) {
      Store.prefs(this).edit().putBoolean("tracking", true).commit();
      startSteps();
    }
    if (req == 3
        && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED)
      startWorkout(
          WorkoutService.stored(this).optBoolean("active") ? "resume" : "start", pendingWorkout);
    if (req == 4 && Build.VERSION.SDK_INT >= 34) HealthSync.sync(this);
    js("refresh()");
  }

  final class Bridge {
    @JavascriptInterface
    public String state() {
      if (!unlocked) return "{}";
      try {
        return Store.snapshot(MainActivity.this).put("pro", billing.snapshot()).toString();
      } catch (Exception e) {
        return "{}";
      }
    }

    @JavascriptInterface
    public String add(String raw) {
      if (!unlocked) return "Unlock the app first";
      synchronized (Store.class) {
        try {
          JSONObject e = new JSONObject(raw), d = Store.data(MainActivity.this);
          String type = e.getString("type");
          if (!java.util.Arrays.asList(
                  "water",
                  "weight",
                  "sleep",
                  "workout",
                  "food",
                  "bloodpressure",
                  "heartrate",
                  "glucose",
                  "temperature",
                  "mood",
                  "symptom",
                  "medication",
                  "period",
                  "note",
                  "dose")
              .contains(type)) return "Invalid category";
          java.time.LocalDate.parse(e.getString("date"));
          if (e.optString("value").length() > 1000
              || e.optString("label").length() > 200
              || e.optString("note").length() > 4000) return "Entry too long";
          String invalid = RecordValidation.check(e);
          if (!invalid.isEmpty()) return invalid;
          e.put("id", java.util.UUID.randomUUID().toString());
          e.put("created", System.currentTimeMillis());
          d.getJSONArray("entries").put(e);
          Store.save(MainActivity.this, d);
          ReminderReceiver.schedule(MainActivity.this);
          return "";
        } catch (Exception e) {
          return "Could not save entry";
        }
      }
    }

    @JavascriptInterface
    public void delete(String id) {
      if (!unlocked) return;
      synchronized (Store.class) {
        try {
          JSONObject d = Store.data(MainActivity.this);
          JSONArray a = d.getJSONArray("entries"), out = new JSONArray();
          for (int i = 0; i < a.length(); i++) {
            JSONObject e = a.getJSONObject(i);
            if (!e.getString("id").equals(id)) out.put(e);
            else if ("healthConnect".equals(e.optString("source"))) {
              java.util.Set<String> tombstones =
                  new java.util.HashSet<>(
                      Store.prefs(MainActivity.this)
                          .getStringSet("deletedHealth", java.util.Collections.emptySet()));
              tombstones.add(id);
              Store.prefs(MainActivity.this)
                  .edit()
                  .putStringSet("deletedHealth", tombstones)
                  .apply();
            }
          }
          d.put("entries", out);
          Store.save(MainActivity.this, d);
          ReminderReceiver.cancel(MainActivity.this, id);
        } catch (Exception ignored) {
        }
      }
    }

    @JavascriptInterface
    public String settings(String raw) {
      if (!unlocked) return "Unlock the app first";
      synchronized (Store.class) {
        try {
          JSONObject next = new JSONObject(raw),
              d = Store.data(MainActivity.this),
              goals = next.getJSONObject("goals");
          if (goals.getInt("steps") < 100
              || goals.getInt("steps") > 100000
              || goals.getInt("water") < 100
              || goals.getInt("water") > 10000
              || goals.getDouble("sleep") < 1
              || goals.getDouble("sleep") > 24
              || next.getDouble("stride") < 0.2
              || next.getDouble("stride") > 2) return "Check your goals and stride length";
          String wu = next.optString("weightUnit", d.optString("weightUnit", "kg"));
          String du = next.optString("distanceUnit", d.optString("distanceUnit", "km"));
          double bw = next.optDouble("bodyWeightKg", d.optDouble("bodyWeightKg", 0));
          if (!(wu.equals("kg") || wu.equals("lbs")) || !(du.equals("km") || du.equals("mi"))
              || !Double.isFinite(bw) || bw < 0 || bw > 700) return "Check your units and body weight";
          d.put("weightUnit", wu);
          d.put("distanceUnit", du);
          d.put("bodyWeightKg", bw);
          if (next.has("setupComplete")) d.put("setupComplete", next.getBoolean("setupComplete"));
          d.put("goals", goals);
          d.put("name", next.getString("name"));
          d.put("stride", next.getDouble("stride"));
          Store.save(MainActivity.this, d);
          StepWidget.update(MainActivity.this);
          return "";
        } catch (Exception e) {
          return "Invalid settings";
        }
      }
    }

    @JavascriptInterface
    public void tracking(boolean enabled) {
      runOnUiThread(
          () -> {
            if (!unlocked) return;
            if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
              java.util.ArrayList<String> p = new java.util.ArrayList<>();
              p.add(android.Manifest.permission.ACTIVITY_RECOGNITION);
              if (Build.VERSION.SDK_INT >= 33)
                p.add(android.Manifest.permission.POST_NOTIFICATIONS);
              requestPermissions(p.toArray(new String[0]), 1);
            } else {
              Store.prefs(MainActivity.this).edit().putBoolean("tracking", true).commit();
              startSteps();
            }
          });
    }

    @JavascriptInterface
    public void notifications() {
      runOnUiThread(
          () -> {
            if (!unlocked) return;
            if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED)
              requestPermissions(new String[] {android.Manifest.permission.POST_NOTIFICATIONS}, 2);
            else
              startActivity(
                  new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                      .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName()));
          });
    }

    @JavascriptInterface
    public void battery() {
      runOnUiThread(
          () ->
              startActivity(
                  new Intent(
                      android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                      android.net.Uri.parse("package:" + getPackageName()))));
    }

    @JavascriptInterface
    public void export() {
      runOnUiThread(
          () -> {
            if (!unlocked) return;
            try {
              exportData = Store.snapshot(MainActivity.this).put("formatVersion", 1).toString(2);
              externalFlow = true;
              startActivityForResult(
                  new Intent(Intent.ACTION_CREATE_DOCUMENT)
                      .addCategory(Intent.CATEGORY_OPENABLE)
                      .setType("application/json")
                      .putExtra(Intent.EXTRA_TITLE, "my-health-" + Store.today() + ".json"),
                  10);
            } catch (Exception e) {
              toast("Could not prepare backup");
            }
          });
    }

    @JavascriptInterface
    public void restore() {
      runOnUiThread(
          () -> {
            if (!unlocked) return;
            externalFlow = true;
            startActivityForResult(
                new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/json"),
                11);
          });
    }

    @JavascriptInterface
    public String edit(String id, String raw) {
      if (!unlocked) return "Unlock the app first";
      synchronized (Store.class) {
        try {
          JSONObject patch = new JSONObject(raw), d = Store.data(MainActivity.this);
          JSONArray a = d.getJSONArray("entries");
          for (int i = 0; i < a.length(); i++) {
            JSONObject e = a.getJSONObject(i);
            if (!id.equals(e.getString("id"))) continue;
            for (java.util.Iterator<String> it = patch.keys(); it.hasNext(); ) {
              String key = it.next();
              if (java.util.Arrays.asList(
                      "value",
                      "label",
                      "date",
                      "note",
                      "distance",
                      "protein",
                      "carbs",
                      "fat",
                      "time",
                      "diastolic",
                      "unit")
                  .contains(key)) e.put(key, patch.get(key));
            }
            String invalid = RecordValidation.check(e);
            if (!invalid.isEmpty()) return invalid;
            e.put("edited", true);
            a.put(i, e);
            Store.save(MainActivity.this, d);
            ReminderReceiver.cancel(MainActivity.this, id);
            ReminderReceiver.schedule(MainActivity.this);
            return "";
          }
          return "Record no longer exists";
        } catch (Exception e) {
          return "Could not update entry";
        }
      }
    }

    @JavascriptInterface
    public void theme(String value) {
      if (!unlocked || !java.util.Arrays.asList("system", "light", "dark").contains(value)) return;
      Store.prefs(MainActivity.this).edit().putString("theme", value).apply();
      runOnUiThread(() -> applySystemTheme(value));
    }

    @JavascriptInterface
    public void hydration(boolean enabled, int minutes) {
      if (!unlocked || minutes < 30 || minutes > 240) return;
      Store.prefs(MainActivity.this)
          .edit()
          .putBoolean("hydration", enabled)
          .putInt("hydrationMinutes", minutes)
          .apply();
      ReminderReceiver.scheduleWater(MainActivity.this);
    }

    @JavascriptInterface
    public void widget() {
      runOnUiThread(
          () -> {
            if (unlocked) StepWidget.pin(MainActivity.this);
          });
    }

    @JavascriptInterface
    public void lock(boolean enabled) {
      runOnUiThread(
          () -> {
            if (!unlocked) return;
            if (enabled) authenticate(true);
            else {
              Store.prefs(MainActivity.this).edit().putBoolean("appLock", false).apply();
              getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
              StepWidget.update(MainActivity.this);
              js("refresh()");
            }
          });
    }

    @JavascriptInterface
    public void workout(String action, String label) {
      runOnUiThread(() -> startWorkout(action, label));
    }

    @JavascriptInterface
    public void report(String format) {
      MainActivity.this.report(format);
    }

    @JavascriptInterface
    public void scan() {
      if (!ProBilling.access(MainActivity.this)) {
        js("showPro()");
        return;
      }
      runOnUiThread(
          () -> {
            if (!unlocked) return;
            externalFlow = true;
            new com.google.zxing.integration.android.IntentIntegrator(MainActivity.this)
                .setCaptureActivity(FoodScannerActivity.class)
                .setDesiredBarcodeFormats(
                    com.google.zxing.integration.android.IntentIntegrator.PRODUCT_CODE_TYPES)
                .setPrompt("Scan a food barcode")
                .setBeepEnabled(false)
                .setOrientationLocked(false)
                .initiateScan();
          });
    }

    @JavascriptInterface
    public void lookup(String barcode) {
      if (!unlocked) return;
      if (!ProBilling.access(MainActivity.this)) {
        js("showPro()");
        return;
      }
      worker.execute(
          () -> {
            try {
              JSONObject product = FoodLookup.lookup(MainActivity.this, barcode);
              js("foodResult(" + product.toString() + ")");
            } catch (Exception e) {
              String message = e.getMessage();
              js(
                  "foodError("
                      + JSONObject.quote(
                          message == null
                              ? "Unable to look up product. Check your connection or enter it"
                                  + " manually."
                              : message)
                      + ")");
            }
          });
    }

    @JavascriptInterface
    public void healthConnect(boolean request) {
      runOnUiThread(
          () -> {
            if (!unlocked) return;
            if (Build.VERSION.SDK_INT < 34) {
              toast("This integration requires Android 14 or later");
              return;
            }
            if (request) requestPermissions(HealthSync.PERMISSIONS, 4);
            else HealthSync.sync(MainActivity.this);
          });
    }

    @JavascriptInterface
    public void buyPro() {
      runOnUiThread(
          () -> {
            if (unlocked) billing.buy();
          });
    }

    @JavascriptInterface
    public void restorePro() {
      runOnUiThread(
          () -> {
            if (unlocked) billing.start();
          });
    }

    @JavascriptInterface
    public void privacy() {
      runOnUiThread(
          () -> {
            if (unlocked) startActivity(new Intent(MainActivity.this, PrivacyActivity.class));
          });
    }

    @JavascriptInterface
    public void deleteData() {
      runOnUiThread(
          () -> {
            if (!unlocked) return;
            new AlertDialog.Builder(MainActivity.this)
                .setTitle("Delete all health data?")
                .setMessage(
                    "This removes your journal, step history, saved routes, profile and reminders"
                        + " from this phone. Export a backup first. Your Pro purchase and Android"
                        + " permissions are kept.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton(
                    "Delete data",
                    (dialog, which) -> {
                      if (!unlocked) return;
                      synchronized (Store.class) {
                        JSONArray entries = Store.data(MainActivity.this).optJSONArray("entries");
                        for (int n = 0; n < entries.length(); n++)
                          ReminderReceiver.cancel(
                              MainActivity.this, entries.optJSONObject(n).optString("id"));
                        stopService(new Intent(MainActivity.this, WorkoutService.class));
                        SharedPreferences.Editor editor = Store.prefs(MainActivity.this).edit();
                        for (String key : Store.prefs(MainActivity.this).getAll().keySet())
                          if (key.startsWith("steps:")
                              || key.startsWith("importSteps:")
                              || key.startsWith("food:")
                              || java.util.Arrays.asList(
                                      "data",
                                      "counter",
                                      "counterTimestamp",
                                      "workout",
                                      "deletedHealth",
                                      "lastSync",
                                      "syncStatus",
                                      "gpsStatus")
                                  .contains(key)) editor.remove(key);
                        editor.putBoolean("hydration", false).commit();
                        ReminderReceiver.schedule(MainActivity.this);
                        DailyBoundaryReceiver.refresh(MainActivity.this);
                        js("refresh()");
                        toast("Health data deleted");
                      }
                    })
                .show();
          });
    }
  }

  protected void onDestroy() {
    if (billing != null) billing.close();
    worker.shutdownNow();
    super.onDestroy();
  }

  private void applySystemTheme(String value) {
    boolean dark =
        value.equals("dark")
            || (value.equals("system")
                && (getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES);
    int color =
        dark ? android.graphics.Color.rgb(16, 27, 23) : android.graphics.Color.rgb(246, 247, 242);
    web.setBackgroundColor(color);
    root.setBackgroundColor(color);
    lockCover.setBackgroundColor(color);
    ((android.widget.TextView) ((android.widget.LinearLayout) lockCover).getChildAt(0))
        .setTextColor(dark ? android.graphics.Color.WHITE : android.graphics.Color.rgb(22, 61, 52));
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            dark
                ? 0
                : android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    getWindow().setStatusBarColor(color);
    getWindow().setNavigationBarColor(color);
  }

  protected void onActivityResult(int req, int result, Intent data) {
    super.onActivityResult(req, result, data);
    com.google.zxing.integration.android.IntentResult scan =
        com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(
            req, result, data);
    if (scan != null) {
      if (scan.getContents() != null)
        js("barcodeScanned(" + JSONObject.quote(scan.getContents()) + ")");
      return;
    }
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    try {
      if (req == 12) {
        try (java.io.OutputStream out = getContentResolver().openOutputStream(data.getData())) {
          out.write(documentBytes);
        }
        documentBytes = null;
        toast("Report saved");
      }
      if (req == 10) {
        try (java.io.OutputStream out = getContentResolver().openOutputStream(data.getData())) {
          out.write(exportData.getBytes(StandardCharsets.UTF_8));
        }
        toast("Backup saved");
      }
      if (req == 11) {
        JSONObject imported;
        try (java.io.InputStream in = getContentResolver().openInputStream(data.getData())) {
          java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
          byte[] chunk = new byte[8192];
          int count;
          while ((count = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, count);
            if (buffer.size() > 50_000_000) throw new Exception();
          }
          byte[] bytes = buffer.toByteArray();
          imported = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        }
        if (imported.getInt("formatVersion") != 1) throw new Exception();
        JSONArray entries = imported.getJSONArray("entries");
        JSONObject steps = imported.getJSONObject("steps"), goals = imported.getJSONObject("goals");
        if (goals.getInt("steps") < 100
            || goals.getInt("steps") > 100000
            || goals.getInt("water") < 100
            || goals.getInt("water") > 10000
            || goals.getDouble("sleep") < 1
            || goals.getDouble("sleep") > 24
            || imported.getDouble("stride") < 0.2
            || imported.getDouble("stride") > 2
            || imported.getString("name").length() > 40) throw new Exception();
        java.util.HashSet<String> ids = new java.util.HashSet<>();
        for (int n = 0; n < entries.length(); n++) {
          JSONObject e = entries.getJSONObject(n);
          String id = e.getString("id");
          if (!java.util.UUID.fromString(id).toString().equals(id) || !ids.add(id))
            throw new Exception();
          if (!java.util.Arrays.asList(
                  "water",
                  "weight",
                  "sleep",
                  "workout",
                  "food",
                  "bloodpressure",
                  "heartrate",
                  "glucose",
                  "temperature",
                  "mood",
                  "symptom",
                  "medication",
                  "period",
                  "note",
                  "dose")
              .contains(e.getString("type"))) throw new Exception();
          java.time.LocalDate.parse(e.getString("date"));
          if (e.getString("value").length() > 1000
              || e.optString("label").length() > 200
              || e.optString("note").length() > 4000) throw new Exception();
          if (e.getString("type").equals("medication"))
            java.time.LocalTime.parse(e.getString("time"));
          if (!RecordValidation.check(e).isEmpty()) throw new Exception();
          if (e.has("route")) {
            JSONArray route = e.getJSONArray("route");
            if (route.length() > 15000) throw new Exception();
            for (int p = 0; p < route.length(); p++) {
              JSONObject point = route.getJSONObject(p);
              double lat = point.getDouble("lat"), lon = point.getDouble("lon");
              if (!Double.isFinite(lat)
                  || !Double.isFinite(lon)
                  || Math.abs(lat) > 90
                  || Math.abs(lon) > 180) throw new Exception();
            }
          }
        }
        for (java.util.Iterator<String> keys = steps.keys(); keys.hasNext(); ) {
          String k = keys.next();
          java.time.LocalDate.parse(k);
          if (steps.getInt(k) < 0) throw new Exception();
        }
        final JSONObject restore = imported;
        new AlertDialog.Builder(this)
            .setTitle("Restore backup?")
            .setMessage("This replaces your current health records and step history.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton(
                "Restore",
                (dialog, which) -> {
                  if (!unlocked) {
                    toast("Unlock the app before restoring records");
                    return;
                  }
                  synchronized (Store.class) {
                    try {
                      JSONArray previous = Store.data(this).getJSONArray("entries");
                      for (int n = 0; n < previous.length(); n++)
                        ReminderReceiver.cancel(this, previous.getJSONObject(n).getString("id"));
                    } catch (Exception ignored) {
                    }
                    SharedPreferences.Editor edit = Store.prefs(this).edit();
                    for (String k : Store.prefs(this).getAll().keySet())
                      if (k.startsWith("steps:") || k.startsWith("importSteps:")) edit.remove(k);
                    for (java.util.Iterator<String> keys = steps.keys(); keys.hasNext(); ) {
                      String k = keys.next();
                      edit.putInt("steps:" + k, steps.optInt(k));
                    }
                    edit.commit();
                    restore.remove("steps");
                    Store.save(this, restore);
                    ReminderReceiver.schedule(this);
                    StepWidget.update(this);
                  }
                  web.evaluateJavascript("refresh()", null);
                  toast("Backup restored");
                })
            .show();
      }
    } catch (Exception e) {
      toast("Could not read or write this backup. Choose a My Health JSON backup.");
    }
  }

  // Legacy devices use this callback; API 33+ uses OnBackInvokedDispatcher above.
  @android.annotation.SuppressLint("GestureBackNavigation")
  public void onBackPressed() {
    web.evaluateJavascript("back()", null);
  }
}
