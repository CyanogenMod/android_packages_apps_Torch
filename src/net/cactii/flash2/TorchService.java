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
    private Notification.Builder mNotificationBuilder;

    private boolean mBright;
    private int mStrobePeriod;

    private IntentReceiver mReceiver;
    private Runnable mStrobeRunnable;

    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mHandler = new Handler();

        mTorchTask = new TimerTask() {
            @Override
            public void run() {
                FlashDevice.instance(TorchService.this).setFlashMode(
                        mBright ? FlashDevice.DEATH_RAY : FlashDevice.ON);
            }
        };
        mTorchTimer = new Timer();

        mStrobeRunnable = new Runnable() {
            private int mCounter = 4;

            @Override
            public void run() {
                final FlashDevice flash = FlashDevice.instance(TorchService.this);
                int flashMode = mBright ? FlashDevice.DEATH_RAY : FlashDevice.ON;
                if (flash.getFlashMode() < flashMode) {
                    if (this.mCounter-- < 1) {
                        flash.setFlashMode(flashMode);
                    }
                } else {
                    flash.setFlashMode(FlashDevice.STROBE);
                    this.mCounter = 4;
                }
            }
        };
        mStrobeTask = new WrapperTask(this.mStrobeRunnable);
        mStrobeTimer = new Timer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(MSG_TAG, "Starting torch");

        if (intent == null) {
            this.stopSelf();
            return START_NOT_STICKY;
        }

        mBright = intent.getBooleanExtra("bright", false);
        if (intent.getBooleanExtra("strobe", false)) {
            mStrobePeriod = intent.getIntExtra("period", 200) / 4;
            mStrobeTimer.schedule(mStrobeTask, 0, mStrobePeriod);
        } else {
            mTorchTimer.schedule(mTorchTask, 0, 100);
        }

        mReceiver = new IntentReceiver();
        registerReceiver(mReceiver, new IntentFilter("net.cactii.flash2.SET_STROBE"));

        mNotificationBuilder = new Notification.Builder(this);
        mNotificationBuilder.setSmallIcon(R.drawable.notification_icon);
        mNotificationBuilder.setTicker(getString(R.string.not_torch_title));
        mNotificationBuilder.setContentTitle(getString(R.string.not_torch_title));
        mNotificationBuilder.setContentIntent(PendingIntent.getActivity(this,
                0, new Intent(this, MainActivity.class), 0));
        mNotificationBuilder.setAutoCancel(false);
        mNotificationBuilder.setOngoing(true);

        PendingIntent turnOff = PendingIntent.getBroadcast(this, 0,
                new Intent(TorchSwitch.TOGGLE_FLASHLIGHT), 0);
        mNotificationBuilder.addAction(R.drawable.ic_appwidget_torch_off,
                getString(R.string.not_torch_toggle), turnOff);

        mNotification = mNotificationBuilder.getNotification();
        mNotificationManager.notify(getString(R.string.app_name).hashCode(), mNotification);

        startForeground(getString(R.string.app_name).hashCode(), mNotification);
        updateState(true);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        this.mNotificationManager.cancelAll();
        this.unregisterReceiver(this.mReceiver);
        stopForeground(true);
        this.mTorchTimer.cancel();
        this.mStrobeTimer.cancel();
        FlashDevice.instance(this).setFlashMode(FlashDevice.OFF);
        updateState(false);
    }

    private void updateState(boolean on) {
        Intent intent = new Intent(TorchSwitch.TORCH_STATE_CHANGED);
        intent.putExtra("state", on ? 1 : 0);
        sendStickyBroadcast(intent);
    }

    public void reschedule(int period) {
        mStrobeTask.cancel();
        mStrobeTask = new WrapperTask(mStrobeRunnable);

        mStrobePeriod = period / 4;
        mStrobeTimer.schedule(mStrobeTask, 0, mStrobePeriod);
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
                    reschedule(intent.getIntExtra("period", 200));
                }
            });
        }
    }
}
