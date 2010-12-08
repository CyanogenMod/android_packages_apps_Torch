package net.cactii.flash2;

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
import android.provider.Settings;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class TorchService extends Service {

    private static final String MSG_TAG = "TorchRoot";

    private Handler mHandler;

    private TimerTask mTorchTask;

    private Timer mTorchTimer;

    private WrapperTask mStrobeTask;

    private Timer mStrobeTimer;

    private NotificationManager mNotificationManager;

    private Notification mNotification;

    private boolean mBright;

    private int mStrobePeriod;

    private IntentReceiver mReceiver;

    private Runnable mStrobeRunnable;

    public void onCreate() {
        String ns = Context.NOTIFICATION_SERVICE;
        this.mNotificationManager = (NotificationManager) getSystemService(ns);

        this.mHandler = new Handler() {
        };

        this.mTorchTask = new TimerTask() {
            public void run() {
                FlashDevice.instance().setFlashMode(mBright ? FlashDevice.DEATH_RAY : FlashDevice.ON);
            }
        };
        this.mTorchTimer = new Timer();

        this.mStrobeRunnable = new Runnable() {
            private int mCounter = 4;

            @Override
            public void run() {
                int flashMode = mBright ? FlashDevice.DEATH_RAY : FlashDevice.ON;
                if (FlashDevice.instance().getFlashMode() < flashMode) {
                    if (this.mCounter-- < 1) {
                        FlashDevice.instance().setFlashMode(flashMode);
                    }
                } else {
                    FlashDevice.instance().setFlashMode(FlashDevice.STROBE);
                    this.mCounter = 4;
                }
            }

        };
        this.mStrobeTask = new WrapperTask(this.mStrobeRunnable);

        this.mStrobeTimer = new Timer();

    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(MSG_TAG, "Starting torch");
        if (intent == null)
            this.stopSelf();
        this.mBright = intent.getBooleanExtra("bright", false);
        if (intent.getBooleanExtra("strobe", false)) {
            this.mStrobePeriod = intent.getIntExtra("period", 200) / 4;
            this.mStrobeTimer.schedule(this.mStrobeTask, 0, this.mStrobePeriod);
        } else {
            this.mTorchTimer.schedule(this.mTorchTask, 0, 100);
        }

        this.mReceiver = new IntentReceiver();
        registerReceiver(this.mReceiver, new IntentFilter("net.cactii.flash2.SET_STROBE"));

        this.mNotification = new Notification(R.drawable.notification_icon, getString(R.string.not_torch_title),
                System.currentTimeMillis());
        this.mNotification.setLatestEventInfo(this, getString(R.string.not_torch_title), getString(R.string.not_torch_summary),
                PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
        this.mNotification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        this.mNotificationManager.notify(0, this.mNotification);

        startForeground(0, this.mNotification);
        Settings.System.putInt(this.getContentResolver(), Settings.System.TORCH_STATE, 1);
        this.sendBroadcast(new Intent(TorchSwitch.TORCH_STATE_CHANGED));
        return START_STICKY;
    }

    public void onDestroy() {
        this.mNotificationManager.cancelAll();
        this.unregisterReceiver(this.mReceiver);
        stopForeground(true);
        this.mTorchTimer.cancel();
        this.mStrobeTimer.cancel();
        FlashDevice.instance().setFlashMode(FlashDevice.OFF);
        Settings.System.putInt(this.getContentResolver(), Settings.System.TORCH_STATE, 0);
        this.sendBroadcast(new Intent(TorchSwitch.TORCH_STATE_CHANGED));
    }

    public void Reshedule(int period) {
        this.mStrobeTask.cancel();
        this.mStrobeTask = new WrapperTask(this.mStrobeRunnable);

        this.mStrobePeriod = period / 4;
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
