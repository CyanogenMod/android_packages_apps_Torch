/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package net.cactii.flash2;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class TorchService extends Service {
    private static final String MSG_TAG = "TorchRoot";

    private int mFlashMode;
    private FlashDevice mFlashDevice;
    private boolean mUseCameraInterface;

    private static final int MSG_UPDATE_FLASH = 1;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_FLASH:
                    setFlashModeOrStop();
                    sendEmptyMessageDelayed(MSG_UPDATE_FLASH, 100);
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        mFlashDevice = FlashDevice.instance(this);
        mUseCameraInterface = getResources().getBoolean(R.bool.useCameraInterface);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(MSG_TAG, "Starting torch");

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        mFlashMode = intent.getBooleanExtra("bright", false)
                ? FlashDevice.DEATH_RAY : FlashDevice.ON;
        if (mUseCameraInterface) {
            // Devices with camera interface don't need constant refresh, so don't do
            // it in order to avoid the refresh from interfering with the synchronous
            // torch shutdown when starting the camera app
            setFlashModeOrStop();
        } else {
            mHandler.sendEmptyMessage(MSG_UPDATE_FLASH);
        }

        startForeground(getString(R.string.app_name).hashCode(), getNotification());
        updateState(true);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        mHandler.removeCallbacksAndMessages(null);
        FlashDevice.instance(this).setFlashMode(FlashDevice.OFF);
        updateState(false);
        super.onDestroy();
    }

    private Notification getNotification() {
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0, new Intent(this, MainActivity.class), 0);
        PendingIntent turnOffIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(TorchSwitch.TOGGLE_FLASHLIGHT), 0);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getString(R.string.not_torch_title))
                .setContentIntent(contentIntent)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(R.drawable.ic_appwidget_torch_off_small,
                        getString(R.string.not_torch_toggle), turnOffIntent)
                .build();
        return notification;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setFlashModeOrStop() {
        try {
            mFlashDevice.setFlashMode(mFlashMode);
        } catch (FlashDevice.InitializationException e) {
            Log.w(MSG_TAG, "Could not set flash mode " + mFlashMode, e);
            stopSelf();
        }
    }

    private void updateState(boolean on) {
        Intent intent = new Intent(TorchSwitch.TORCH_STATE_CHANGED);
        intent.putExtra("state", on ? 1 : 0);
        sendStickyBroadcast(intent);
    }
}
