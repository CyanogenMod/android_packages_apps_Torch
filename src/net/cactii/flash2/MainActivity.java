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
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    private TorchWidgetProvider mWidgetProvider;

    /** On button */
    private ToggleButton mOnButton;

    /** Strobe toggle */
    private CheckBox mStrobe;

    /** High brightness toggle */
    private CheckBox mHighBright;

    private boolean mBright;

    /** Strobe frequency slider */
    private SeekBar mSlider;

    /** Period of the strobe in milliseconds */
    private int mStrobePeriod;

    private Context mContext;

    /** Label showing strobe frequency */
    private TextView mStrobeLabel;

    private SharedPreferences mPrefs;

    private SharedPreferences.Editor mPrefsEditor = null;

    private static boolean sUseBrightSetting = !Build.DEVICE.equals("crespo");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainnew);
        mContext = this.getApplicationContext();
        mOnButton = (ToggleButton) findViewById(R.id.buttonOn);
        mStrobe = (CheckBox) findViewById(R.id.strobe);
        mStrobeLabel = (TextView) findViewById(R.id.strobeTimeLabel);
        mSlider = (SeekBar) findViewById(R.id.slider);
        mHighBright = (CheckBox) findViewById(R.id.bright);

        mStrobePeriod = 100;

        mWidgetProvider = TorchWidgetProvider.getInstance();

        // Preferences
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // preferenceEditor
        this.mPrefsEditor = this.mPrefs.edit();

        if (sUseBrightSetting) {
            mBright = this.mPrefs.getBoolean("bright", false);
            mHighBright.setChecked(mBright);
            mHighBright.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked && mPrefs.getBoolean("bright", false))
                        MainActivity.this.mBright = true;
                    else if (isChecked)
                        openBrightDialog();
                    else {
                        mBright = false;
                        mPrefsEditor.putBoolean("bright", false);
                        mPrefsEditor.commit();
                    }
                }
            });
        } else {
            mHighBright.setEnabled(false);
        }
        mStrobeLabel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mStrobe.setChecked(!mStrobe.isChecked());
            }
        });

        mOnButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
                intent.putExtra("strobe", mStrobe.isChecked());
                intent.putExtra("period", mStrobePeriod);
                intent.putExtra("bright", mBright);
                mContext.sendBroadcast(intent);
            }
        });

        // Strobe frequency slider bar handling
        setProgressBarVisibility(true);
        mSlider.setHorizontalScrollBarEnabled(true);
        mSlider.setProgress(400 - this.mPrefs.getInt("strobeperiod", 100));
        mStrobePeriod = this.mPrefs.getInt("strobeperiod", 100);
        final String strStrobeLabel = this.getString(R.string.setting_frequency_title);
        mStrobeLabel.setText(strStrobeLabel + ": " +
                666 / mStrobePeriod + "Hz / " + 40000 / mStrobePeriod + "BPM");
        mSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mStrobePeriod = 401 - progress;
                if (mStrobePeriod < 20)
                    mStrobePeriod = 20;

                mStrobeLabel.setText(strStrobeLabel + ": " +
                        666 / mStrobePeriod + "Hz / " + 40000 / mStrobePeriod + "BPM");

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
        if (!this.mPrefs.getBoolean("aboutSeen", false)) {
            this.openAboutDialog();
            this.mPrefsEditor.putBoolean("aboutSeen", true);
        }
    }

    public void onPause() {
        this.mPrefsEditor.putInt("strobeperiod", mStrobePeriod);
        this.mPrefsEditor.commit();
        this.updateWidget();
        mContext.unregisterReceiver(mStateReceiver);
        super.onPause();
    }

    public void onDestroy() {
        this.updateWidget();
        super.onDestroy();
    }

    public void onResume() {
        updateBigButtonState();
        this.updateWidget();
        mContext.registerReceiver(mStateReceiver, new IntentFilter(TorchSwitch.TORCH_STATE_CHANGED));
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.about:
                openAboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void openAboutDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.aboutview, null);
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(this.getString(R.string.about_title))
                .setView(view)
                .setNegativeButton(this.getString(R.string.about_close),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Log.d(MSG_TAG, "Close pressed");
                            }
                        })
                .show();
    }

    private void openBrightDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.brightwarn, null);
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(this.getString(R.string.warning_label))
                .setView(view)
                .setNegativeButton(this.getString(R.string.brightwarn_negative),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                MainActivity.this.mHighBright.setChecked(false);
                            }
                        })
                .setNeutralButton(this.getString(R.string.brightwarn_accept),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                MainActivity.this.mBright = true;
                                mPrefsEditor.putBoolean("bright", true);
                                mPrefsEditor.commit();
                            }
                        })
                .show();
    }

    public void updateWidget() {
        this.mWidgetProvider.updateAllStates(mContext);
    }

    private void updateBigButtonState() {
        if (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.TORCH_STATE, 0) == 1) {
            mOnButton.setChecked(true);
            mHighBright.setEnabled(false);
            mStrobe.setEnabled(false);
            if (!mStrobe.isChecked()) {
                mSlider.setEnabled(false);
            }
        } else {
            mOnButton.setChecked(false);
            mHighBright.setEnabled(sUseBrightSetting);
            mStrobe.setEnabled(true);
            mSlider.setEnabled(true);
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
