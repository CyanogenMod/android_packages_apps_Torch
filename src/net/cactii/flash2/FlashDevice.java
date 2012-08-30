package net.cactii.flash2;

import java.io.FileWriter;
import java.io.IOException;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import net.cactii.flash2.R;
import android.content.Context;
import android.util.Log;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

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
    private static boolean surfaceCreated = false;

    private FileWriter mWriter = null;

    private int mFlashMode = OFF;

    private Camera mCamera = null;
    private Camera.Parameters mParams;

    private FlashDevice(Context context) {
        mValueOn = context.getResources().getInteger(R.integer.valueOn);
        mValueHigh = context.getResources().getInteger(R.integer.valueHigh);
        mValueDeathRay = context.getResources().getInteger(R.integer.valueDeathRay);
        mFlashDevice = context.getResources().getString(R.string.flashDevice);
        mUseCameraInterface = context.getResources().getBoolean(R.bool.useCameraInterface);
        if (mUseCameraInterface) {
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
                        surfaceCreated = false;
                    }
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                } else {
                    if (!surfaceCreated) {
                        Log.d(MSG_TAG,
                        "KalimAz Prepare surface ...........................................................................");

                        int[] textures = new int[1];
                        // generate one texture pointer and bind it as an
                        // external texture.
                        GLES20.glGenTextures(1, textures, 0);
                        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                                textures[0]);
                        // No mip-mapping with camera source.
                        GLES20.glTexParameterf(
                                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
                        GLES20.glTexParameterf(
                                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
                        // Clamp to edge is only option.
                        GLES20.glTexParameteri(
                                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
                        GLES20.glTexParameteri(
                                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

                        mCamera.setPreviewTexture(new SurfaceTexture(
                                textures[0]));
                        surfaceCreated = true;
                        mCamera.startPreview();
                    }
                    mParams = mCamera.getParameters();
                    mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(mParams);
                    if (!mWakeLock.isHeld()) {  // only get the wakelock if we don't have it already
                        mWakeLock.acquire(); // we don't want to go to sleep while cam is up
                    }
                    /*if (mFlashMode != STROBE) {
                        Log.d(MSG_TAG, "KalimAz Preview no strobe .............................................................................");
                        mCamera.startPreview();
                    }*/
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
