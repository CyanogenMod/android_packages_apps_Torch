/*
 * Copyright (C) 2014 The CyanogenMod Project
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
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

public class MainActivity extends Activity {

    private static final int ANIMATION_DURATION = 300;

    private TorchWidgetProvider mWidgetProvider;

    private Context mContext;

    private boolean mBright;
    private boolean mTorchOn;

    // Preferences
    private SharedPreferences mPrefs;

    private boolean mHasBrightSetting = false;

    private float mFullScreenScale;

    private ImageView mLightbulb, mLightbulbOn;
    private ImageView mBackgroundShape;

    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TorchSwitch.TORCH_STATE_CHANGED)) {
                mTorchOn = intent.getIntExtra("state", 0) != 0;
                if (mTorchOn) {
                    onFlashOn();
                } else {
                    onFlashOff();
                }
            }
        }
    };

    private void onFlashOn() {
        if (mBackgroundShape == null) {
            return;
        }
        getActionBar().hide();
        mBackgroundShape.animate()
                .scaleX(mFullScreenScale)
                .scaleY(mFullScreenScale)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(ANIMATION_DURATION);
        mLightbulbOn.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION);
    }

    private void onFlashOff() {
        if (mBackgroundShape == null) {
            return;
        }
        getActionBar().show();
        mBackgroundShape.animate()
                .scaleX(1)
                .scaleY(1)
                .setInterpolator(new OvershootInterpolator())
                .setDuration(ANIMATION_DURATION);
        mLightbulbOn.animate()
                .alpha(0f)
                .setDuration(ANIMATION_DURATION);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().show();

        setContentView(R.layout.main);

        mBackgroundShape = (ImageView) findViewById(R.id.bg);
        mLightbulb = (ImageView) findViewById(R.id.lightbulb);
        mLightbulbOn = (ImageView) findViewById(R.id.lightbulb_on);

        mTorchOn = false;

        mWidgetProvider = TorchWidgetProvider.getInstance();

        // Preferences
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mBright = mPrefs.getBoolean("bright", false);

        mHasBrightSetting = getResources().getBoolean(R.bool.hasHighBrightness) &&
                !getResources().getBoolean(R.bool.useCameraInterface);

        mLightbulb.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
                intent.putExtra("bright", mBright);
                sendBroadcast(intent);
            }
        });
    }

    @Override
    public void onPause() {
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
        updateWidget();
        registerReceiver(mStateReceiver, new IntentFilter(TorchSwitch.TORCH_STATE_CHANGED));
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem brightness = menu.findItem(R.id.action_high_brightness);
        if (mHasBrightSetting) {
            brightness.setChecked(mBright);
        } else {
            brightness.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_about) {
            openAboutDialog();
            return true;
        } else if (menuItem.getItemId() == R.id.action_high_brightness) {
            boolean isChecked = false;
            menuItem.setChecked(isChecked = !menuItem.isChecked());
            if (isChecked && !mPrefs.contains("bright")) {
                // reverse reverse!
                menuItem.setChecked(!isChecked);
                openBrightDialog();
            } else if (isChecked) {
                mBright = true;
                mPrefs.edit().putBoolean("bright", true).commit();
            } else {
                mBright = false;
                mPrefs.edit().putBoolean("bright", false).commit();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFullScreenScale = getMeasureScale();
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

    private float getMeasureScale() {
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float displayHeight = outMetrics.heightPixels;
        float displayWidth  = outMetrics.widthPixels;
        return (Math.max(displayHeight, displayWidth) /
                mContext.getResources().getDimensionPixelSize(R.dimen.button_size)) * 2;
    }
}
