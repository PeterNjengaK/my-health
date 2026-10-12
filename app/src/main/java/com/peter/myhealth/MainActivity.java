package com.peter.myhealth;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.webkit.*;
import android.widget.Toast;
import org.json.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView web;
    private String exportData;
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(246,247,242));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(246,247,242));
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        web = new WebView(this);
        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) WebView.setWebContentsDebuggingEnabled(true);
        web.setBackgroundColor(android.graphics.Color.rgb(246,247,242));
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setBlockNetworkLoads(true);
        web.addJavascriptInterface(new Bridge(), "Health");
        web.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return true; }
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                String path = r.getUrl().getPath();
                if ("app.health.local".equals(r.getUrl().getHost()) && path != null && !path.contains("..")) {
                    try { String mime = path.endsWith(".js") ? "application/javascript" : path.endsWith(".css") ? "text/css" : "text/html";
                        return new WebResourceResponse(mime, "UTF-8", getAssets().open(path.substring(1))); }
                    catch (Exception ignored) {}
                }
                return new WebResourceResponse("text/plain", "UTF-8", new java.io.ByteArrayInputStream(new byte[0]));
            }
        });
        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.addView(web, new android.widget.FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        // Keep the interface inside system bars, including Android's enforced edge-to-edge layout.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
        web.loadUrl("https://app.health.local/index.html");
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> web.evaluateJavascript("back()", null));
        if (Store.prefs(this).getBoolean("tracking", false)) startSteps();
        ReminderReceiver.schedule(this);
    }
    void toast(String s) { runOnUiThread(() -> Toast.makeText(this, s, Toast.LENGTH_LONG).show()); }
    void startSteps() {
        if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
            startForegroundService(new Intent(this, StepService.class));
    }
    public void onRequestPermissionsResult(int req, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(req, permissions, results);
        if (req == 1 && checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            Store.prefs(this).edit().putBoolean("tracking", true).commit(); startSteps();
        }
        web.evaluateJavascript("refresh()", null);
    }
    final class Bridge {
        @JavascriptInterface public String state() { try { return Store.snapshot(MainActivity.this).toString(); } catch (Exception e) { return "{}"; } }
        @JavascriptInterface public String add(String raw) {
            synchronized (Store.class) { try {
                JSONObject e = new JSONObject(raw), d = Store.data(MainActivity.this);
                String type = e.getString("type");
                if (!java.util.Set.of("water","weight","sleep","workout","food","bloodpressure","heartrate","glucose","temperature","mood","symptom","medication","period","note","dose").contains(type)) return "Invalid category";
                java.time.LocalDate.parse(e.getString("date"));
                if (e.optString("value").length()>1000 || e.optString("label").length()>200 || e.optString("note").length()>4000) return "Entry too long";
                e.put("id", java.util.UUID.randomUUID().toString()); e.put("created", System.currentTimeMillis());
                d.getJSONArray("entries").put(e); Store.save(MainActivity.this, d); ReminderReceiver.schedule(MainActivity.this); return "";
            } catch (Exception e) { return "Could not save entry"; } }
        }
        @JavascriptInterface public void delete(String id) {
            synchronized(Store.class) { try {
                JSONObject d=Store.data(MainActivity.this); JSONArray a=d.getJSONArray("entries"), out=new JSONArray();
                for(int i=0;i<a.length();i++) if(!a.getJSONObject(i).getString("id").equals(id)) out.put(a.get(i));
                d.put("entries",out); Store.save(MainActivity.this,d); ReminderReceiver.cancel(MainActivity.this,id);
            } catch(Exception ignored) {} }
        }
        @JavascriptInterface public String settings(String raw) {
            synchronized(Store.class) { try {
                JSONObject next=new JSONObject(raw), d=Store.data(MainActivity.this), goals=next.getJSONObject("goals");
                if(goals.getInt("steps")<100 || goals.getInt("steps")>100000 || goals.getInt("water")<100 || goals.getInt("water")>10000 || goals.getDouble("sleep")<1 || goals.getDouble("sleep")>24 || next.getDouble("stride")<0.2 || next.getDouble("stride")>2) return "Check your goals and stride length";
                d.put("goals",goals); d.put("name",next.getString("name")); d.put("stride",next.getDouble("stride")); Store.save(MainActivity.this,d); return "";
            } catch(Exception e) { return "Invalid settings"; } }
        }
        @JavascriptInterface public void tracking(boolean enabled) { runOnUiThread(() -> {
            if (!enabled) { Store.prefs(MainActivity.this).edit().putBoolean("tracking",false).remove("counter").commit(); stopService(new Intent(MainActivity.this,StepService.class)); return; }
            if(checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED) {
                java.util.ArrayList<String> p=new java.util.ArrayList<>(); p.add(android.Manifest.permission.ACTIVITY_RECOGNITION);
                if(Build.VERSION.SDK_INT>=33) p.add(android.Manifest.permission.POST_NOTIFICATIONS);
                requestPermissions(p.toArray(new String[0]),1);
            } else { Store.prefs(MainActivity.this).edit().putBoolean("tracking",true).commit(); startSteps(); }
        }); }
        @JavascriptInterface public void notifications() { runOnUiThread(() -> {
            if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},2);
            else startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName()));
        }); }
        @JavascriptInterface public void battery() { runOnUiThread(() -> startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())))); }
        @JavascriptInterface public void export() { runOnUiThread(() -> {
            try { exportData=Store.snapshot(MainActivity.this).put("formatVersion",1).toString(2);
                startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"my-health-"+Store.today()+".json"),10);
            } catch(Exception e) { toast("Could not prepare backup"); }
        }); }
        @JavascriptInterface public void restore() { runOnUiThread(() -> startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json"),11)); }
    }
    protected void onActivityResult(int req,int result,Intent data) {
        super.onActivityResult(req,result,data);
        if(result!=RESULT_OK || data==null || data.getData()==null) return;
        try {
            if(req==10) { try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())) { out.write(exportData.getBytes(StandardCharsets.UTF_8)); } toast("Backup saved"); }
            if(req==11) {
                JSONObject imported;
                try(java.io.InputStream in=getContentResolver().openInputStream(data.getData())) {
                    java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream(); byte[] chunk=new byte[8192]; int count;
                    while((count=in.read(chunk))!=-1) { buffer.write(chunk,0,count); if(buffer.size()>5_000_000) throw new Exception(); }
                    byte[] bytes=buffer.toByteArray();
                    imported=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
                }
                if(imported.getInt("formatVersion")!=1) throw new Exception();
                JSONArray entries=imported.getJSONArray("entries"); JSONObject steps=imported.getJSONObject("steps"), goals=imported.getJSONObject("goals");
                if(goals.getInt("steps")<100 || goals.getInt("steps")>100000 || goals.getInt("water")<100 || goals.getInt("water")>10000 || goals.getDouble("sleep")<1 || goals.getDouble("sleep")>24 || imported.getDouble("stride")<0.2 || imported.getDouble("stride")>2 || imported.getString("name").length()>40) throw new Exception();
                java.util.HashSet<String> ids=new java.util.HashSet<>();
                for(int n=0;n<entries.length();n++) {
                    JSONObject e=entries.getJSONObject(n); String id=e.getString("id");
                    if(!java.util.UUID.fromString(id).toString().equals(id) || !ids.add(id)) throw new Exception();
                    if(!java.util.Arrays.asList("water","weight","sleep","workout","food","bloodpressure","heartrate","glucose","temperature","mood","symptom","medication","period","note","dose").contains(e.getString("type"))) throw new Exception();
                    java.time.LocalDate.parse(e.getString("date"));
                    if(e.getString("value").length()>1000 || e.optString("label").length()>200 || e.optString("note").length()>4000) throw new Exception();
                    if(e.getString("type").equals("medication")) java.time.LocalTime.parse(e.getString("time"));
                }
                for(java.util.Iterator<String> keys=steps.keys();keys.hasNext();) { String k=keys.next(); java.time.LocalDate.parse(k); if(steps.getInt(k)<0) throw new Exception(); }
                final JSONObject restore=imported;
                new AlertDialog.Builder(this).setTitle("Restore backup?").setMessage("This replaces your current health records and step history.").setNegativeButton("Cancel",null).setPositiveButton("Restore",(dialog,which)-> {
                    synchronized(Store.class) {
                        try { JSONArray previous=Store.data(this).getJSONArray("entries"); for(int n=0;n<previous.length();n++) ReminderReceiver.cancel(this,previous.getJSONObject(n).getString("id")); } catch(Exception ignored) {}
                        SharedPreferences.Editor edit=Store.prefs(this).edit(); for(String k:Store.prefs(this).getAll().keySet()) if(k.startsWith("steps:")) edit.remove(k);
                        for(java.util.Iterator<String> keys=steps.keys();keys.hasNext();) { String k=keys.next(); edit.putInt("steps:"+k,steps.optInt(k)); } edit.commit();
                        restore.remove("steps"); Store.save(this,restore); ReminderReceiver.schedule(this);
                    } web.evaluateJavascript("refresh()",null); toast("Backup restored");
                }).show();
            }
        } catch(Exception e) { toast("Could not read or write this backup. Choose a My Health JSON backup."); }
    }
    // Legacy devices use this callback; API 33+ uses OnBackInvokedDispatcher above.
    @android.annotation.SuppressLint("GestureBackNavigation")
    public void onBackPressed() { web.evaluateJavascript("back()",null); }
}
