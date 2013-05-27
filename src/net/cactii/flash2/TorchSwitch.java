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
import android.preference.PreferenceManager;
import android.provider.Settings;

public class TorchSwitch extends BroadcastReceiver {

    public static final String TOGGLE_FLASHLIGHT = "net.cactii.flash2.TOGGLE_FLASHLIGHT";
    public static final String TURNOFF_FLASHLIGHT = "net.cactii.flash2.TURNOFF_FLASHLIGHT";
    public static final String TORCH_STATE_CHANGED = "net.cactii.flash2.TORCH_STATE_CHANGED";

    private SharedPreferences mPrefs;

    @Override
    public void onReceive(Context context, Intent receivingIntent) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Intent i = new Intent(context, TorchService.class);
        if (receivingIntent.getAction().equals(TOGGLE_FLASHLIGHT)) {
            // bright setting can come from intent or from prefs depending on
            // on what send the broadcast
            //
            // Unload intent extras if they exist:
            boolean bright = receivingIntent.getBooleanExtra("bright", false) |
                    mPrefs.getBoolean("bright", false);
            boolean strobe = receivingIntent.getBooleanExtra("strobe", false) |
                    mPrefs.getBoolean("strobe", false);
            int period = receivingIntent.getIntExtra("period", 200);
            if (isTorchServiceRunning(context)) {
                context.stopService(i);
            } else {
                // Just ensure a correct behaviour
                Settings.System.putInt(context.getContentResolver(), Settings.System.TORCH_STATE, 0);

                i.putExtra("bright", bright);
                i.putExtra("strobe", strobe);
                i.putExtra("period", period);
                context.startService(i);
            }
        } else if (receivingIntent.getAction().equals(TURNOFF_FLASHLIGHT)) {
            context.stopService(i);
        }
    }

    private boolean isTorchServiceRunning(Context context) {
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
