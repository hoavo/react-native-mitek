package com.mitekmisnaprnbridge;

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
import com.miteksystems.misnap.misnapworkflow_UX2.MiSnapWorkflowActivity_UX2;
import com.miteksystems.misnap.misnapworkflow_UX2.params.WorkflowApi;
import com.miteksystems.misnap.params.BarcodeApi;
import com.miteksystems.misnap.params.CameraApi;
import com.miteksystems.misnap.params.CreditCardApi;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.params.ScienceApi;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Base64;

import java.util.Map;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

@ReactModule(name = MitekMisnapRnBridgeModule.NAME)
public class MitekMisnapRnBridgeModule extends ReactContextBaseJavaModule {
    public static final String NAME = "MitekMisnapRnBridge";
    private static final long PREVENT_DOUBLE_CLICK_TIME_MS = 1000;
    private long mTime;
    private static int mGeoRegion = ScienceApi.GEO_REGION_US;
    private final ReactApplicationContext reactContext;
    private Promise promise;
    private String selectedJobType;
    private int ocrMode = ScienceApi.REQUEST_OCR_NONE;

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

    public MitekMisnapRnBridgeModule(ReactApplicationContext reactContext) {
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
    public void doMiSnapWorkflow(final String docType, final Promise promise) {
        this.promise = promise;
        this.selectedJobType = docType;
        startMiSnapWorkflow();
    }

    private void startMiSnapWorkflow() {
        // Prevent multiple MiSnap instances by preventing multiple button presses
        if (System.currentTimeMillis() - mTime < PREVENT_DOUBLE_CLICK_TIME_MS) {
            Log.e("UxStateMachine", "Double-press detected");
            return;
        }

        mTime = System.currentTimeMillis();
        JSONObject misnapParams = new JSONObject();
        try {
            misnapParams.put(MiSnapApi.MiSnapDocumentType, selectedJobType);
            if (selectedJobType.equals(MiSnapApi.PARAMETER_DOCTYPE_CHECK_FRONT)) {
                misnapParams.put(ScienceApi.MiSnapGeoRegion, mGeoRegion);
            }


            // Example of how to add additional barcode scanning options
            if (selectedJobType.equals(MiSnapApi.PARAMETER_DOCTYPE_BARCODES)) {
                // Set everything except Code 128 because it conflicts w/ the PDF417 barcode on the back of most drivers licenses.
                misnapParams.put(BarcodeApi.BarCodeTypes, BarcodeApi.BARCODE_ALL - BarcodeApi.BARCODE_CODE_128);
            }

            misnapParams.put(ScienceApi.MiSnapRequestOCR, ocrMode);

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
