/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package net.cactii.flash2;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class TorchWidgetProvider extends AppWidgetProvider {

    private static TorchWidgetProvider sInstance;

    static synchronized TorchWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new TorchWidgetProvider();
        }
        return sInstance;
    }

    private enum WidgetState {
        OFF (R.drawable.ic_appwidget_torch_off),
        ON  (R.drawable.ic_appwidget_torch_on);

        /**
         * The drawable resources associated with this widget state.
         */
        private final int mDrawImgRes;

        private WidgetState(int drawImgRes) {
            mDrawImgRes = drawImgRes;
        }

        public int getImgDrawable() {
            return mDrawImgRes;
        }
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds)
            this.updateState(context, appWidgetId);
    }

    private static PendingIntent getLaunchPendingIntent(Context context, int appWidgetId) {
        Intent launchIntent = new Intent();
        launchIntent.setClass(context, TorchWidgetProvider.class);
        launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        launchIntent.setData(Uri.parse("custom:" + appWidgetId + "/" + 0));
        return PendingIntent.getBroadcast(context,
                0 /* no requestCode */, launchIntent, 0 /* no flags */);
    }

    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            String[] parts = intent.getData().getSchemeSpecificPart().split("/");
            int widgetId = Integer.parseInt(parts[0]);
            int buttonId = Integer.parseInt(parts[1]);

            if (buttonId == 0) {
                Intent pendingIntent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
                pendingIntent.putExtra("bright",
                        mPrefs.getBoolean("widget_bright_" + widgetId, false));
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

    void updateState(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Intent stateIntent = context.registerReceiver(null,
                new IntentFilter(TorchSwitch.TORCH_STATE_CHANGED));
        boolean on = stateIntent != null && stateIntent.getIntExtra("state", 0) != 0;

        views.setOnClickPendingIntent(R.id.btn, getLaunchPendingIntent(context, appWidgetId));

        if (on) {
            views.setImageViewResource(R.id.img_torch, WidgetState.ON.getImgDrawable());
        } else {
            views.setImageViewResource(R.id.img_torch, WidgetState.OFF.getImgDrawable());
        }

        if (prefs.getBoolean("widget_bright_" + appWidgetId, false)) {
            views.setTextViewText(R.id.ind_text, context.getString(R.string.label_high));
        }

        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(appWidgetId, views);
    }
}
