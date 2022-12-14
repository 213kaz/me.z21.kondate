package me.z21.kondate;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import me.z21.kondate.databinding.ActivityFullscreenBinding;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    private static final String     TAG           = FullscreenActivity.class.getSimpleName();
    private static final String[]   mPermission = {
              Manifest.permission.INTERNET
            , Manifest.permission.RECEIVE_BOOT_COMPLETED
    };
    private static final int        REQUEST_CODE = 1231;
    private WebView                 webView;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler(Looper.myLooper());
    private View mContentView;
    private View mControlsView;
    private boolean mVisible;
    private ActivityFullscreenBinding binding;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            if (Build.VERSION.SDK_INT >= 30) {
                mContentView.getWindowInsetsController().hide(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            } else {
                // Note that some of these constants are new as of API 16 (Jelly Bean)
                // and API 19 (KitKat). It is safe to use them, as they are inlined
                // at compile-time and do nothing on earlier devices.
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    };

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            Log.d(TAG,"onTouch");
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFullscreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //
        webView = findViewById(R.id.fullscreen_content);
        WebSettings settings = webView.getSettings();
        // ????????????????????????????????????????????????????????????????????????
        settings.setLoadWithOverviewMode(true);

        mVisible = true;
        mControlsView = binding.fullscreenContentControls;
        mContentView = binding.fullscreenContent;

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });


        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //binding.dummyButton.setOnTouchListener(mDelayHideTouchListener);
//        binding.fullscreenContent.setOnTouchListener(mDelayHideTouchListener);
        mContentView.setOnTouchListener(mDelayHideTouchListener);

        // ??????????????????
        checkPermission();

        // ????????????????????????
        setScreenON();

        regReciever();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume()");

        // URL??????
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String strUrl     = sharedPreferences.getString(getString(R.string.url), getString(R.string.default_url));

        Log.d(TAG,"URL:" + strUrl);

        // html??????????????????????????????????????????
        webView.loadUrl(strUrl);

    }

    ///////////////////////
    //  ??????
    ///////////////////////
    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG,"onDestroy()");

        // ?????????????????????????????????
        setScreenOffTime();

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            mContentView.getWindowInsetsController().show(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        mVisible = true;

        webView.reload();

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);

    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // ????????????
    ////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////
    // ?????????????????????
    //////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    //////////////////////////
    // ????????????????????????
    //////////////////////////
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:  // ??????
                showSetting();
                //Log.d(TAG,"--- MENU --- action_settings ---");
                return true;

            case R.id.action_exit:      // ????????????
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    ///////////////////////
    // ??????????????????
    ///////////////////////
    private void showSetting() {
        Intent intent = new Intent(this, SettingsActivity.class); // ????????????
        startActivity(intent);
        Log.d(TAG,"showSetting()");
    }

    ///////////////////////
    // ?????????????????????????????????????????????
    ///////////////////////
    protected void setScreenON(){

        //final int NO_OFF_VALUE = -1;
        //intScreenOffTime = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, NO_OFF_VALUE);
        //Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, NO_OFF_VALUE);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG,"?????????????????????????????????:?????????");
    }

    ///////////////////////
    // ??????????????????????????????????????????
    ///////////////////////
    protected void setScreenOffTime(){

        //if (intScreenOffTime > 0) {
        //    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, intScreenOffTime);
        //    Log.d(TAG,"?????????????????????????????????" + intScreenOffTime);
        //}

        // Keep screen off
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG,"?????????????????????????????????:??????");
    }

    //private void checkPermission(final String permissions[],final int request_code){
    private void checkPermission(){
        // ????????????????????????????????????????????????????????????????????????????????????????????????
        ActivityCompat.requestPermissions(this, mPermission, REQUEST_CODE);
    }

    private void regReciever(){

        BootReceiver receiver = new BootReceiver();
        IntentFilter intentFilter = new IntentFilter();
        registerReceiver(receiver, intentFilter);

    }


}