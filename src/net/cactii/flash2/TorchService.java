/*
 * Copyright (C) 2010 Ben Buxton
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * This file is part of n1torch.
 *
 * n1torch is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * n1torch is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with n1torch.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.cactii.flash2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class TorchService extends Service {

    private static final String MSG_TAG = "TorchRoot";

    private Handler mHandler;

    private TimerTask mTorchTask;

    private Timer mTorchTimer;

    private WrapperTask mStrobeTask;

    private Timer mStrobeTimer;

    private NotificationManager mNotificationManager;

    private Notification mNotification;

    private Notification.Builder mNotificationBuilder;

    private boolean mBright;

    private int mStrobePeriod;

    private IntentReceiver mReceiver;

    private Runnable mStrobeRunnable;

    private Context mContext;

    public void onCreate() {
        String ns = Context.NOTIFICATION_SERVICE;
        mNotificationManager = (NotificationManager) getSystemService(ns);
        mContext = getApplicationContext();

        mHandler = new Handler() {
        };

        mTorchTask = new TimerTask() {
            public void run() {
                FlashDevice.instance(mContext).setFlashMode(mBright ? FlashDevice.DEATH_RAY : FlashDevice.ON);
            }
        };
        mTorchTimer = new Timer();

        mStrobeRunnable = new Runnable() {
            private int mCounter = 4;

            @Override
            public void run() {
                int flashMode = mBright ? FlashDevice.DEATH_RAY : FlashDevice.ON;
                if (FlashDevice.instance(mContext).getFlashMode() < flashMode) {
                    if (mCounter-- < 1) {
                        FlashDevice.instance(mContext).setFlashMode(flashMode);
                    }
                } else {
                    FlashDevice.instance(mContext).setFlashMode(FlashDevice.STROBE);
                    mCounter = 4;
                }
            }

        };
        mStrobeTask = new WrapperTask(mStrobeRunnable);

        mStrobeTimer = new Timer();

    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(MSG_TAG, "Starting torch");
        if (intent == null)
            stopSelf();
        mBright = intent.getBooleanExtra("bright", false);
        if (intent.getBooleanExtra("strobe", false)) {
            mStrobePeriod = intent.getIntExtra("period", 200) / 4;
            mStrobeTimer.schedule(this.mStrobeTask, 0, this.mStrobePeriod);
        } else {
            this.mTorchTimer.schedule(this.mTorchTask, 0, 100);
        }

        mReceiver = new IntentReceiver();
        registerReceiver(mReceiver, new IntentFilter("net.cactii.flash2.SET_STROBE"));

        mNotificationBuilder = new Notification.Builder(this);
        mNotificationBuilder.setSmallIcon(R.drawable.notification_icon);
        mNotificationBuilder.setTicker(getString(R.string.not_torch_title));
        mNotificationBuilder.setContentTitle(getString(R.string.not_torch_title));
        mNotificationBuilder.setContentText(getString(R.string.not_torch_summary));
        mNotificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this,
                MainActivity.class), 0));
        mNotificationBuilder.setAutoCancel(false);
        mNotificationBuilder.setOngoing(true);

        mNotification = mNotificationBuilder.getNotification();
        mNotificationManager.notify(getString(R.string.app_name).hashCode(), mNotification);

        startForeground(getString(R.string.app_name).hashCode(), mNotification);
        Settings.System.putInt(getContentResolver(), Settings.System.TORCH_STATE, 1);
        sendBroadcast(new Intent(TorchSwitch.TORCH_STATE_CHANGED));
        return START_STICKY;
    }

    public void onDestroy() {
        mNotificationManager.cancelAll();
        unregisterReceiver(mReceiver);
        stopForeground(true);
        mTorchTimer.cancel();
        mStrobeTimer.cancel();
        FlashDevice.instance(mContext).setFlashMode(FlashDevice.OFF);
        Settings.System.putInt(getContentResolver(), Settings.System.TORCH_STATE, 0);
        sendBroadcast(new Intent(TorchSwitch.TORCH_STATE_CHANGED));
    }

    public void Reshedule(int period) {
        mStrobeTask.cancel();
        mStrobeTask = new WrapperTask(mStrobeRunnable);

        mStrobePeriod = period / 4;
        mStrobeTimer.schedule(mStrobeTask, 0, mStrobePeriod);
    }

    public class WrapperTask extends TimerTask {
        private final Runnable mTarget;

        public WrapperTask(Runnable target) {
            mTarget = target;
        }

        public void run() {
            mTarget.run();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class IntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, final Intent intent) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    Reshedule(intent.getIntExtra("period", 200));
                }

            });
        }
    }
}
