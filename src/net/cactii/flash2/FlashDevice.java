package net.cactii.flash2;

import android.os.Build;

import java.io.FileWriter;
import java.io.IOException;
import android.hardware.Camera;

public class FlashDevice {
    
    private static final String DEVICE = "/sys/class/leds/flashlight/brightness";
    private static final String DEVICE_SHOLES = "/sys/class/leds/spotlight/brightness";

    public static final int STROBE    = -1;
	public static final int OFF       = 0;
	public static final int ON        = 1;
        // device speedy has 4 brightness levels: 125, 126, 127, 128
	public static final int SPEEDY_ON = 125; 
	public static final int DEATH_RAY = 3;
	public static final int HIGH      = 128;
	public static final int ZEPP_ON   = 100;
	public static final int ZEPP_DEATH_RAY = 255;

	private static FlashDevice instance;

	private static boolean useDeathRay = !Build.DEVICE.equals("supersonic") && !Build.DEVICE.equals("glacier") && !Build.DEVICE.equals("speedy");
	private static boolean useZeppDeathRay = Build.DEVICE.contains("zepp") || Build.DEVICE.equals("sholes");
	private static boolean useCameraInterface = Build.DEVICE.contains("crespo") || Build.DEVICE.contains("p990") || Build.DEVICE.contains("p999");

	private FileWriter mWriter = null;

	private int mFlashMode = OFF;

        private Camera mCamera = null;
        private Camera.Parameters mParams;

	private FlashDevice() {}

	public static synchronized FlashDevice instance() {
	    if (instance == null) {
	        instance = new FlashDevice();
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
	                value = useDeathRay ? DEATH_RAY : HIGH;
	                value = (useZeppDeathRay && useDeathRay) ? ZEPP_DEATH_RAY : value;
	                break;
	            case ON:
	                value = (Build.DEVICE.contains("zepp")) ? ZEPP_ON : value;
	                value = (Build.DEVICE.contains("speedy")) ? SPEEDY_ON : value;
	                break;
                }        
                if (useCameraInterface) {
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
	                if (Build.DEVICE.contains("sholes")) {
	                    mWriter = new FileWriter(DEVICE_SHOLES);
	                } else {
	                    mWriter = new FileWriter(DEVICE);
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
	        throw new RuntimeException("Can't open flash device: " + DEVICE, e);
	    }
	}

	public synchronized int getFlashMode() {
	    return mFlashMode;
	}
}
