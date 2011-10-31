package net.cactii.flash2;

import android.os.Build;

import java.io.FileWriter;
import java.io.IOException;
import android.hardware.Camera;
import net.cactii.flash2.R;
import android.content.Context;
import android.util.Log;

import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import java.io.File;


public class FlashDevice {

    private static final String MSG_TAG = "TorchDevice";

    /* New variables, init'ed by resource items */
    private static int mValueOn;
    private static int mValueHigh;
    private static int mValueDeathRay;
    private static String mFlashDevice;
    private static boolean mUseCameraInterface;
    private WakeLock mWakeLock;

    public static final int STROBE    = -1;
    public static final int OFF       = 0;
    public static final int ON        = 1;
    public static final int HIGH      = 128;
    public static final int DEATH_RAY = 3;

    private static FlashDevice instance;

    private FileWriter mWriter = null;

    private int mFlashMode = OFF;

    private Camera mCamera = null;
    private Camera.Parameters mParams;

    private FlashDevice(Context context) {
        this.mValueOn = context.getResources().getInteger(R.integer.valueOn);
        this.mValueHigh = context.getResources().getInteger(R.integer.valueHigh);
        this.mValueDeathRay = context.getResources().getInteger(R.integer.valueDeathRay);
        this.mFlashDevice = context.getResources().getString(R.string.flashDevice);
        this.mUseCameraInterface = context.getResources().getBoolean(R.bool.useCameraInterface);
        if (this.mUseCameraInterface) {
            PowerManager pm
                = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Torch");
        }
    }

    public static synchronized FlashDevice instance(Context context) {
        if (instance == null) {
            instance = new FlashDevice(context);
        }
        return instance;
    }

    public synchronized void setFlashMode(int mode) {
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
                    mWakeLock.acquire(); // we don't want to go to sleep while cam is up
                    if (mFlashMode != STROBE) {
                        mCamera.startPreview();
                    }
                }
            } else {
                if (mWriter == null) {
                    mWriter = new FileWriter(mFlashDevice);
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
