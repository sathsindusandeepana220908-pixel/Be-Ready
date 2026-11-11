package com.beready.almaster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        int id = intent.getIntExtra("id", 1001);
        MainActivity.notifyFromAlarm(context, title == null ? "BeReady reminder" : title, body == null ? "Time to study" : body, id);
    }
}
