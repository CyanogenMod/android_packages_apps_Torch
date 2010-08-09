package net.cactii.flash2;

import net.cactii.flash2.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

public class MainActivity extends Activity {

	public TorchWidgetProvider mWidgetProvider;
	
	public FlashDevice device;
	// On button
	private Button buttonOn;
	// Strobe toggle
	private CheckBox buttonStrobe;
	// Is the strobe running?
	public Boolean strobing;
	
	public RelativeLayout brightRow;
	public RelativeLayout strobeRow;
	
	public CheckBox buttonBright;
	public TextView labelBright;
	public boolean bright;

	// Thread to handle strobing
	public Thread strobeThread;
	public boolean mStrobeThreadRunning;
	
	public Thread torchThread;
	public boolean mTorchThreadRunning;
	public boolean mTorchOn;
	// Strobe frequency slider.
	public SeekBar slider;
	// Period of strobe, in milliseconds
	public int strobeperiod;
	// Strobe has timed out
	public boolean mTimedOut;
	private Context context;
	// Label showing strobe frequency
	public TextView strobeLabel;
	// Represents a 'su' instance
	public Su su_command;
	
	public boolean has_root;
	
	// Preferences
	public SharedPreferences mPrefs;
	public SharedPreferences.Editor mPrefsEditor = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainnew);
        context = this.getApplicationContext();
        buttonOn = (Button) findViewById(R.id.buttonOn);
        buttonStrobe = (CheckBox) findViewById(R.id.strobe);
        strobeLabel = (TextView) findViewById(R.id.strobeTimeLabel);
        slider = (SeekBar) findViewById(R.id.slider);
        buttonBright = (CheckBox)findViewById(R.id.bright);
        labelBright = (TextView)findViewById(R.id.brightLabel);
        
        brightRow = (RelativeLayout)findViewById(R.id.brightRow);
        strobeRow = (RelativeLayout)findViewById(R.id.strobeRow);

        
        strobing = false;
        strobeperiod = 100;
        mTorchOn = false;
        mTorchThreadRunning = false;
        has_root = false;
        
        mWidgetProvider = TorchWidgetProvider.getInstance();
        device = FlashDevice.getInstance();
        
        // Preferences
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // preferenceEditor
        this.mPrefsEditor = this.mPrefs.edit();
        
        bright = this.mPrefs.getBoolean("bright", false);
        buttonBright.setChecked(bright);
        buttonBright.setOnCheckedChangeListener(new OnCheckedChangeListener() {

          @Override
          public void onCheckedChanged(CompoundButton buttonView,
              boolean isChecked) {
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

        
        strobeLabel.setOnClickListener(new OnClickListener() {

          @Override
          public void onClick(View v) {
            buttonStrobe.setChecked(!buttonStrobe.isChecked());
          }
          
        });

        buttonOn.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
            Intent intent;
            
            if (Build.VERSION.RELEASE.equals("2.2")) {
              if (bright) {
                if (device.Writable()) {
                  intent = new Intent(MainActivity.this, RootTorchService.class);
                } else {
                  Toast.makeText(context, "No root, cant go high brightness.", Toast.LENGTH_SHORT).show();
                  intent = new Intent(MainActivity.this, TorchService.class);
                }
              } else {
                intent = new Intent(MainActivity.this, TorchService.class);
              }
            } else {  // Pre-Froyo
              if (!device.Writable()) {
                Toast.makeText(context, "No root, cant open LED.", Toast.LENGTH_SHORT).show();
                return;
              }
              intent = new Intent(MainActivity.this, RootTorchService.class);
            }

            intent.putExtra("strobe", buttonStrobe.isChecked());
            intent.putExtra("period", strobeperiod);
            intent.putExtra("bright", bright);

            if (!mTorchOn) {
              startService(intent);
              mTorchOn = true;
              buttonOn.setText("Off");
              buttonBright.setEnabled(false);
              buttonStrobe.setEnabled(false);
              if (!buttonStrobe.isChecked())
                slider.setEnabled(false);
            } else {
              stopService(intent);
              mTorchOn = false;
              buttonOn.setText("On");
              buttonBright.setEnabled(true);
              buttonStrobe.setEnabled(true);
              slider.setEnabled(true);
            }
          }

        });
     
         // Strobe frequency slider bar handling
        setProgressBarVisibility(true);
        slider.setHorizontalScrollBarEnabled(true);
        slider.setProgress(200 - this.mPrefs.getInt("strobeperiod", 100));
        strobeperiod = this.mPrefs.getInt("strobeperiod", 100);
        strobeLabel.setText("Strobe frequency: " + 500/strobeperiod + "Hz");
        slider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

    			@Override
    			public void onProgressChanged(SeekBar seekBar, int progress,
    					boolean fromUser) {
    				strobeperiod = 201 - progress;
    				if (strobeperiod < 20)
    					strobeperiod = 20;
    				strobeLabel.setText("Strobe frequency: " + 500/strobeperiod + "Hz");
    				
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

        
      if (new File("/dev/msm_camera/config0").exists() == false) {
      	Toast.makeText(context, "Only Nexus One is supported, sorry!", Toast.LENGTH_LONG).show();
      	has_root = false;
      }

      if (!device.Writable()) {
      	Log.d("Torch", "Cant open flash RW");
        su_command = new Su();
      	has_root = this.su_command.can_su;
      	if (!has_root) {
      	  if (!Build.VERSION.RELEASE.equals("2.2")) {
      		  this.openNotRootDialog();
      	  }
          buttonBright.setChecked(false);
          labelBright.setText("High brightness (needs root)");
          labelBright.setTextColor(0xffaaaaaa);
          buttonBright.setEnabled(false);
      		
      	} else
      		su_command.Run("chmod 666 /dev/msm_camera/config0");
      }
    }

    public void onPause() {

      this.mPrefsEditor.putInt("strobeperiod", this.strobeperiod);
    	this.mPrefsEditor.commit();
    	this.updateWidget();
    	super.onPause();
    }
    
    public void onDestroy() {
    	this.updateWidget();
    	super.onDestroy();
    }
    
    public void onResume() {
      if (this.TorchServiceRunning(context)) {
        buttonOn.setText("Off");
        buttonBright.setEnabled(false);
        buttonStrobe.setEnabled(false);
        if (!buttonStrobe.isChecked())
          slider.setEnabled(false);
        this.mTorchOn = true;
      }
    	this.updateWidget();
    	super.onResume();
    }

    private boolean TorchServiceRunning(Context context) {
      ActivityManager am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

      List<ActivityManager.RunningServiceInfo> svcList = am.getRunningServices(100);

      if (!(svcList.size() > 0))
        return false;
      for (int i = 0; i < svcList.size(); i++) {
        RunningServiceInfo serviceInfo = svcList.get(i);
        ComponentName serviceName = serviceInfo.service;
        if (serviceName.getClassName().endsWith(".TorchService")
            || serviceName.getClassName().endsWith(".RootTorchService"))
          return true;
      }
      return false;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean supRetVal = super.onCreateOptionsMenu(menu);
    	menu.addSubMenu(0, 0, 0, "About Torch");
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
		new AlertDialog.Builder(MainActivity.this)
        .setTitle("About")
        .setView(view)
        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        //Log.d(MSG_TAG, "Close pressed");
                }
        })
        .show();  		
   	}
   	
   	private void openNotRootDialog() {
		LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.norootview, null); 
		new AlertDialog.Builder(MainActivity.this)
        .setTitle("Not Root!")
        .setView(view)
        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        MainActivity.this.finish();
                }
        })
        .setNeutralButton("Ignore", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // nothing
                }
        })
        .show();
   	}
   	
    
    private void openBrightDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.brightwarn, null); 
        new AlertDialog.Builder(MainActivity.this)
        .setTitle("Hi-brite 'On' button!!!")
        .setView(view)
        .setNegativeButton("No thanks", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        MainActivity.this.buttonBright.setChecked(false);
                }
        })
        .setNeutralButton("Accept", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                  MainActivity.this.bright = true;
                  mPrefsEditor.putBoolean("bright", true);
                  mPrefsEditor.commit();
                }
        })
        .show();
    }
   	
   	public void updateWidget() {
   		this.mWidgetProvider.updateAllStates(context);
   	}
   	
}
