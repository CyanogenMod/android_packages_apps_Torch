/*
 * Copyright (C) 2013 The CyanogenMod Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

    private ToggleButton mButtonOn;
    private Switch mStrobeSwitch;
    private Switch mBrightSwitch;

    private boolean mBright;
    private boolean mTorchOn;

    // Strobe frequency slider.
    private SeekBar mSlider;

    // Period of strobe, in milliseconds
    private int mStrobePeriod;

    // Label showing strobe frequency
    private TextView mStrobeLabel;

    // Preferences
    private SharedPreferences mPrefs;

    private boolean mHasBrightSetting = false;

    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TorchSwitch.TORCH_STATE_CHANGED)) {
                mTorchOn = intent.getIntExtra("state", 0) != 0;
                updateBigButtonState();
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mButtonOn = (ToggleButton) findViewById(R.id.buttonOn);
        mStrobeSwitch = (Switch) findViewById(R.id.strobe_switch);
        mStrobeLabel = (TextView) findViewById(R.id.strobeTimeLabel);
        mSlider = (SeekBar) findViewById(R.id.slider);
        mBrightSwitch = (Switch) findViewById(R.id.bright_switch);

        mStrobePeriod = 100;
        mTorchOn = false;

        mWidgetProvider = TorchWidgetProvider.getInstance();

        // Preferences
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mHasBrightSetting = getResources().getBoolean(R.bool.hasHighBrightness) &&
                                !getResources().getBoolean(R.bool.useCameraInterface);
        if (mHasBrightSetting) {
            mBright = mPrefs.getBoolean("bright", false);
            mBrightSwitch.setChecked(mBright);
            mBrightSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked && mPrefs.getBoolean("bright", false)) {
                        mBright = true;
                    } else if (isChecked) {
                        openBrightDialog();
                    } else {
                        mBright = false;
                        mPrefs.edit().putBoolean("bright", false).commit();
                    }
                }
            });
        } else {
            // Fully hide the UI elements on Crespo since we can't use them
            mBrightSwitch.setVisibility(View.GONE);
            findViewById(R.id.ruler2).setVisibility(View.GONE);
        }

        // Set the state of the strobing section and hide as appropriate
        final boolean isStrobing = mPrefs.getBoolean("strobe", false);
        final LinearLayout strobeLayout = (LinearLayout) findViewById(R.id.strobeRow);
        int visibility = isStrobing ? View.VISIBLE : View.GONE;

        strobeLayout.setVisibility(visibility);
        mStrobeSwitch.setChecked(isStrobing);
        mStrobeSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int visibility = isChecked ? View.VISIBLE : View.GONE;
                strobeLayout.setVisibility(visibility);
                mPrefs.edit().putBoolean("strobe", isChecked).commit();
            }
        });

        mButtonOn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
                intent.putExtra("strobe", mStrobeSwitch.isChecked());
                intent.putExtra("period", mStrobePeriod);
                intent.putExtra("bright", mBright);
                sendBroadcast(intent);
            }
        });

        // Strobe frequency slider bar handling
        setProgressBarVisibility(true);
        updateStrobePeriod(mPrefs.getInt("strobeperiod", 100));
        mSlider.setHorizontalScrollBarEnabled(true);
        mSlider.setProgress(400 - mStrobePeriod);

        mSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateStrobePeriod(Math.max(20, 401 - progress));

                Intent intent = new Intent("net.cactii.flash2.SET_STROBE");
                intent.putExtra("period", mStrobePeriod);
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
        if (!mPrefs.getBoolean("aboutSeen", false)) {
            openAboutDialog();
            mPrefs.edit().putBoolean("aboutSeen", true).commit();
        }
    }

    @Override
    public void onPause() {
        mPrefs.edit().putInt("strobeperiod", mStrobePeriod).commit();
        updateWidget();
        unregisterReceiver(mStateReceiver);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        updateWidget();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        updateBigButtonState();
        updateWidget();
        registerReceiver(mStateReceiver, new IntentFilter(TorchSwitch.TORCH_STATE_CHANGED));
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

    private void updateStrobePeriod(int period) {
        mStrobeLabel.setText(getString(R.string.setting_frequency_title) + ": " +
                666 / period + "Hz / " + 40000 / period + "BPM");
        mStrobePeriod = period;
    }

    private void openAboutDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.aboutview, null);

        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setView(view)
                .setNegativeButton(R.string.about_close, null)
                .show();
    }

    private void openBrightDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.brightwarn, null);

        new AlertDialog.Builder(this)
                .setTitle(R.string.warning_label)
                .setView(view)
                .setNegativeButton(R.string.brightwarn_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mBrightSwitch.setChecked(false);
                    }
                })
                .setPositiveButton(R.string.brightwarn_accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mBright = true;
                        mPrefs.edit().putBoolean("bright", true).commit();
                    }
                })
                .show();
    }

    private void updateWidget() {
        mWidgetProvider.updateAllStates(this);
    }

    private void updateBigButtonState() {
        mButtonOn.setChecked(mTorchOn);
        mBrightSwitch.setEnabled(!mTorchOn && mHasBrightSetting);
        mStrobeSwitch.setEnabled(!mTorchOn);
        mSlider.setEnabled(!mTorchOn || mStrobeSwitch.isChecked());
    }
}
