/*
 * Copyright (C) 2010 Ben Buxton
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * This file is part of n1torch.
 *
 * n1torch is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * n1torch is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with n1torch.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.cactii.flash2;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

// TODO: Get this working properly.

public class SmlTorchWidgetProvider extends TorchWidgetProvider {
    private static SmlTorchWidgetProvider sInstance;

    static final ComponentName THIS_APPWIDGET =
            new ComponentName("net.cactii.flash",
                    "net.cactii.flash2.SmlTorchWidgetProvider");

    static synchronized SmlTorchWidgetProvider getInstance() {
        if (sInstance == null)
            sInstance = new SmlTorchWidgetProvider();
        return sInstance;
    }

    @Override
    public void updateState(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget);
        if (FlashDevice.instance(context).getFlashMode() > 0) {
            views.setImageViewResource(R.id.img_torch, R.drawable.icon);
        } else {
            views.setImageViewResource(R.id.img_torch, R.drawable.widget_off);
        }
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(THIS_APPWIDGET, views);
    }
}
