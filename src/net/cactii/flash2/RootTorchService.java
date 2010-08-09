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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class RootTorchService extends Service {

  public static final String MSG_TAG = "TorchRoot";
  private FlashDevice mDevice;
  public Thread mStrobeThread;
  public Handler mHandler;
  
  public TimerTask mTorchTask;
  public Timer mTorchTimer;
  
  public WrapperTask mStrobeTask;
  public Timer mStrobeTimer;
  
  private NotificationManager mNotificationManager;
  private Notification mNotification;
  
  public boolean mBright;
  public boolean mTorchOn;
  private int mStrobePeriod;
  private IntentReceiver mReceiver;
  private Runnable mStrobeRunnable;
  
  public void onCreate() {
    String ns = Context.NOTIFICATION_SERVICE;
    this.mNotificationManager = (NotificationManager) getSystemService(ns);
    
    this.mHandler = new Handler() {};
    
    this.mTorchTask = new TimerTask() {
      public void run() {
         FlashDevice.setFlashFlash();
      }
    };
    this.mTorchTimer = new Timer();
    
    this.mStrobeRunnable = new Runnable() {
      public int mCounter = 4;
      public boolean mOn;
      
      @Override
      public void run() {
        if (!this.mOn) {
          if (this.mCounter-- < 1) {
            FlashDevice.setFlashFlash();
            this.mOn = true;
          }
        } else {
          mDevice.FlashOff();
          this.mCounter = 4;
          this.mOn = false;
        }
      }
      
    };
    this.mStrobeTask = new WrapperTask(this.mStrobeRunnable);

    this.mStrobeTimer = new Timer();
    
  }
  
  public int onStartCommand(Intent intent, int flags, int startId) {
    
    this.mDevice = new FlashDevice();
    
    if (!this.mDevice.Writable()) {
      Log.d("Torch", "Cant open flash RW");
      Su su = new Su();
      if (!su.can_su) {
        Toast.makeText(this, "Torch - cannot get root", Toast.LENGTH_SHORT).show();
        this.stopSelf();
      }
      su.Run("chmod 666 " + FlashDevice.getInstance().getDevice());
    }

    this.mDevice.Open();
    Log.d(MSG_TAG, "Starting torch");
    if (intent == null)
      this.stopSelf();
    this.mBright = intent.getBooleanExtra("bright", false);
    if (intent.getBooleanExtra("strobe", false)) {
      this.mStrobePeriod = intent.getIntExtra("period", 200)/4;
      this.mStrobeTimer.schedule(this.mStrobeTask, 0, this.mStrobePeriod);
    } else if (this.mBright) {
      this.mTorchTimer.schedule(this.mTorchTask, 0, 200);
    } else {
      this.mDevice.FlashOn();
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
    this.mNotificationManager.cancelAll();
    this.unregisterReceiver(this.mReceiver);
    stopForeground(true);
    this.mTorchTimer.cancel();
    this.mStrobeTimer.cancel();
    this.mDevice.FlashOff();
    this.mDevice.Close();
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
