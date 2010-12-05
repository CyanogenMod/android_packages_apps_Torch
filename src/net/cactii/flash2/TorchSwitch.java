package net.cactii.flash2;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;

public class TorchSwitch extends BroadcastReceiver {

    public static final String TOGGLE_FLASHLIGHT = "net.cactii.flash2.TOGGLE_FLASHLIGHT";
    public static final String TORCH_STATE_CHANGED = "net.cactii.flash2.TORCH_STATE_CHANGED";
    public static final String TORCH_WIDGET_PRESSED = "net.cactii.flash2.TORCH_WIDGET_PRESSED";

    private SharedPreferences mPrefs;

    @Override
    public void onReceive(Context context, Intent receivingIntent) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (receivingIntent.getAction().equals(TOGGLE_FLASHLIGHT)) {
            boolean bright = receivingIntent.getBooleanExtra("bright", false);
            Intent i = new Intent(context, TorchService.class);
            if (this.TorchServiceRunning(context)) {
                context.stopService(i);
                Settings.System.putInt(context.getContentResolver(), Settings.System.TORCH_STATE, 0);
            } else {
                if (receivingIntent.hasExtra("bright")) {
                    i.putExtra("bright", bright);
                } else {
                    i.putExtra("bright", mPrefs.getBoolean("bright", false));
                }
                context.startService(i);
                Settings.System.putInt(context.getContentResolver(), Settings.System.TORCH_STATE, 1);
            }
        } else if (receivingIntent.getAction().equals(TORCH_WIDGET_PRESSED)) {
            // This is only to update state and broadcast to the other system widgets
            // This package's torch widget does it's own service handling so it can allow strobe, etc..
            // All we want with this is to make sure everything else changes graphic properly
            if (this.TorchServiceRunning(context)) {
                Settings.System.putInt(context.getContentResolver(), Settings.System.TORCH_STATE, 1);
            } else {
                Settings.System.putInt(context.getContentResolver(), Settings.System.TORCH_STATE, 0);
            }
        }
        context.sendBroadcast(new Intent(TORCH_STATE_CHANGED));
    }

    private boolean TorchServiceRunning(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> svcList = am.getRunningServices(100);

        if (!(svcList.size() > 0))
            return false;
        for (RunningServiceInfo serviceInfo : svcList) {
            ComponentName serviceName = serviceInfo.service;
            if (serviceName.getClassName().endsWith(".TorchService")
                    || serviceName.getClassName().endsWith(".RootTorchService"))
                return true;
        }
        return false;
    }
}
