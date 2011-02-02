
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
import android.provider.Settings;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TorchWidgetProvider mWidgetProvider;

    // On button
    private ToggleButton buttonOn;

    // Strobe toggle
    private CheckBox buttonStrobe;

    private CheckBox buttonBright;

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
    
    // Labels
    private String labelOn = null;
    private String labelOff = null;

    private static boolean useBrightSetting = !Build.DEVICE.equals("crespo");

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainnew);
        context = this.getApplicationContext();
        buttonOn = (ToggleButton) findViewById(R.id.buttonOn);
        buttonStrobe = (CheckBox) findViewById(R.id.strobe);
        strobeLabel = (TextView) findViewById(R.id.strobeTimeLabel);
        slider = (SeekBar) findViewById(R.id.slider);
        buttonBright = (CheckBox) findViewById(R.id.bright);

        strobeperiod = 100;
        mTorchOn = false;

        labelOn = this.getString(R.string.label_on);
        labelOff = this.getString(R.string.label_off);

        mWidgetProvider = TorchWidgetProvider.getInstance();

        // Preferences
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // preferenceEditor
        this.mPrefsEditor = this.mPrefs.edit();

        if (useBrightSetting) {
            bright = this.mPrefs.getBoolean("bright", false);
            buttonBright.setChecked(bright);
            buttonBright.setOnCheckedChangeListener(new OnCheckedChangeListener() {
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
            buttonBright.setEnabled(false);
        }
        strobeLabel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonStrobe.setChecked(!buttonStrobe.isChecked());
            }
        });

        buttonOn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
                intent.putExtra("strobe", buttonStrobe.isChecked());
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
        boolean supRetVal = super.onCreateOptionsMenu(menu);
        menu.addSubMenu(0, 0, 0, this.getString(R.string.about_btn));
        return supRetVal;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        boolean supRetVal = super.onOptionsItemSelected(menuItem);
        this.openAboutDialog();
        return supRetVal;
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
        new AlertDialog.Builder(MainActivity.this).setTitle(this.getString(R.string.brightwarn_title))
                .setView(view)
                .setNegativeButton(this.getString(R.string.brightwarn_negative), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        MainActivity.this.buttonBright.setChecked(false);
                    }
                }).setNeutralButton(this.getString(R.string.brightwarn_accept), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        MainActivity.this.bright = true;
                        mPrefsEditor.putBoolean("bright", true);
                        mPrefsEditor.commit();
                    }
                }).show();
    }

    public void updateWidget() {
        this.mWidgetProvider.updateAllStates(context);
    }

    private void updateBigButtonState() {
        if (Settings.System.getInt(context.getContentResolver(),
                Settings.System.TORCH_STATE, 0) == 1) {
            mTorchOn = true;
            buttonOn.setChecked(true);
            buttonBright.setEnabled(false);
            buttonStrobe.setEnabled(false);
            if (!buttonStrobe.isChecked()) {
                slider.setEnabled(false);
            }
        } else {
            mTorchOn = false;
            buttonOn.setChecked(false);
            buttonBright.setEnabled(useBrightSetting);
            buttonStrobe.setEnabled(true);
            slider.setEnabled(true);
        }
    }

    private BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TorchSwitch.TORCH_STATE_CHANGED)) {
                updateBigButtonState();
            }
        }
    };
}
