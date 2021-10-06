package com.miteksystems.misnap.misnapworkflow_UX2;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import android.view.WindowManager;

import com.miteksystems.misnap.events.OnShutdownEvent;
import com.miteksystems.misnap.misnapworkflow_UX2.ui.overlay.YourCameraOverlayFragment;
import com.miteksystems.misnap.misnapworkflow_UX2.ui.screen.FTManualTutorialFragment;
import com.miteksystems.misnap.misnapworkflow_UX2.ui.screen.FTVideoTutorialFragment;
import com.miteksystems.misnap.misnapworkflow_UX2.ui.screen.ManualHelpFragment;
import com.miteksystems.misnap.misnapworkflow_UX2.ui.screen.VideoDetailedFailoverFragment;
import com.miteksystems.misnap.misnapworkflow_UX2.ui.screen.VideoHelpFragment;
import com.miteksystems.misnap.misnapworkflow_UX2.workflow.UxStateMachine;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.params.MiSnapIntentCheck;
import com.miteksystems.misnap.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

public class MiSnapWorkflowActivity_UX2 extends AppCompatActivity implements
        FTVideoTutorialFragment.OnFragmentInteractionListener,
        FTManualTutorialFragment.OnFragmentInteractionListener,
        VideoHelpFragment.OnFragmentInteractionListener,
        ManualHelpFragment.OnFragmentInteractionListener,
        YourCameraOverlayFragment.OnFragmentInteractionListener,
        VideoDetailedFailoverFragment.OnFragmentInteractionListener {
    private static final String TAG = MiSnapWorkflowActivity_UX2.class.getName();
    private static final String SAVED_CURRENT_STATE = "SAVED_CURRENT_STATE";
    private UxStateMachine mUxStateMachine;
    private int mCurrentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // B-03373: Prevent Intent manipulation vulnerabilities
        // Verify that the starting Intent is not null before proceeding
        if (MiSnapIntentCheck.isDangerous(getIntent())) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Set the application flags/settings here
        // NOTE: You must call these BEFORE calling setContentView!
        // Go full screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Prevent screenshots and viewing on non-secure displays
        String jobSettings = getIntent().getStringExtra(MiSnapApi.JOB_SETTINGS);
        try {
            JSONObject params = new JSONObject(jobSettings);
            CameraParamMgr reader = new CameraParamMgr(params);
            if (reader.getAllowScreenshots() != 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                if (reader.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_LANDSCAPE_DOCUMENT_LANDSCAPE){
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                } else if (reader.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_PORTRAIT_DOCUMENT_LANDSCAPE
                        || reader.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_PORTRAIT_DOCUMENT_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Unlock the screen when running unit and regression tests
        if (BuildConfig.DEBUG) {
            Utils.unlockAndTurnOnScreen(this);
        }

        // NOTE: Make sure you've set the flags first.
        setContentView(R.layout.misnap_activity_misnapworkflow);

        // GPU optimization - prevents unnecessary frame overdraws
        getWindow().setBackgroundDrawable(null);

        mUxStateMachine = new UxStateMachine(this);
        mCurrentState = mUxStateMachine.getCurrentState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUxStateMachine != null) {
            mUxStateMachine.destroy();
            mUxStateMachine = null;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        // You can optionally pass in a locale to override the user's current locale
//        LocaleHelper.changeLanguage(this, "es"); // e.g. Spanish
        mUxStateMachine.resume(mCurrentState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCurrentState = mUxStateMachine.getCurrentState();
        mUxStateMachine.pause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (mUxStateMachine != null) {
            savedInstanceState.putInt(SAVED_CURRENT_STATE, mUxStateMachine.getCurrentState());
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mCurrentState = savedInstanceState.getInt(SAVED_CURRENT_STATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (!mUxStateMachine.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        setResult(resultCode, data);

        String miSnapReason = data.getStringExtra(MiSnapApi.RESULT_CODE);
        EventBus.getDefault().post(new OnShutdownEvent(resultCode, miSnapReason)); // Send STOP event to MiSnap
    }

    //Stub this method to disable system reading out the app's name constantly during accessibility
    @Override
    public boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent event){
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mUxStateMachine != null) {
            mUxStateMachine.onRotate();
        }
    }


    /**************************************************************************
     * All events from fragments are forwarded from this Activity
     * to the state machine for processing.
     */

    // From FTVideoTutorialFragment
    @Override
    public void onFTVideoTutorialDone() {
        mUxStateMachine.onFirstTimeVideoTutorialFragmentDone();
    }

    // From FTManualTutorialFragment
    @Override
    public void onFTManualTutorialDone() {
        mUxStateMachine.onFirstTimeManualTutorialFragmentDone();
    }


    // From VideoHelpFragment
    @Override
    public void onVideoHelpRestartMiSnapSession() {
        mUxStateMachine.onVideoHelpRestartMiSnapSession();
    }
    @Override
    public void onVideoHelpAbortMiSnap() {
        mUxStateMachine.onVideoHelpAbortMiSnap();
    }

    // From ManualHelpFragment
    @Override
    public void onManualHelpRestartMiSnapSession() {
        mUxStateMachine.onManualHelpRestartMiSnapSession();
    }
    @Override
    public void onManualHelpAbortMiSnap() {
        mUxStateMachine.onManualHelpAbortMiSnap();
    }

    // From YourCameraOverlayFragment
    @Override
    public void onHelpButtonClicked() {
        mUxStateMachine.onHelpButtonClicked();
    }
    @Override
    public void onCancelButtonClicked() {
        mUxStateMachine.onCancelButtonClicked();
    }
    @Override
    public void onTorchButtonClicked(boolean shouldTurnOn) {
        mUxStateMachine.onTorchButtonClicked(shouldTurnOn);
    }
    @Override
    public void onCaptureButtonClicked() {
        mUxStateMachine.onCaptureButtonClicked();
    }

//    @Override
//    public void onBackPressed(){
//        try {
//            Thread.sleep(700);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        mUxStateMachine.onBackPressed();
//    }

    // From VideoDetailedFailoverFragment
    @Override
    public void onManualCaptureAfterDetailedFailover() {
        mUxStateMachine.onManualCaptureAfterDetailedFailover();
    }
    @Override
    public void onAbortAfterDetailedFailover() {
        mUxStateMachine.onAbortAfterDetailedFailover();
    }
    @Override
    public void onRetryAfterDetailedFailover() {
        mUxStateMachine.onRetryAfterDetailedFailover();
    }

    @VisibleForTesting
    public void setNextState(int state) {
        mUxStateMachine.nextMiSnapState(state);
    }
}
