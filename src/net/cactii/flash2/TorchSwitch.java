package net.cactii.flash2;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class TorchSwitch extends BroadcastReceiver {

    public static final String TOGGLE_FLASHLIGHT = "net.cactii.flash2.TOGGLE_FLASHLIGHT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TOGGLE_FLASHLIGHT)) {
            if (this.TorchServiceRunning(context)) {
                context.stopService(new Intent(context, TorchService.class));
                return;
            }
            Intent pendingIntent = new Intent(context, TorchService.class);
            if (this.TorchServiceRunning(context)) {
                context.stopService(pendingIntent);
            } else {
                context.startService(pendingIntent);
            }
        }
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
