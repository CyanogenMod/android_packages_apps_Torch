package net.cactii.flash2;

import net.cactii.flash2.R;

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
		if (FlashDevice.instance().getFlashMode() > 0) {
			views.setImageViewResource(R.id.img_torch, R.drawable.icon);
		} else {
			views.setImageViewResource(R.id.img_torch, R.drawable.widget_off);
		}
		final AppWidgetManager gm = AppWidgetManager.getInstance(context);
		gm.updateAppWidget(THIS_APPWIDGET, views);
	}
}
