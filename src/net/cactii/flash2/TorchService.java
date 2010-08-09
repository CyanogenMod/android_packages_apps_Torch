package net.cactii.flash2;

import net.cactii.flash2.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class TorchService extends Service {
  public static final String MSG_TAG = "TorchNotRoot";

  private Camera mCamera;
  private Camera.Parameters mParams;
  
  private NotificationManager mNotificationManager;
  private Notification mNotification;
  
  public TimerTask mStrobeTask;
  public Timer mStrobeTimer;
  public int mStrobePeriod;
  
  public Handler mHandler;
  private IntentReceiver mReceiver;

  private Runnable mStrobeRunnable;
  
  public void onCreate() {
    String ns = Context.NOTIFICATION_SERVICE;
    this.mNotificationManager = (NotificationManager) getSystemService(ns);
    
    this.mStrobeRunnable = new Runnable() {
      public int mCounter = 4;
      public boolean mOn;
      
      public void run() {
        if (!this.mOn) {
          if (this.mCounter-- < 1) {
            Camera.Parameters params = mCamera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(params);
            this.mOn = true;
          }
        } else {
          Camera.Parameters params = mCamera.getParameters();
          params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
          mCamera.setParameters(params);
          this.mCounter = 4;
          this.mOn = false;
        }
      }
    };
    this.mStrobeTask = new WrapperTask(this.mStrobeRunnable);
    
    this.mStrobeTimer = new Timer();
    
    this.mHandler = new Handler() {
      
    };
  }
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(MSG_TAG, "Starting torch");
    try {
      this.mCamera = Camera.open();
    } catch (RuntimeException e) {
      
    }
    if (!Build.VERSION.RELEASE.equals("2.2")) {
      this.stopSelf();
    }

    if (intent != null && intent.getBooleanExtra("strobe", false)) {
      this.mStrobePeriod = intent.getIntExtra("period", 200)/4;
      this.mStrobeTimer.schedule(this.mStrobeTask, 0,
          this.mStrobePeriod);
    } else {
      this.mParams = mCamera.getParameters();
      this.mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
      this.mCamera.setParameters(this.mParams);
    }
    
    this.mReceiver = new IntentReceiver();
    registerReceiver(this.mReceiver, new IntentFilter("net.cactii.flash2.SET_STROBE"));
    
    this.mNotification = new Notification(R.drawable.notification_icon,
        "Torch on", System.currentTimeMillis());
    this.mNotification.setLatestEventInfo(this, "Torch on",
        "Torch currently on",
        PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
    
    this.mNotificationManager.notify(0, this.mNotification);
    
    startForeground(0, this.mNotification);
    return START_STICKY;
  }

  
  public void onDestroy() {
    this.mStrobeTimer.cancel();
    this.mCamera.release();
    this.mNotificationManager.cancelAll();
    this.unregisterReceiver(this.mReceiver);
    stopForeground(true);
  }
  
  public void Reshedule(int period) {
    this.mStrobeTask.cancel();
    this.mStrobeTask = new WrapperTask(this.mStrobeRunnable);
    
    this.mStrobePeriod = period/4;
    this.mStrobeTimer.schedule(this.mStrobeTask, 0, this.mStrobePeriod);
  }
  
  public class WrapperTask extends TimerTask {
    private final Runnable target;
    public WrapperTask(Runnable target) {
      this.target = target;
    }
    public void run() {
      target.run();
    }
  }
  
  @Override
  public IBinder onBind(Intent intent) {
    // TODO Auto-generated method stub
    return null;
  }

  public class IntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, final Intent intent) {
      mHandler.post(new Runnable() {

        @Override
        public void run() {
          Reshedule(intent.getIntExtra("period", 200));
        }
        
      });
    }
  }
}
