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

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.RemoteViews;

public class WidgetOptionsFragment extends PreferenceFragment implements
        OnSharedPreferenceChangeListener {

    private int mAppWidgetId;

    private SeekBarPreference mStrobeFrequency;

    private SharedPreferences mPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.optionsview);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        CheckBoxPreference mBrightPref = (CheckBoxPreference) findPreference("widget_bright");
        mBrightPref.setChecked(false);

        CheckBoxPreference mStrobePref = (CheckBoxPreference) findPreference("widget_strobe");
        mStrobePref.setChecked(false);

        mStrobeFrequency = (SeekBarPreference) findPreference("widget_strobe_freq");
        mStrobeFrequency.setEnabled(false);

        // keeps 'Strobe frequency' option available
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    public void addWidget() {
        Editor editor = mPreferences.edit();
        editor.putBoolean("widget_strobe_" + mAppWidgetId,
                mPreferences.getBoolean("widget_strobe", false));

        // TODO: Fix temporary patch
        // had to do +1 to fix division by zero crash, only temporary fix:
        editor.putInt("widget_strobe_freq_" + mAppWidgetId,
                666 / (1 + mPreferences.getInt("widget_strobe_freq", 5)));
        editor.putBoolean("widget_bright_" + mAppWidgetId,
                mPreferences.getBoolean("widget_bright", false));
        editor.commit();

        // Initialize widget view for first update
        Context context = getActivity().getApplicationContext();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        Intent launchIntent = new Intent();
        launchIntent.setClass(context, TorchWidgetProvider.class);
        launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        launchIntent.setData(Uri.parse("custom:" + mAppWidgetId + "/0"));
        PendingIntent pi = PendingIntent.getBroadcast(context, 0 /*
                                                                  * no
                                                                  * requestCode
                                                                  */, launchIntent, 0 /*
                                                                                       * no
                                                                                       * flags
                                                                                       */);
        views.setOnClickPendingIntent(R.id.btn, pi);
        if (mPreferences.getBoolean("widget_strobe_" + mAppWidgetId, false)) {
            views.setTextViewText(R.id.ind, context.getString(R.string.label_strobe));
        } else if (mPreferences.getBoolean("widget_bright_" + mAppWidgetId, false)) {
            views.setTextViewText(R.id.ind, context.getString(R.string.label_high));
        } else {
            views.setTextViewText(R.id.ind, context.getString(R.string.app_name));
        }

        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(mAppWidgetId, views);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        getActivity().setResult(Activity.RESULT_OK, resultValue);

        // close the activity
        getActivity().finish();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("widget_strobe")) {
            this.mStrobeFrequency.setEnabled(sharedPreferences.getBoolean("widget_strobe", false));
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.widget, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.saveSetting: // Changes are accepted
                addWidget();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
