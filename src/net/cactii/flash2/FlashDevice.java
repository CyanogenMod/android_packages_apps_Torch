package net.cactii.flash2;

import android.os.Build;

import java.io.FileWriter;
import java.io.IOException;
import android.hardware.Camera;
import net.cactii.flash2.R;
import android.content.Context;

import java.io.File;

public class FlashDevice {

    /* New variables, init'ed by resource items */
    private static int mValueOn;
    private static int mValueHigh;
    private static int mValueDeathRay;
    private static String mFlashDevice;
    private static boolean mUseCameraInterface;

    public static final int STROBE    = -1;
    public static final int OFF       = 0;
    public static final int ON        = 1;
    public static final int HIGH      = 128;
    public static final int DEATH_RAY = 3;


    /* ---------- TEMPORARY BLOCK FOR DELETION -----------------------------
     * Old variables. Delete these once all devices have
     * been converted */
    private static String DEVICE_FLASH = "/sys/class/leds/flashlight/brightness";
    private static final String DEVICE_SPOTL = "/sys/class/leds/spotlight/brightness";

/*
   some motorola devices don't have the flashlight file under flashlight
   instead it's located under torch-flash, so first check for the default
   and if it's not there, redefine it to torch-flash
*/
    static {
        File ff = new File(DEVICE_FLASH);
        if (! ff.exists()) {
            DEVICE_FLASH = "/sys/class/leds/torch-flash/brightness";
        }
    }

    // device speedy has 4 brightness levels: 125, 126, 127, 128
    public static final int SPEEDY_ON = 125;
    public static final int MOTO_ON   = 100;
    public static final int MOTO_DEATH_RAY = 255;

    private static boolean useDeathRay = !Build.DEVICE.equals("supersonic") && !Build.DEVICE.equals("glacier") && !Build.DEVICE.equals("speedy");
    private static boolean useMotoDeathRay = Build.DEVICE.contains("droid2") || Build.DEVICE.contains("jordan") || Build.DEVICE.contains("motus") || Build.DEVICE.contains("shadow") || Build.DEVICE.contains("sholes") || Build.DEVICE.contains("zepp");
    private static boolean useCameraInterface = Build.DEVICE.contains("crespo") || Build.DEVICE.contains("olympus") || Build.DEVICE.contains("p990") || Build.DEVICE.contains("p999");

    private static boolean useMotoWriter = Build.DEVICE.contains("droid2") || Build.DEVICE.contains("jordan") || Build.DEVICE.contains("shadow") || Build.DEVICE.contains("sholes");
    /* ---------- END OF TEMPORARY BLOCK FOR DELETION ----------------------- */

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
                        value = useMotoDeathRay ? MOTO_DEATH_RAY : DEATH_RAY;
                        value = useDeathRay ? value : HIGH;
                    }
                    break;
                case ON:
                    if (mValueOn >= 0) {
                        value = mValueOn;
                    } else {
                        value = useMotoDeathRay ? MOTO_ON : ON;
                        value = (Build.DEVICE.contains("speedy")) ? SPEEDY_ON : value;
                    }
                    break;
            }
            if (mUseCameraInterface || useCameraInterface) {
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
                } else {
                    mParams = mCamera.getParameters();
                    mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(mParams);
                    if (mFlashMode != STROBE) {
                        mCamera.startPreview();
                    }
                }
            } else {
                if (mWriter == null) {
                    if (useMotoWriter) {
                        mWriter = new FileWriter(DEVICE_SPOTL);
                    } else {
                        mWriter = new FileWriter(mFlashDevice.equals("") ? DEVICE_FLASH : mFlashDevice);
                    }
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
