package net.cactii.flash2;

import android.os.Build;
import android.util.Log;

public class FlashDevice {
	private static FlashDevice currentInstance;
	public boolean open;
	public boolean on;
	
	static synchronized FlashDevice getInstance() {
		if (currentInstance == null) {
			currentInstance = new FlashDevice();
			currentInstance.on = currentInstance.open = false;
		}
		return currentInstance;
	}

	public String getDevice() {
	    String dev = "/dev/msm_camera/config" + (Build.DEVICE.equals("supersonic") ? "1" : "0");
	    return dev;
	}

	public void Open() {

		if (!open && "Failed".equals(openFlash(getDevice())))
			open = false;
		else
			open = true;

		Log.d("Torch", "flash opened: " + open);

	}

	public boolean Writable() {
		String result = flashWritable(getDevice());
		Log.d("Torch", "Writable: " + result);
		return "OK".equals(result);
	}

	public void Close() {
		Log.d("Torch", "Closing: " + closeFlash());
		open = false;
	}
	
	public String FlashOn() {
		on = true;
		return setFlashOn();
	}
	
	public String FlashOff() {
		on = false;
		return setFlashOff();
	}

    // These functions are defined in the native libflash library.
    public static native String  openFlash(String device);
    public static native String  setFlashOff();
    public static native String  setFlashOn();
    public static native String  setFlashFlash();
    public static native String  closeFlash();
    public static native String  flashWritable(String device);
   	// Load libflash once on app startup.
   	static {
   		System.loadLibrary("jni_flash");
   	}
}
