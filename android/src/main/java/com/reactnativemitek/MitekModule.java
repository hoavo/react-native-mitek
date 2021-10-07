package com.reactnativemitek;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.miteksystems.facialcapture.science.api.params.FacialCaptureApi;
import com.miteksystems.facialcapture.workflow.FacialCaptureWorkflowActivity;
import com.miteksystems.facialcapture.workflow.params.FacialCaptureWorkflowParameters;
import com.miteksystems.misnap.misnapworkflow_UX2.MiSnapWorkflowActivity_UX2;
import com.miteksystems.misnap.misnapworkflow_UX2.params.WorkflowApi;
import com.miteksystems.misnap.params.BarcodeApi;
import com.miteksystems.misnap.params.CameraApi;
import com.miteksystems.misnap.params.MiSnapApi;

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

    private static final String FACIAL_CAPTURE_LICENSE_KEY = "{\"signature\":\"wVp+6TCSsx3K0UZ\\/TlAQp+OcyRSqLr7LrF4BwoJDyJFLqDceJGn01ThvNE7CXyhcK7\\/4fo7YAjzzhSt78QbFQPSjlYNmsSANBYhhk4sAck4J7XRJ5eFOZR076wV1PvFlRAb3h0WdoHQzyey6H85N8o7Yyy45fiBUEAyy32DmEuKRg+wtMebDyguy5bTnqqmbGLdRyGqJrskuRTn6vSaRAieMNJjBB0q\\/G7lYPFttiemimPZh6zlEH+VfB7oaGTUF4XoUGhJExLEyhfsNh7AqYi423xFytC8\\/H+YKkYK7Ed1OvtgTR+n7kJPmzcfRVbmbxBugrIgvlL81ughUVKQQ1w==\",\"organization\":\"Daon\",\"signed\":{\"features\":[\"ALL\"],\"expiry\":\"2021-08-05 00:00:00\",\"applicationIdentifier\":\"com.bakkt.FacialCaptureSampleApp\"},\"version\":\"2.1\"}";

    private final ReactApplicationContext reactContext;
    private Promise promise;
    private String selectedJobType;

    private long mTime;

    private final ActivityEventListener activityEventListener = new ActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (MiSnapApi.RESULT_PICTURE_CODE == requestCode) {
                if (RESULT_OK == resultCode) {
                    if (data != null) {
                        Bundle extras = data.getExtras();
                        String miSnapResultCode = extras.getString(MiSnapApi.RESULT_CODE);

                        switch (miSnapResultCode) {
                            // MiSnap check capture
                            case MiSnapApi.RESULT_SUCCESS_VIDEO:
                            case MiSnapApi.RESULT_SUCCESS_STILL:
                                String mibiData = extras.getString(MiSnapApi.RESULT_MIBI_DATA);
                                // Image returned successfully
                                byte[] sImage = data.getByteArrayExtra(MiSnapApi.RESULT_PICTURE_DATA);

                                String base64EncodedImage = Base64.encodeToString(sImage, Base64.DEFAULT);

                                final WritableMap map = Arguments.createMap();
                                if (MiSnapApi.PARAMETER_DOCTYPE_DRIVER_LICENSE.equalsIgnoreCase(selectedJobType)) {
                                    map.putString("miSnapImage", base64EncodedImage);
                                    map.putString("miSnapMibiData", mibiData);
                                } else {
                                    map.putString("faceCaptureImage", base64EncodedImage);
                                    map.putString("faceCaptureMibiData", mibiData);
                                }

                                promise.resolve(map);
                                break;
                            // Barcode capture
                            case MiSnapApi.RESULT_SUCCESS_PDF417:
                                processPDF417(data);
                                break;
                        }
                    } else {
                        // Image canceled, stop
                        promise.reject(new Throwable("MiSnap canceled"));
                    }
                } else if (RESULT_CANCELED == resultCode) {
                    // Camera not working or not available, stop
                    if (data != null) {
                        Bundle extras = data.getExtras();
                        String miSnapResultCode = extras.getString(MiSnapApi.RESULT_CODE);
                        if (!miSnapResultCode.isEmpty()) {
                            promise.reject(new Throwable("Shutdown reason: " + miSnapResultCode));
                        }
                    } else {
                        if (MiSnapApi.PARAMETER_DOCTYPE_DRIVER_LICENSE.equalsIgnoreCase(selectedJobType)) {
                            promise.reject(new Throwable("Capture Driver License cancelled!"));
                        } else if (MiSnapApi.PARAMETER_DOCTYPE_PDF417.equalsIgnoreCase(selectedJobType)) {
                            promise.reject(new Throwable("Capture PDF417 cancelled!"));
                        } else {
                            promise.reject(new Throwable("Capture Facial cancelled!"));
                        }
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
    public void setServerTypeAndVersion(final String serverType,
                       final String serverVersion) {
    }

    @ReactMethod
    public void setDocumentType(final String docType) {
        this.selectedJobType = docType;
    }

    @ReactMethod
    public void doSnap(final Promise promise) {
        this.promise = promise;
        startSnapFlow();
    }

    @ReactMethod
    public void doCaptureFace(final Promise promise) {
        this.promise = promise;
        startFacialCaptureWorkflow();
    }

    private void startSnapFlow() {
        // Prevent multiple MiSnap instances by preventing multiple button presses
        if (System.currentTimeMillis() - mTime < PREVENT_DOUBLE_CLICK_TIME_MS) {
            Log.e("UxStateMachine", "Double-press detected");
            return;
        }

        mTime = System.currentTimeMillis();
        JSONObject misnapParams = new JSONObject();
        try {
            misnapParams.put(MiSnapApi.MiSnapDocumentType, selectedJobType);

            // Example of how to add additional barcode scanning options
            if (selectedJobType.equals(MiSnapApi.PARAMETER_DOCTYPE_BARCODES)) {
                // Set everything except Code 128 because it conflicts w/ the PDF417 barcode on the back of most drivers licenses.
                misnapParams.put(BarcodeApi.BarCodeTypes, BarcodeApi.BARCODE_ALL - BarcodeApi.BARCODE_CODE_128);
            }

            // Here you can override optional API parameter defaults
            misnapParams.put(CameraApi.MiSnapAllowScreenshots, 1);
            // e.g. misnapParams.put(MiSnapApi.AppVersion, "1.0");
            // Workflow parameters are now put into the same JSONObject as MiSnap parameters
            misnapParams.put(WorkflowApi.MiSnapTrackGlare, WorkflowApi.TRACK_GLARE_ENABLED);
            //misnapParams.put(CameraApi.MiSnapFocusMode, CameraApi.PARAMETER_FOCUS_MODE_HYBRID_NEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent intentMiSnap;
        final Activity activity = reactContext.getCurrentActivity();
        intentMiSnap = new Intent(activity, MiSnapWorkflowActivity_UX2.class);
        intentMiSnap.putExtra(MiSnapApi.JOB_SETTINGS, misnapParams.toString());
        activity.startActivityForResult(intentMiSnap, MiSnapApi.RESULT_PICTURE_CODE);
    }

    private void startFacialCaptureWorkflow() {
        // Prevent multiple MiSnap instances by preventing multiple button presses
        if (System.currentTimeMillis() - mTime < PREVENT_DOUBLE_CLICK_TIME_MS) {
            // Double-press detected
            return;
        }

        final Activity activity = reactContext.getCurrentActivity();
        mTime = System.currentTimeMillis();

        // Add in parameter info for MiSnap
        ParameterOverrides overrides = new ParameterOverrides(activity);
        Map<String, Integer> paramMap = overrides.load();
        JSONObject jjs = new JSONObject();
        try {
            // MiSnap-specific parameters
            jjs.put(CameraApi.MiSnapAllowScreenshots, 1);

            // Add FacialCapture-specific parameters from the Settings Activity, stored in shared preferences
            // NOTE: If you do not set these, the optimized defaults for this SDK version will be used.
            // NOTE: Do not set these unless you are purposefully overriding the defaults!
            for (Map.Entry<String, Integer> param : paramMap.entrySet()) {
                jjs.put(param.getKey(), param.getValue());
            }
            jjs.put(FacialCaptureApi.FacialCaptureLicenseKey, FACIAL_CAPTURE_LICENSE_KEY);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject jjsWorkflow = new JSONObject();
        try {
            // Optionally add in customizable runtime settings for the FacialCapture workflow.
            // NOTE: These don't go into the JOB_SETTINGS because they are for your app, not for core FacialCapture.
            jjsWorkflow.put(FacialCaptureWorkflowParameters.FACIALCAPTURE_WORKFLOW_MESSAGE_DELAY, 500);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent intentFacialCapture = new Intent(activity, FacialCaptureWorkflowActivity.class);
        intentFacialCapture.putExtra(MiSnapApi.JOB_SETTINGS, jjs.toString());
        intentFacialCapture.putExtra(FacialCaptureWorkflowParameters.EXTRA_WORKFLOW_PARAMETERS, jjsWorkflow.toString());
        activity.startActivityForResult(intentFacialCapture, MiSnapApi.RESULT_PICTURE_CODE);
    }

    private void processPDF417(Intent returnIntent) {
        String mibiData = returnIntent.getExtras().getString(MiSnapApi.RESULT_MIBI_DATA);
        String sUnformattedStr = returnIntent.getExtras().getString(BarcodeApi.RESULT_PDF417_DATA);
        if (sUnformattedStr != null) {
            final WritableMap map = Arguments.createMap();
            map.putString("miSnapBarcodeScannerMIBIData", mibiData);
            map.putString("miSnapPDF417Data", sUnformattedStr);
            promise.resolve(map);
        }
    }

}
