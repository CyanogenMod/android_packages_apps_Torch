/*
 * Copyright (C) 2013 The CyanogenMod Project
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

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import net.cactii.flash2.R;

import java.io.FileWriter;
import java.io.IOException;

public class FlashDevice {

    private static final String MSG_TAG = "TorchDevice";

    /* New variables, init'ed by resource items */
    private static int mValueOff;
    private static int mValueOn;
    private static int mValueLow;
    private static int mValueHigh;
    private static int mValueDeathRay;
    private static String mFlashDevice;
    private static String mFlashDeviceLuminosity;
    private static String mFlashDeviceLuminosity2;
    private static boolean mUseCameraInterface;
    private WakeLock mWakeLock;

    public static final int STROBE    = -1;
    public static final int OFF       = 0;
    public static final int ON        = 1;
    public static final int HIGH      = 128;
    public static final int DEATH_RAY = 3;

    private static FlashDevice sInstance;

    private FileWriter mFlashDeviceWriter = null;
    private FileWriter mFlashDeviceLuminosityWriter = null;
    private FileWriter mFlashDeviceLuminosityWriter2 = null;

    private int mFlashMode = OFF;

    private Camera mCamera = null;
    private SurfaceTexture mSurfaceTexture = null;

    private FlashDevice(Context context) {
        mValueOff = context.getResources().getInteger(R.integer.valueOff);
        mValueOn = context.getResources().getInteger(R.integer.valueOn);
        mValueLow = context.getResources().getInteger(R.integer.valueLow);
        mValueHigh = context.getResources().getInteger(R.integer.valueHigh);
        mValueDeathRay = context.getResources().getInteger(R.integer.valueDeathRay);
        mFlashDevice = context.getResources().getString(R.string.flashDevice);
        mFlashDeviceLuminosity = context.getResources().getString(R.string.flashDeviceLuminosity);
        mFlashDeviceLuminosity2 = context.getResources().getString(R.string.flashDeviceLuminosity2);
        mUseCameraInterface = context.getResources().getBoolean(R.bool.useCameraInterface);

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Torch");
    }

    public static synchronized FlashDevice instance(Context context) {
        if (sInstance == null) {
            sInstance = new FlashDevice(context.getApplicationContext());
        }
        return sInstance;
    }

    public synchronized void setFlashMode(int mode) {
	Log.d(MSG_TAG, "setFlashMode " + mode);

        if (mFlashMode == mode) return;

        try {
            int value = mode;
            switch (mode) {
                case STROBE:
                    value = OFF;
                    break;
                case DEATH_RAY:
                    if (mValueDeathRay >= 0) {
                        value = mValueDeathRay;
                    } else if (mValueHigh >= 0) {
                        value = mValueHigh;
                    } else {
                        value = 0;
                        Log.d(MSG_TAG,"Broken device configuration");
                    }
                    break;
                case ON:
                    if (mValueOn >= 0) {
                        value = mValueOn;
                    } else {
                        value = 0;
                        Log.d(MSG_TAG,"Broken device configuration");
                    }
                    break;
            }
            if (mUseCameraInterface) {
                if (mCamera == null) {
                    mCamera = Camera.open();
                }
                if (value == OFF) {
                    Camera.Parameters params = mCamera.getParameters();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(params);
                    if (mode != STROBE) {
                        mCamera.stopPreview();
                        mCamera.release();
                        mCamera = null;
                        if (mSurfaceTexture != null) {
                            mSurfaceTexture.release();
                            mSurfaceTexture = null;
                        }
                    }
                    if (mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                } else {
                    if (mSurfaceTexture == null) {
                        // Create a dummy texture, otherwise setPreview won't work on some devices
                        mSurfaceTexture = new SurfaceTexture(0);
                        mCamera.setPreviewTexture(mSurfaceTexture);
                        mCamera.startPreview();
                    }
                    Camera.Parameters params = mCamera.getParameters();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(params);
                    if (!mWakeLock.isHeld()) {
                        mWakeLock.acquire();
                    }
                }
            } else {
                // Devices with sysfs toggle and sysfs luminosity
                if (mFlashDeviceLuminosity != null && mFlashDeviceLuminosity.length() > 0) {
                    if (mFlashDeviceWriter == null) {
                        mFlashDeviceWriter = new FileWriter(mFlashDevice);
                    }
                    if (mFlashDeviceLuminosityWriter == null) {
                        mFlashDeviceLuminosityWriter = new FileWriter(mFlashDeviceLuminosity);
                    }
                    if (mFlashDeviceLuminosityWriter2 == null && mFlashDeviceLuminosity2.length() > 0) {
                        mFlashDeviceLuminosityWriter2 = new FileWriter(mFlashDeviceLuminosity2);
                    }

                    mFlashDeviceWriter.write(String.valueOf(mValueOn));
                    mFlashDeviceWriter.flush();

                    switch (mode) {
                        case ON:
                            mFlashDeviceLuminosityWriter.write(String.valueOf(mValueLow));
                            mFlashDeviceLuminosityWriter.flush();
                            if (mFlashDeviceLuminosityWriter2 != null) {
                                mFlashDeviceLuminosityWriter2.write(String.valueOf(mValueLow));
                                mFlashDeviceLuminosityWriter2.flush();
                            }
                            if (!mWakeLock.isHeld()) {
                                mWakeLock.acquire();
                            }
                            break;
                        case OFF:
                            mFlashDeviceLuminosityWriter.write(String.valueOf(mValueLow));
                            mFlashDeviceLuminosityWriter.close();
                            mFlashDeviceLuminosityWriter = null;
                            if (mFlashDeviceLuminosityWriter2 != null) {
                                mFlashDeviceLuminosityWriter2.write(String.valueOf(mValueLow));
                                mFlashDeviceLuminosityWriter2.close();
                                mFlashDeviceLuminosityWriter2 = null;
                            }
                            mFlashDeviceWriter.write(String.valueOf(mValueOff));
                            mFlashDeviceWriter.close();
                            mFlashDeviceWriter = null;
                            if (mWakeLock.isHeld()) {
                                mWakeLock.release();
                            }
                            break;
                        case STROBE:
                            mFlashDeviceWriter.write(String.valueOf(OFF));
                            mFlashDeviceWriter.flush();
                            if (!mWakeLock.isHeld()) {
                                mWakeLock.acquire();
                            }
                            break;
                        case DEATH_RAY:
                            if (mValueDeathRay >= 0) {
                                mFlashDeviceLuminosityWriter.write(String.valueOf(mValueDeathRay));
                                mFlashDeviceLuminosityWriter.flush();
                                if (mFlashDeviceLuminosityWriter2 != null) {
                                    mFlashDeviceLuminosityWriter2.write(String.valueOf(mValueDeathRay));
                                    mFlashDeviceLuminosityWriter2.flush();
                                }
                                if (!mWakeLock.isHeld()) {
                                    mWakeLock.acquire();
                                }
                            } else if (mValueHigh >= 0) {
                                mFlashDeviceLuminosityWriter.write(String.valueOf(mValueHigh));
                                mFlashDeviceLuminosityWriter.flush();
                                if (mFlashDeviceLuminosityWriter2 != null) {
                                    mFlashDeviceLuminosityWriter2.write(String.valueOf(mValueHigh));
                                    mFlashDeviceLuminosityWriter2.flush();
                                }
                                if (!mWakeLock.isHeld()) {
                                    mWakeLock.acquire();
                                }
                            } else {
                                mFlashDeviceLuminosityWriter.write(String.valueOf(OFF));
                                mFlashDeviceLuminosityWriter.flush();
                                if (mFlashDeviceLuminosityWriter2 != null) {
                                    mFlashDeviceLuminosityWriter2.write(String.valueOf(OFF));
                                    mFlashDeviceLuminosityWriter2.flush();
                                }
                                if (mWakeLock.isHeld()) {
                                    mWakeLock.release();
                                }
                                Log.d(MSG_TAG,"Broken device configuration");
                            }
                            break;
                    }
                } else {
                    // Devices with just a sysfs toggle
                    if (mFlashDeviceWriter == null) {
                        mFlashDeviceWriter = new FileWriter(mFlashDevice);
                    }
                    // Write to sysfs only if not already on
                    if (mode != mFlashMode) {
                        mFlashDeviceWriter.write(String.valueOf(value));
                        mFlashDeviceWriter.flush();
                        if (mode != OFF && !mWakeLock.isHeld()) {
                            mWakeLock.acquire();
                        }
                    }
                    if (mode == OFF) {
                        mFlashDeviceWriter.close();
                        mFlashDeviceWriter = null;
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                    }
                }
            }
            mFlashMode = mode;
        } catch (IOException e) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            throw new RuntimeException("Can't open flash device", e);
        }
    }

    public synchronized int getFlashMode() {
        return mFlashMode;
    }
}
