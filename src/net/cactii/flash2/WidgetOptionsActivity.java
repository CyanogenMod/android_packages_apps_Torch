package net.cactii.flash2;

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
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class WidgetOptionsActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private int mAppWidgetId;

    private SeekBarPreference mStrobeFrequency;

    private SharedPreferences mPreferences;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.optionsview);
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
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

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        Preference mSave = (Preference) findPreference("saveSettings");
        mSave.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Editor editor = mPreferences.edit();

                editor.putBoolean("widget_strobe_" + mAppWidgetId,
                        mPreferences.getBoolean("widget_strobe", false));
                //had to do +1 to fix division by zero crash, only temporary fix:
                editor.putInt("widget_strobe_freq_" + mAppWidgetId,
                        666 / (1 + mPreferences.getInt("widget_strobe_freq", 5)));
                editor.putBoolean("widget_bright_" + mAppWidgetId,
                        mPreferences.getBoolean("widget_bright", false));
                editor.commit();

                //Initialize widget view for first update
                Context context = getApplicationContext();
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
                if (mPreferences.getBoolean("widget_strobe_" + mAppWidgetId, false))
                    views.setTextViewText(R.id.ind, "Strobe");
                else if (mPreferences.getBoolean("widget_bright_" + mAppWidgetId, false))
                    views.setTextViewText(R.id.ind, "Bright");
                else
                    views.setTextViewText(R.id.ind, "Torch");
                final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                appWidgetManager.updateAppWidget(mAppWidgetId, views);

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
                return false;
            }

        });
    }

    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("widget_strobe")) {
            this.mStrobeFrequency.setEnabled(sharedPreferences.getBoolean("widget_strobe", false));
        }

    }
}
