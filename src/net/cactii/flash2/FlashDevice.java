package net.cactii.flash2;

import android.os.Build;

import java.io.FileWriter;
import java.io.IOException;

public class FlashDevice {
    
    private static final String DEVICE = "/sys/devices/platform/flashlight.0/leds/flashlight/brightness";
	
    public static final int STROBE    = -1;
	public static final int OFF       = 0;
	public static final int ON        = 1;
	public static final int DEATH_RAY = 3;
	public static final int HIGH      = 128;
	
	private static FlashDevice instance;
	
	private static boolean useDeathRay = !Build.DEVICE.equals("supersonic");
	
	private FileWriter mWriter = null;
	
	private int mFlashMode = OFF;
	
	private FlashDevice() {}
	
	public static synchronized FlashDevice instance() {
	    if (instance == null) {
	        instance = new FlashDevice();
	    }
	    return instance;
	}
	
	public synchronized void setFlashMode(int mode) {
	    try {
	        if (mWriter == null) {
	            mWriter = new FileWriter(DEVICE);
	        }
	        
	        int value = mode;
	        switch (mode) {
	            case STROBE:
	                value = OFF;
	                break;
	            case DEATH_RAY:
	                value = useDeathRay ? DEATH_RAY : HIGH;
	                break;
	        }
	        mWriter.write(String.valueOf(value));
	        mWriter.flush();
	        mFlashMode = mode;
	        
	        if (mode == OFF) {
	            mWriter.close();
	            mWriter = null;
	        }
	    } catch (IOException e) {
	        throw new RuntimeException("Can't open flash device: " + DEVICE, e);
	    }
	}

	public synchronized int getFlashMode() {
	    return mFlashMode;
	}
}
