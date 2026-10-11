package com.pokeroutine.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL_SOUND = "pokeroutine_tasks_sound";
    static final String CHANNEL_SILENT = "pokeroutine_tasks_silent";

    static boolean soundEnabled(Context c) {
        return c.getSharedPreferences("pokeroutine_settings", 0).getBoolean("sound", true);
    }

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        AudioAttributes audio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        NotificationChannel sound = new NotificationChannel(CHANNEL_SOUND, "Lembretes com som", NotificationManager.IMPORTANCE_DEFAULT);
        sound.setDescription("Lembretes de tarefas com toque retro opcional");
        sound.setSound(Uri.parse("android.resource://" + c.getPackageName() + "/" + com.pokeroutine.app.R.raw.pokeroutine_alert), audio);
        NotificationChannel silent = new NotificationChannel(CHANNEL_SILENT, "Lembretes silenciosos", NotificationManager.IMPORTANCE_DEFAULT);
        silent.setDescription("Lembretes de tarefas sem som");
        silent.setSound(null, null);
        manager.createNotificationChannel(sound);
        manager.createNotificationChannel(silent);
    }

    static PendingIntent pending(Context c, String id, String title) {
        Intent intent = new Intent(c, ReminderReceiver.class).putExtra("id", id).putExtra("title", title);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, id.hashCode(), intent, flags);
    }

    static void schedule(Context c, String id, String title, long at) {
        AlarmManager manager = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent intent = pending(c, id, title);
        if (Build.VERSION.SDK_INT >= 31) {
            if (manager.canScheduleExactAlarms()) {
                try { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent); }
                catch (SecurityException e) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent); }
            } else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent);
        } else if (Build.VERSION.SDK_INT >= 23) {
            try { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent); }
            catch (SecurityException e) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent); }
        } else manager.setExact(AlarmManager.RTC_WAKEUP, at, intent);
    }

    static void cancel(Context c, String id) {
        AlarmManager manager = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pending(c, id, ""));
        try {
            String raw = c.getSharedPreferences("pokeroutine_alarms", 0).getString("items", "[]");
            JSONArray old = new JSONArray(raw), next = new JSONArray();
            for (int i = 0; i < old.length(); i++)
                if (!old.getJSONObject(i).optString("id").equals(id)) next.put(old.getJSONObject(i));
            c.getSharedPreferences("pokeroutine_alarms", 0).edit().putString("items", next.toString()).apply();
        } catch (Exception ignored) {}
    }

    @Override public void onReceive(Context c, Intent intent) {
        String id = intent.getStringExtra("id"), title = intent.getStringExtra("title");
        if (id == null) return;
        ensureChannel(c);
        int flags = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent launch = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), flags);
        boolean withSound = soundEnabled(c);
        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new android.app.Notification.Builder(c, withSound ? CHANNEL_SOUND : CHANNEL_SILENT)
                : new android.app.Notification.Builder(c);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Hora da sua tarefa")
                .setContentText(title == null ? "Sua rotina está esperando por você." : title)
                .setAutoCancel(true).setContentIntent(launch);
        if (Build.VERSION.SDK_INT < 26 && withSound)
            builder.setSound(Uri.parse("android.resource://" + c.getPackageName() + "/" + com.pokeroutine.app.R.raw.pokeroutine_alert));
        NotificationManager manager = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id.hashCode(), builder.build());
        cancel(c, id);
    }
}

