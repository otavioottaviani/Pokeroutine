package com.pokeroutine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent) {
        if(!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        ReminderReceiver.ensureChannel(c);
        try {
            JSONArray a=new JSONArray(c.getSharedPreferences("pokeroutine_alarms",0).getString("items","[]"));
            long now=System.currentTimeMillis();
            for(int i=0;i<a.length();i++) {
                JSONObject x=a.getJSONObject(i); long at=x.optLong("at");
                if(at>now) ReminderReceiver.schedule(c,x.optString("id"),x.optString("title"),at);
            }
        } catch(Exception ignored) {}
    }
}
