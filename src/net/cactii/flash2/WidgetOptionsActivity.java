
package net.cactii.flash2;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

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
            Log.d("TorchOptions", "Widget id: " + mAppWidgetId);
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
                editor.putInt("widget_strobe_freq_" + mAppWidgetId,
                        500 / mPreferences.getInt("widget_strobe_freq", 5));
                editor.putBoolean("widget_bright_" + mAppWidgetId,
                        mPreferences.getBoolean("widget_bright", false));
                editor.commit();
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
