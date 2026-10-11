package com.pokeroutine.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView web;
    private static final String PREFS = "pokeroutine_alarms";
    private static final String ALARMS = "items";
    private static final int NOTIFICATION_REQUEST = 41;
    private static final int EXPORT_REQUEST = 42;
    private static final int IMPORT_REQUEST = 43;
    private String pendingBackup;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(Color.rgb(16,43,41));
        getWindow().setNavigationBarColor(Color.rgb(16,43,41));
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(244,237,207));
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(false);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
        ReminderReceiver.ensureChannel(this);
    }

    @Override protected void onResume() { super.onResume(); syncAlarmPermission(); }
    private void syncAlarmPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
            if (am.canScheduleExactAlarms()) rescheduleStoredAlarms();
        }
    }
    private void rescheduleStoredAlarms() {
        try { JSONArray a=new JSONArray(getSharedPreferences(PREFS,0).getString(ALARMS,"[]")); long now=System.currentTimeMillis();
            for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);if(x.optLong("at")>now)ReminderReceiver.schedule(this,x.optString("id"),x.optString("title"),x.optLong("at"));}
        } catch(Exception ignored) {}
    }
    @Override public void onBackPressed() {
        web.evaluateJavascript("document.querySelector('#modal')?.classList.add('hidden')", null);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == NOTIFICATION_REQUEST && web != null) {
            boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
            web.evaluateJavascript("window.pokeroutineNotificationResult && window.pokeroutineNotificationResult(" + granted + ")", null);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 44) { syncAlarmPermission(); return; }
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == EXPORT_REQUEST && pendingBackup != null) {
                OutputStream out = getContentResolver().openOutputStream(uri);
                if (out != null) { out.write(pendingBackup.getBytes("UTF-8")); out.close(); }
                pendingBackup = null;
                Toast.makeText(this, "Backup salvo.", Toast.LENGTH_SHORT).show();
            } else if (requestCode == IMPORT_REQUEST) {
                InputStream in = getContentResolver().openInputStream(uri);
                if (in == null) return;
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096]; int n;
                while ((n = in.read(buf)) > 0) bytes.write(buf,0,n);
                in.close();
                web.evaluateJavascript("window.pokeroutineImportBackup && window.pokeroutineImportBackup(" + JSONObject.quote(bytes.toString("UTF-8")) + ")", null);
            }
        } catch (Exception e) { Toast.makeText(this,"Não foi possível acessar o arquivo.",Toast.LENGTH_SHORT).show(); }
    }

    public final class Bridge {
        @JavascriptInterface public boolean requestNotifications() {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                runOnUiThread(() -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST));
            } else if (web != null) {
                runOnUiThread(() -> web.evaluateJavascript("window.pokeroutineNotificationResult && window.pokeroutineNotificationResult(true)", null));
            }
            return true;
        }
        @JavascriptInterface public void scheduleReminder(String id, String title, long at) {
            try {
                JSONObject item = new JSONObject(); item.put("id", id); item.put("title", title); item.put("at", at);
                JSONArray old = new JSONArray(getSharedPreferences(PREFS,0).getString(ALARMS,"[]"));
                JSONArray next = new JSONArray();
                for (int i=0;i<old.length();i++) if (!old.getJSONObject(i).optString("id").equals(id)) next.put(old.getJSONObject(i));
                next.put(item);
                getSharedPreferences(PREFS,0).edit().putString(ALARMS,next.toString()).apply();
                ReminderReceiver.schedule(MainActivity.this, id, title, at);
            } catch (Exception ignored) {}
        }
        @JavascriptInterface public void cancelReminder(String id) { ReminderReceiver.cancel(MainActivity.this,id); }
        @JavascriptInterface public void setSoundEnabled(boolean enabled) { getSharedPreferences("pokeroutine_settings",0).edit().putBoolean("sound",enabled).apply(); }
        @JavascriptInterface public boolean requestExactAlarmAccess() {
            if (Build.VERSION.SDK_INT >= 31) { AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE); if(!am.canScheduleExactAlarms()) {
                try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName()))); } catch(Exception ignored) {} return false; } } return true;
        }
        @JavascriptInterface public void toast(String message) { runOnUiThread(() -> Toast.makeText(MainActivity.this,message,Toast.LENGTH_SHORT).show()); }
        @JavascriptInterface public void exportBackup(String json) {
            pendingBackup = json;
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"pokeroutine-backup.json");
            runOnUiThread(() -> startActivityForResult(i,EXPORT_REQUEST));
        }
        @JavascriptInterface public void importBackup() {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");
            runOnUiThread(() -> startActivityForResult(i,IMPORT_REQUEST));
        }
    }
}
