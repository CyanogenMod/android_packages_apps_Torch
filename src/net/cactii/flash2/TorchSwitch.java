/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.cactii.flash2;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

public class TorchSwitch extends BroadcastReceiver {

    public static final String TOGGLE_FLASHLIGHT = "net.cactii.flash2.TOGGLE_FLASHLIGHT";
    public static final String TORCH_STATE_CHANGED = "net.cactii.flash2.TORCH_STATE_CHANGED";

    private SharedPreferences mPrefs;

    @Override
    public void onReceive(Context context, Intent receivingIntent) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
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
            Intent i = new Intent(context, TorchService.class);
            if (this.TorchServiceRunning(context)) {
                context.stopService(i);
            } else {
                i.putExtra("bright", bright);
                i.putExtra("strobe", strobe);
                i.putExtra("period", period);
                context.startService(i);
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
