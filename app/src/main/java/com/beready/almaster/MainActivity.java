package com.beready.almaster;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView web;
    private static final int PICK_FILE = 41;
    private static final String CHANNEL = "beready_study";
    private static MainActivity instance;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        instance = this;
        getWindow().setStatusBarColor(android.graphics.Color.rgb(255,251,246));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(255,251,246));
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        createChannel();
        web = new WebView(this);
        web.setBackgroundColor(android.graphics.Color.rgb(255,251,246));
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(this), "BeReady");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Study reminders", NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("Study timer and timetable reminders");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }

    public static void notifyFromAlarm(Context context, String title, String body, int id) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(context, CHANNEL)
            : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body).setAutoCancel(true);
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, b.build());
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri u = data.getData();
            getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String name = u.getLastPathSegment() == null ? "Document" : u.getLastPathSegment().replace(":", "_");
            String js = "window.onPickedDocument && window.onPickedDocument(" + org.json.JSONObject.quote(name) + "," + org.json.JSONObject.quote(u.toString()) + ")";
            web.evaluateJavascript(js, null);
        }
    }

    @Override public void onBackPressed() {
        if (web != null) web.evaluateJavascript("if(window.goBackInApp){goBackInApp()}else{history.back()}", null);
        else super.onBackPressed();
    }

    public class Bridge {
        private final Context ctx;
        Bridge(Context c) { ctx = c; }

        @JavascriptInterface public void vibrate(long ms) {
            try {
                Vibrator v = (Vibrator)ctx.getSystemService(VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(Math.min(ms,1000), VibrationEffect.DEFAULT_AMPLITUDE));
                    else v.vibrate(Math.min(ms,1000));
                }
            } catch (Exception ignored) {}
        }

        @JavascriptInterface public void notifyNow(String title, String body) {
            notifyFromAlarm(ctx, title, body, (int)(System.currentTimeMillis() & 0x7fffffff));
        }

        @JavascriptInterface public void scheduleReminder(long atMillis, String title, String body, int id) {
            try {
                ctx.getSharedPreferences("beready_alarms", Context.MODE_PRIVATE).edit().putString(String.valueOf(id), atMillis + "\n" + title + "\n" + body).apply();
                Intent i = new Intent(ctx, ReminderReceiver.class);
                i.putExtra("title", title); i.putExtra("body", body); i.putExtra("id", id);
                PendingIntent p = PendingIntent.getBroadcast(ctx, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                AlarmManager a = (AlarmManager)ctx.getSystemService(ALARM_SERVICE);
                if (a != null && atMillis > System.currentTimeMillis()) {
                    if (Build.VERSION.SDK_INT >= 23) a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, p);
                    else a.set(AlarmManager.RTC_WAKEUP, atMillis, p);
                }
            } catch (Exception e) { Toast.makeText(ctx, "Could not schedule reminder", Toast.LENGTH_SHORT).show(); }
        }

        @JavascriptInterface public void openDocument(String uri) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } catch (Exception e) {
                Toast.makeText(ctx, "No app can open this file", Toast.LENGTH_SHORT).show();
            }
        }

        @JavascriptInterface public void scheduleWeeklyReminder(long atMillis, String title, String body, int id) {
            try {
                ctx.getSharedPreferences("beready_alarms", Context.MODE_PRIVATE).edit().putString(String.valueOf(id), atMillis + "\n" + title + "\n" + body + "\nweekly").apply();
                Intent i = new Intent(ctx, ReminderReceiver.class);
                i.putExtra("title", title); i.putExtra("body", body); i.putExtra("id", id); i.putExtra("weekly", true);
                PendingIntent p = PendingIntent.getBroadcast(ctx, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                AlarmManager a = (AlarmManager)ctx.getSystemService(ALARM_SERVICE);
                if (a != null && atMillis > System.currentTimeMillis()) {
                    a.setInexactRepeating(AlarmManager.RTC_WAKEUP, atMillis, AlarmManager.INTERVAL_DAY * 7L, p);
                }
            } catch (Exception e) { Toast.makeText(ctx, "Could not schedule weekly reminder", Toast.LENGTH_SHORT).show(); }
        }

        @JavascriptInterface public void cancelReminder(int id) {
            ctx.getSharedPreferences("beready_alarms", Context.MODE_PRIVATE).edit().remove(String.valueOf(id)).apply();
            Intent i = new Intent(ctx, ReminderReceiver.class);
            PendingIntent p = PendingIntent.getBroadcast(ctx, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager a = (AlarmManager)ctx.getSystemService(ALARM_SERVICE);
            if (a != null) a.cancel(p);
        }

        @JavascriptInterface public void chooseDocument() {
            if (instance == null) return;
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(i, PICK_FILE);
            });
        }
    }
}
