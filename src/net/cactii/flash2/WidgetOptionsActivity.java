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
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.RemoteViews;

public class WidgetOptionsActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private int mAppWidgetId;
    private SeekBarPreference mStrobeFrequency;
    private SharedPreferences mPreferences;

    @SuppressWarnings("deprecation")
    //No need to go to fragments right now
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.optionsview);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        CheckBoxPreference brightPref = (CheckBoxPreference) findPreference("widget_bright");
        brightPref.setChecked(false);

        CheckBoxPreference strobePref = (CheckBoxPreference) findPreference("widget_strobe");
        strobePref.setChecked(false);

        mStrobeFrequency = (SeekBarPreference) findPreference("widget_strobe_freq");
        mStrobeFrequency.setEnabled(false);

        //keeps 'Strobe frequency' option available
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    void addWidget() {
        Editor editor = mPreferences.edit();

        editor.putBoolean("widget_strobe_" + mAppWidgetId,
                mPreferences.getBoolean("widget_strobe", false));
        //TODO: Fix temporary patch
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

        PendingIntent pi = PendingIntent.getBroadcast(context,
                0 /* no requestCode */, launchIntent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.btn, pi);

        if (mPreferences.getBoolean("widget_strobe_" + mAppWidgetId, false)) {
            views.setTextViewText(R.id.ind_text, context.getString(R.string.label_strobe));
        } else if (mPreferences.getBoolean("widget_bright_" + mAppWidgetId, false)) {
            views.setTextViewText(R.id.ind_text, context.getString(R.string.label_high));
        }

        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(mAppWidgetId, views);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);

        //close the activity
        finish();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("widget_strobe")) {
            mStrobeFrequency.setEnabled(sharedPreferences.getBoolean("widget_strobe", false));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.widget, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.saveSetting : //Changes are accepted
                addWidget();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
