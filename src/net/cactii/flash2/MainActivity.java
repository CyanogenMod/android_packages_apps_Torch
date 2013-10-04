
package net.cactii.flash2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.ToggleButton;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TorchWidgetProvider mWidgetProvider;

    // On button
    private ToggleButton buttonOn;

    private Switch strobeSwitch;

    private Switch brightSwitch;

    private boolean bright;

    private boolean mTorchOn;

    // Strobe frequency slider.
    private SeekBar slider;

    // Period of strobe, in milliseconds
    private int strobeperiod;

    private Context context;

    // Label showing strobe frequency
    private TextView strobeLabel;

    // Preferences
    private SharedPreferences mPrefs;

    private SharedPreferences.Editor mPrefsEditor = null;

    private static final boolean useBrightSetting = !Build.DEVICE.equals("crespo");

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainnew);
        context = this.getApplicationContext();
        buttonOn = (ToggleButton) findViewById(R.id.buttonOn);
        strobeSwitch = (Switch) findViewById(R.id.strobe_switch);
        strobeLabel = (TextView) findViewById(R.id.strobeTimeLabel);
        slider = (SeekBar) findViewById(R.id.slider);
        brightSwitch = (Switch) findViewById(R.id.bright_switch);

        strobeperiod = 100;
        mTorchOn = false;

        mWidgetProvider = TorchWidgetProvider.getInstance();

        // Preferences
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // preferenceEditor
        this.mPrefsEditor = this.mPrefs.edit();

        if (useBrightSetting) {
            bright = this.mPrefs.getBoolean("bright", false);
            brightSwitch.setChecked(bright);
            brightSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked && mPrefs.getBoolean("bright", false))
                        MainActivity.this.bright = true;
                    else if (isChecked)
                        openBrightDialog();
                    else {
                        bright = false;
                        mPrefsEditor.putBoolean("bright", false);
                        mPrefsEditor.commit();
                    }
                }
            });
        } else {

            // Fully hide the UI elements on Crespo since we can't use them
            brightSwitch.setVisibility(View.GONE);
            brightSwitch.setEnabled(false);
            findViewById(R.id.ruler2).setVisibility(View.GONE);
        }

        // Set the state of the strobing section and hide as appropriate
        final boolean isStrobing = mPrefs.getBoolean("strobe", false);
        final LinearLayout strobeLayout = (LinearLayout) MainActivity.this.findViewById(R.id
                .strobeRow);
        int visibility = isStrobing ? View.VISIBLE : View.GONE;
        strobeLayout.setVisibility(visibility);
        strobeSwitch.setChecked(isStrobing);
        strobeSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int visibility = isChecked ? View.VISIBLE : View.GONE;
                strobeLayout.setVisibility(visibility);
                mPrefsEditor.putBoolean("strobe", isChecked).commit();
            }
        });

        buttonOn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
                intent.putExtra("strobe", strobeSwitch.isChecked());
                intent.putExtra("period", strobeperiod);
                intent.putExtra("bright", bright);
                context.sendBroadcast(intent);
            }
        });

        // Strobe frequency slider bar handling
        setProgressBarVisibility(true);
        slider.setHorizontalScrollBarEnabled(true);
        slider.setProgress(400 - this.mPrefs.getInt("strobeperiod", 100));
        strobeperiod = this.mPrefs.getInt("strobeperiod", 100);
        final String strStrobeLabel = this.getString(R.string.setting_frequency_title);
        strobeLabel.setText(strStrobeLabel + ": " +
                666 / strobeperiod + "Hz / " + 40000 / strobeperiod + "BPM");
        slider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                strobeperiod = 401 - progress;
                if (strobeperiod < 20)
                    strobeperiod = 20;
                
                strobeLabel.setText(strStrobeLabel + ": " +
                        666 / strobeperiod + "Hz / " + 40000 / strobeperiod + "BPM");

                Intent intent = new Intent("net.cactii.flash2.SET_STROBE");
                intent.putExtra("period", strobeperiod);
                sendBroadcast(intent);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });

        // Show the about dialog, the first time the user runs the app.
        if (!this.mPrefs.getBoolean("aboutSeen", false)) {
            this.openAboutDialog();
            this.mPrefsEditor.putBoolean("aboutSeen", true);
        }
    }

    public void onPause() {
        this.mPrefsEditor.putInt("strobeperiod", this.strobeperiod);
        this.mPrefsEditor.commit();
        this.updateWidget();
        context.unregisterReceiver(mStateReceiver);
        super.onPause();
    }

    public void onDestroy() {
        this.updateWidget();
        super.onDestroy();
    }

    public void onResume() {
        updateBigButtonState();
        this.updateWidget();
        context.registerReceiver(mStateReceiver, new IntentFilter(TorchSwitch.TORCH_STATE_CHANGED));
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_about) {
            openAboutDialog();
            return true;
        }
        return false;
    }

    private void openAboutDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.aboutview, null);
        new AlertDialog.Builder(MainActivity.this).setTitle(this.getString(R.string.about_title)).setView(view)
                .setNegativeButton(this.getString(R.string.about_close), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Log.d(MSG_TAG, "Close pressed");
                    }
                }).show();
    }

    private void openBrightDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.brightwarn, null);
        new AlertDialog.Builder(MainActivity.this).setTitle(this.getString(R.string.warning_label))
                .setView(view)
                .setNegativeButton(this.getString(R.string.brightwarn_negative), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        MainActivity.this.brightSwitch.setChecked(false);
                    }
                }).setNeutralButton(this.getString(R.string.brightwarn_accept), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        MainActivity.this.bright = true;
                        mPrefsEditor.putBoolean("bright", true);
                        mPrefsEditor.commit();
                    }
                }).show();
    }

    void updateWidget() {
        this.mWidgetProvider.updateAllStates(context);
    }

    private void updateBigButtonState() {
        buttonOn.setChecked(mTorchOn);
        brightSwitch.setEnabled(!mTorchOn && useBrightSetting);
        strobeSwitch.setEnabled(!mTorchOn);
        slider.setEnabled(!mTorchOn || strobeSwitch.isChecked());
    }

    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TorchSwitch.TORCH_STATE_CHANGED)) {
                mTorchOn = intent.getIntExtra("state", 0) != 0;
                updateBigButtonState();
            }
        }
    };
}
