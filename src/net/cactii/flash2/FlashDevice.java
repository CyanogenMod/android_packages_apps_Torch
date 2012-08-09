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

import java.io.FileWriter;
import java.io.IOException;
import android.hardware.Camera;
import net.cactii.flash2.R;
import android.content.Context;
import android.util.Log;

import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class FlashDevice {

    private static final String MSG_TAG = "TorchDevice";

    /* New variables, init'ed by resource items */
    private static int sValueOn;
    private static int sValueHigh;
    private static int sValueDeathRay;
    private static String sFlashDevice;
    private static boolean sUseCameraInterface;
    private WakeLock mWakeLock;

    public static final int STROBE = -1;
    public static final int OFF = 0;
    public static final int ON = 1;
    public static final int HIGH = 128;
    public static final int DEATH_RAY = 3;

    private static FlashDevice sInstance;

    private FileWriter mWriter = null;

    private int mFlashMode = OFF;

    private Camera mCamera = null;
    private Camera.Parameters mParams;

    private FlashDevice(Context context) {
        sValueOn = context.getResources().getInteger(R.integer.valueOn);
        sValueHigh = context.getResources().getInteger(R.integer.valueHigh);
        sValueDeathRay = context.getResources().getInteger(R.integer.valueDeathRay);
        sFlashDevice = context.getResources().getString(R.string.flashDevice);
        sUseCameraInterface = context.getResources().getBoolean(R.bool.useCameraInterface);
        if (sUseCameraInterface) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Torch");
        }
    }

    public static synchronized FlashDevice instance(Context context) {
        if (sInstance == null) {
            sInstance = new FlashDevice(context);
        }
        return sInstance;
    }

    public synchronized void setFlashMode(int mode) {
        try {
            int value = mode;
            switch (mode) {
                case STROBE:
                    value = OFF;
                    break;
                case DEATH_RAY:
                    if (sValueDeathRay >= 0) {
                        value = sValueDeathRay;
                    } else if (sValueHigh >= 0) {
                        value = sValueHigh;
                    } else {
                        value = 0;
                        Log.d(MSG_TAG, "Broken device configuration");
                    }
                    break;
                case ON:
                    if (sValueOn >= 0) {
                        value = sValueOn;
                    } else {
                        value = 0;
                        Log.d(MSG_TAG, "Broken device configuration");
                    }
                    break;
            }
            if (sUseCameraInterface) {
                if (mCamera == null) {
                    mCamera = Camera.open();
                }
                if (value == OFF) {
                    mParams = mCamera.getParameters();
                    mParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(mParams);
                    if (mode != STROBE) {
                        mCamera.stopPreview();
                        mCamera.release();
                        mCamera = null;
                    }
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                } else {
                    mParams = mCamera.getParameters();
                    mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(mParams);
                    if (!mWakeLock.isHeld()) { // only get the wakelock if we
                                               // don't have it already
                        mWakeLock.acquire(); // we don't want to go to sleep
                                             // while cam is up
                    }
                    if (mFlashMode != STROBE) {
                        mCamera.startPreview();
                    }
                }
            } else {
                if (mWriter == null) {
                    mWriter = new FileWriter(sFlashDevice);
                }
                mWriter.write(String.valueOf(value));
                mWriter.flush();
                if (mode == OFF) {
                    mWriter.close();
                    mWriter = null;
                }
            }
            mFlashMode = mode;
        } catch (IOException e) {
            throw new RuntimeException("Can't open flash device", e);
        }
    }

    public synchronized int getFlashMode() {
        return mFlashMode;
    }
}
