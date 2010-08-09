package net.cactii.flash2;

import net.cactii.flash2.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.List;

public class TorchWidgetProvider extends AppWidgetProvider {

  public FlashDevice device;
  private static TorchWidgetProvider sInstance;
  public boolean mTorchOn;
  public Thread brightThread;
  public SharedPreferences mPrefs;

  static final ComponentName THIS_APPWIDGET = new ComponentName("net.cactii.flash",
      "net.cactii.flash2.TorchWidgetProvider");

  static synchronized TorchWidgetProvider getInstance() {
    if (sInstance == null) {
      sInstance = new TorchWidgetProvider();
    }
    return sInstance;
  }

  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    final int N = appWidgetIds.length;
    for (int i = 0; i < N; i++) {
      int appWidgetId = appWidgetIds[i];
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

      views.setOnClickPendingIntent(R.id.btn, getLaunchPendingIntent(context, appWidgetId, 0));

      this.updateState(context, appWidgetId);
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }

  private static PendingIntent getLaunchPendingIntent(Context context, int appWidgetId, int buttonId) {
    Intent launchIntent = new Intent();
    Log.d("TorchWidget", "WIdget id: " + appWidgetId);
    launchIntent.setClass(context, TorchWidgetProvider.class);
    launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
    launchIntent.setData(Uri.parse("custom:" + appWidgetId + "/" + buttonId));
    PendingIntent pi = PendingIntent
        .getBroadcast(context, 0 /* no requestCode */, launchIntent, 0 /*
                                                                        * no
                                                                        * flags
                                                                        */);
    return pi;
  }

  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);
    SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
      Uri data = intent.getData();
      int buttonId;
      int widgetId;
      widgetId = Integer.parseInt(data.getSchemeSpecificPart().split("/")[0]);
      buttonId = Integer.parseInt(data.getSchemeSpecificPart().split("/")[1]);

      device = FlashDevice.getInstance();
      Log.d("TorchWidget", "Button Id is: " + widgetId);
      if (buttonId == 0) {
        Intent pendingIntent;
        
        if (this.TorchServiceRunning(context)) {
          context.stopService(new Intent(context, TorchService.class));
          context.stopService(new Intent(context, RootTorchService.class));
          this.updateAllStates(context);
          return;
        }

        if (!device.Writable())
          this.AttemptToChmodDevice(context);
        
        if (Build.VERSION.RELEASE.equals("2.2")) {
          if (mPrefs.getBoolean("widget_bright_" + widgetId, false)) {
            if (device.Writable()) {
              pendingIntent = new Intent(context, RootTorchService.class);
            } else {
              Toast.makeText(context, "No root, cant go high brightness.", Toast.LENGTH_SHORT).show();
              pendingIntent = new Intent(context, TorchService.class);
            }
          } else {
            pendingIntent = new Intent(context, TorchService.class);
          }
        } else {  // Pre-Froyo
          if (!device.Writable()) {
            Toast.makeText(context, "Cant open LED. No root?", Toast.LENGTH_SHORT).show();
            return;
          }
          pendingIntent = new Intent(context, RootTorchService.class);
        }
        pendingIntent.putExtra("bright", mPrefs.getBoolean("widget_bright_" + widgetId, false));
        if (mPrefs.getBoolean("widget_strobe_" + widgetId, false)) {
          pendingIntent.putExtra("strobe", true);
          pendingIntent.putExtra("period", mPrefs.getInt("widget_strobe_freq_" + widgetId, 200));
        }
        if (this.TorchServiceRunning(context)) {
          context.stopService(pendingIntent);
        } else {
          context.startService(pendingIntent);
        }
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      this.updateAllStates(context);
    }
  }
  
  private void AttemptToChmodDevice(Context context) {
    Log.d("Torch", "Cant open flash RW");
    Su su = new Su();
    if (su.can_su) {
      su.Run("chmod 666 /dev/msm_camera/config0");
    } else {
      Toast.makeText(context, "Torch - cannot get root", Toast.LENGTH_SHORT).show();
    }
  }

  private boolean TorchServiceRunning(Context context) {
    ActivityManager am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

    List<ActivityManager.RunningServiceInfo> svcList = am.getRunningServices(100);

    if (!(svcList.size() > 0))
      return false;
    for (int i = 0; i < svcList.size(); i++) {
      RunningServiceInfo serviceInfo = svcList.get(i);
      ComponentName serviceName = serviceInfo.service;
      if (serviceName.getClassName().endsWith(".TorchService")
          || serviceName.getClassName().endsWith(".RootTorchService"))
        return true;
    }
    return false;
  }
  
  public void updateAllStates(Context context) {
    final AppWidgetManager am = AppWidgetManager.getInstance(context);
    for (int id : am.getAppWidgetIds(THIS_APPWIDGET))
      this.updateState(context, id);
  }

  public void updateState(Context context, int appWidgetId) {
    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

    if (this.TorchServiceRunning(context)) {
      views.setImageViewResource(R.id.img_torch, R.drawable.icon);
    } else {
      views.setImageViewResource(R.id.img_torch, R.drawable.widget_off);
    }

    if (prefs.getBoolean("widget_strobe_" + appWidgetId, false))
      views.setTextViewText(R.id.ind, "Strobe");
    else if (prefs.getBoolean("widget_bright_" + appWidgetId, false))
      views.setTextViewText(R.id.ind, "Bright");
    else
      views.setTextViewText(R.id.ind, "Torch");

    final AppWidgetManager gm = AppWidgetManager.getInstance(context);
    gm.updateAppWidget(appWidgetId, views);
  }
}
