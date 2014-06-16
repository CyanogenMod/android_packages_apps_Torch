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
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;

public class MainActivity extends Activity {

    private static final int ANIMATION_DURATION = 300;

    private TorchWidgetProvider mWidgetProvider;

    private Context mContext;

    private boolean mBright;
    public boolean mTorchOn;

    // Period of strobe, in milliseconds
    public int mStrobePeriod;

    // Preferences
    public SharedPreferences mPrefs;

    public boolean mHasBrightSetting = false;

    private float mFullScreenScale;

    private ImageView mLightbulb, mLightbulbOn;
    private ImageView mBackgroundShape;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

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
        if (mFullScreenScale <= 0.0f) {
            mFullScreenScale = getMeasureScale();
        }
        getActionBar().hide();
        mBackgroundShape.animate().scaleX(mFullScreenScale)
                .scaleY(mFullScreenScale)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(ANIMATION_DURATION);
        mLightbulbOn.animate().alpha(1f).setDuration(ANIMATION_DURATION);
    }

    private void onFlashOff() {
        if (mBackgroundShape == null) {
            return;
        }
        getActionBar().show();
        mBackgroundShape.animate().scaleX(1).scaleY(1)
                .setInterpolator(new OvershootInterpolator())
                .setDuration(ANIMATION_DURATION);
        mLightbulbOn.animate().alpha(0f).setDuration(ANIMATION_DURATION);
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
        mStrobePeriod = mPrefs.getInt("period", 100);

        mHasBrightSetting = getResources().getBoolean(R.bool.hasHighBrightness)
                && !getResources().getBoolean(R.bool.useCameraInterface);

        mLightbulb.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mTorchOn = !mTorchOn;

                Intent intent = new Intent(TorchSwitch.TOGGLE_FLASHLIGHT);
                intent.putExtra("bright", mBright);
                sendBroadcast(intent);
            }
        });

        // Handle Navigation Drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.navbar_shadow, Gravity.LEFT);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new DrawerListAdapter(MainActivity.this));
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                if (view.getTag().equals("About")) {
                    mDrawerLayout.closeDrawers();
                    openAboutDialog();
                }

            }

        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open,
                R.string.drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
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
        registerReceiver(mStateReceiver, new IntentFilter(
                TorchSwitch.TORCH_STATE_CHANGED));
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {

        if (mDrawerToggle.onOptionsItemSelected(menuItem)) {
            return true;
        }
        return false;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFullScreenScale = getMeasureScale();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void openAboutDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.aboutview, null);

        new AlertDialog.Builder(this).setTitle(R.string.about_title)
                .setView(view).setNegativeButton(R.string.about_close, null)
                .show();
    }

    public void openBrightDialog(final CompoundButton ref) {
        LayoutInflater inflater = LayoutInflater.from(this);
        mDrawerLayout.closeDrawers();
        View view = inflater.inflate(R.layout.brightwarn, null);

        new AlertDialog.Builder(this)
                .setTitle(R.string.warning_label)
                .setView(view)
                .setNegativeButton(R.string.brightwarn_negative,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                mBright = false;
                                mPrefs.edit().putBoolean("bright", false)
                                        .commit();

                                if (ref != null)
                                    ref.setChecked(false);
                            }
                        })
                .setPositiveButton(R.string.brightwarn_accept,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                mBright = true;
                                mPrefs.edit().putBoolean("bright", true)
                                        .commit();

                                if (ref != null)
                                    ref.setChecked(true);
                            }
                        }).show();
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
        float displayWidth = outMetrics.widthPixels;
        return (Math.max(displayHeight, displayWidth) / mContext.getResources()
                .getDimensionPixelSize(R.dimen.button_size)) * 2;
    }
}
