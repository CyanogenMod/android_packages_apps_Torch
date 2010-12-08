package net.cactii.flash2;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.RemoteViews;

public class TorchWidgetProvider extends AppWidgetProvider {

    private static TorchWidgetProvider sInstance;

    static synchronized TorchWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new TorchWidgetProvider();
        }
        return sInstance;
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds)
            this.updateState(context, appWidgetId);
    }

    private static PendingIntent getLaunchPendingIntent(Context context, int appWidgetId,
            int buttonId) {
        Intent launchIntent = new Intent();
        launchIntent.setClass(context, TorchWidgetProvider.class);
        launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        launchIntent.setData(Uri.parse("custom:" + appWidgetId + "/" + buttonId));
        PendingIntent pi = PendingIntent.getBroadcast(context, 0 /*
                                                                  * no
                                                                  * requestCode
                                                                  */, launchIntent, 0 /*
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

            if (buttonId == 0) {
                Intent pendingIntent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
                pendingIntent.putExtra("bright",
                        mPrefs.getBoolean("widget_bright_" + widgetId, false));
                pendingIntent.putExtra("strobe",
                        mPrefs.getBoolean("widget_strobe_" + widgetId, false)); 
                pendingIntent.putExtra("period",
                        mPrefs.getInt("widget_strobe_freq_" + widgetId, 200));
                context.sendBroadcast(pendingIntent);
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.updateAllStates(context);
        } else if (intent.getAction().equals(TorchSwitch.TORCH_STATE_CHANGED)) {
            this.updateAllStates(context);
        }
    }

    public void updateAllStates(Context context) {
        final AppWidgetManager am = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = am.getAppWidgetIds(
                new ComponentName(context, this.getClass()));
        for (int appWidgetId : appWidgetIds)
            this.updateState(context, appWidgetId);
    }

    public void updateState(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        views.setOnClickPendingIntent(R.id.btn, getLaunchPendingIntent(context, appWidgetId, 0));

        if ((Settings.System.getInt(context.getContentResolver(),
                Settings.System.TORCH_STATE, 0) == 1)) {
            views.setImageViewResource(R.id.img_torch, R.drawable.icon);
        } else {
            views.setImageViewResource(R.id.img_torch, R.drawable.widget_off);
        }

        if (prefs.getBoolean("widget_strobe_" + appWidgetId, false)) {
            views.setTextViewText(R.id.ind, "Strobe");
        } else if (prefs.getBoolean("widget_bright_" + appWidgetId, false)) {
            views.setTextViewText(R.id.ind, "Bright");
        } else {
            views.setTextViewText(R.id.ind, "Torch");
        }

        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(appWidgetId, views);
    }
}
