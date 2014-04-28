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

    private static final int MSG_UPDATE_FLASH = 1;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final FlashDevice flash = FlashDevice.instance(TorchService.this);

            switch (msg.what) {
                case MSG_UPDATE_FLASH:
                    flash.setFlashMode(mFlashMode);
                    removeMessages(MSG_UPDATE_FLASH);
                    sendEmptyMessageDelayed(MSG_UPDATE_FLASH, 100);
                    break;
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(MSG_TAG, "Starting torch");

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        mFlashMode = intent.getBooleanExtra("bright", false)
                ? FlashDevice.DEATH_RAY : FlashDevice.ON;

        mHandler.sendEmptyMessage(MSG_UPDATE_FLASH);

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

    private void updateState(boolean on) {
        Intent intent = new Intent(TorchSwitch.TORCH_STATE_CHANGED);
        intent.putExtra("state", on ? 1 : 0);
        sendStickyBroadcast(intent);
    }
}
