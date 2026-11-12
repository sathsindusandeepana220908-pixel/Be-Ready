package com.beready.almaster;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Map;

public class ReminderBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        Map<String, ?> saved = context.getSharedPreferences("beready_alarms", Context.MODE_PRIVATE).getAll();
        AlarmManager alarms = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        for (Map.Entry<String, ?> entry : saved.entrySet()) {
            try {
                String[] parts = String.valueOf(entry.getValue()).split("\\n", 4);
                if (parts.length < 3) continue;
                long at = Long.parseLong(parts[0]);
                int id = Integer.parseInt(entry.getKey());
                boolean weekly = parts.length > 3 && "weekly".equals(parts[3]);
                if (at <= System.currentTimeMillis() && !weekly) continue;
                Intent reminder = new Intent(context, ReminderReceiver.class);
                reminder.putExtra("title", parts[1]);
                reminder.putExtra("body", parts[2]);
                reminder.putExtra("id", id);
                reminder.putExtra("weekly", weekly);
                PendingIntent pending = PendingIntent.getBroadcast(context, id, reminder, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (weekly) alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, Math.max(at, System.currentTimeMillis() + 60000L), AlarmManager.INTERVAL_DAY * 7L, pending);
                else if (Build.VERSION.SDK_INT >= 23) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
                else alarms.set(AlarmManager.RTC_WAKEUP, at, pending);
            } catch (Exception ignored) {}
        }
    }
}
