package com.reactnativemitek;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.miteksystems.facialcapture.workflow.FacialCaptureWorkflowActivity;
import com.miteksystems.facialcapture.workflow.api.FacialCaptureResult;
import com.miteksystems.facialcapture.workflow.params.FacialCaptureWorkflowApi;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.params.CameraApi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Base64;

import java.util.Map;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

@ReactModule(name = MitekModule.NAME)
public class MitekModule extends ReactContextBaseJavaModule {
    public static final String NAME = "Mitek";
    private static final long PREVENT_DOUBLE_CLICK_TIME_MS = 1000;
    private long mTime;

    private final ReactApplicationContext reactContext;
    private Promise promise;

    private final ActivityEventListener activityEventListener = new ActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (MiSnapApi.RESULT_PICTURE_CODE == requestCode) {
                if (RESULT_OK == resultCode) {
                    if (data != null) {
                        Bundle extras = data.getExtras();
                        String mibiData = extras.getString(MiSnapApi.RESULT_MIBI_DATA);
                        // Image returned successfully
                        byte[] sImage = data.getByteArrayExtra(MiSnapApi.RESULT_PICTURE_DATA);

                        String base64EncodedImage = Base64.encodeToString(sImage, Base64.DEFAULT);
                        final WritableMap map = Arguments.createMap();
                        map.putString("faceCaptureImage", base64EncodedImage);
                        map.putString("faceCaptureMibiData", mibiData);
                        promise.resolve(map);
                    }else {
                        promise.reject(new Throwable("MiSnap canceled"));
                    }
                } else if (RESULT_CANCELED == resultCode) {
                    if (data != null) {
                        FacialCaptureResult.Failure failureResult = data.getParcelableExtra(FacialCaptureWorkflowApi.FACIAL_CAPTURE_RESULT);
                        promise.reject(new Throwable("Shutdown reason: " + failureResult.getResultCode()));
                    }else {
                        promise.reject(new Throwable("Shutdown no reason"));
                    }
                }
            }
        }

        @Override
        public void onNewIntent(Intent intent) {
        }
    };

    public MitekModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.reactContext.addActivityEventListener(activityEventListener);
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void startFacialCapture(final Promise promise) {
        this.promise = promise;
        startFacialCaptureWorkflow();
    }
    
    private void startFacialCaptureWorkflow() {
        // Prevent multiple MiSnap instances by preventing multiple button presses
        if (System.currentTimeMillis() - mTime < PREVENT_DOUBLE_CLICK_TIME_MS) {
            // Double-press detected
            return;
        }
        mTime = System.currentTimeMillis();

        // Add in parameter info for MiSnap
        JSONObject jjs = new JSONObject();
        try {
            // MiSnap-specific parameters
            jjs.put(CameraApi.MiSnapAllowScreenshots, 1);

            // An example of how to set workflow parameters
            //jjs.put(FacialCaptureWorkflowApi.FacialCaptureWorkflowMessageDelay, 500);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final Activity activity = reactContext.getCurrentActivity();
        Intent intentFacialCapture = new Intent(activity, FacialCaptureWorkflowActivity.class);
        intentFacialCapture.putExtra(MiSnapApi.JOB_SETTINGS, jjs.toString());

        activity.startActivityForResult(intentFacialCapture, MiSnapApi.RESULT_PICTURE_CODE);
    }

}
