package com.miteksystems.misnaphybridcontroller;

import android.app.Activity;
import androidx.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.ViewGroup;

import com.miteksystems.misnap.ControllerFragment;
import com.miteksystems.misnap.analyzer.MiSnapAnalyzer;
import com.miteksystems.misnap.barcode.analyzer.BarcodeAnalyzer;
import com.miteksystems.misnap.barcode.analyzer.MiSnapBarcodeDetector;
import com.miteksystems.misnap.camera.MiSnapCamera;
import com.miteksystems.misnap.events.OnCapturedFrameEvent;
import com.miteksystems.misnap.events.SetCaptureModeEvent;
import com.miteksystems.misnap.natives.MiSnapScience;
import com.miteksystems.misnap.params.BarcodeApi;
import com.miteksystems.misnap.params.CameraApi;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import static com.miteksystems.misnap.params.MiSnapApi.RESULT_ERROR_SDK_STATE_ERROR;

public class MiSnapHybridFragment extends ControllerFragment {

    private static final String TAG = MiSnapHybridFragment.class.getName();
    private static final int MISNAP_ORIENTATION_COUNT = 4;
    private MiSnapHybridController controller;
    private MiSnapAnalyzer miSnapAnalyzer;
    private BarcodeAnalyzer barcodeAnalyzer;
    private MiSnapBarcodeDetector miSnapBarcodeDetector;
    private OrientationEventListener orientationEventListener;
    private int lastOrientation;

    @Override
    protected void init() {
        setOrientationListener(getActivity().getApplicationContext());
        super.init();
    }

    @Override
    protected void deinit() {
        super.deinit();
        if (controller != null) {
            controller.end();
            controller = null;
        }

        if (miSnapAnalyzer != null) {
            miSnapAnalyzer.deinit();
            //no longer null because onOrientationChanged() could be called in a different thread than deinit()
//            analyzer = null;
        }

        if (barcodeAnalyzer != null) {
            barcodeAnalyzer.deinit();
            //no longer null because onOrientationChanged() could be called in a different thread than deinit()
//            analyzer = null;
        }

        if (miSnapBarcodeDetector != null) {
            miSnapBarcodeDetector.deinit();
        }

        stopOrientationListener();
    }

    @Override
    public void initializeController() {
        try {
            //set the surfaceview in the main activity
            MiSnapCamera camera = cameraMgr.getMiSnapCamera();
            if (camera != null) {
                miSnapAnalyzer = new MiSnapAnalyzer(getActivity().getApplicationContext(), miSnapParams, getDlDocumentOrientation(), getDeviceOrientation(), false);
                JSONObject customParams = new JSONObject(miSnapParams.toString());
                customParams.put(MiSnapApi.MiSnapDocumentType, MiSnapApi.PARAMETER_DOCTYPE_PDF417);
                barcodeAnalyzer = new BarcodeAnalyzer(getActivity().getApplicationContext(), customParams, getActivity().getResources().getConfiguration().orientation, getBarcodeDocumentOrientation());
                miSnapBarcodeDetector = new MiSnapBarcodeDetector();
                miSnapAnalyzer.init();
                barcodeAnalyzer.init();
                miSnapBarcodeDetector.init();
//                barcodeAnalyzer.setScanningSpeed(BarcodeApi.BARCODE_SPEED_SLOW);
                controller = new MiSnapHybridController(camera, miSnapAnalyzer, barcodeAnalyzer, miSnapBarcodeDetector, miSnapParams);

                // this is needed so that the frame can be returned in return intent
                controller.getResult().observe(this, new Observer<MiSnapHybridControllerResult>() {
                    @Override
                    public void onChanged(@Nullable MiSnapHybridControllerResult result) {
                        if (null == result) {
                            Log.w(TAG, "empty result");
                            return;
                        }

                        handleFinalFrame(result);

                        // it's possible for this fragment to go away while analyzer finishes processing a frame
                        if (null != cameraMgr) {
                            cameraMgr.receivedGoodFrame();
                        }
                    }
                });
                controller.start();

                ((ViewGroup)getView()).addView(cameraMgr.getSurfaceView());

            } else {
                handleErrorState(RESULT_ERROR_SDK_STATE_ERROR);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            handleErrorState(RESULT_ERROR_SDK_STATE_ERROR);
        }
    }

    @Override
    public void onEvent(SetCaptureModeEvent event) {
        if (CameraApi.PARAMETER_CAPTURE_MODE_MANUAL == event.mode
                && camParamsMgr.isCurrentModeVideo()) {
            miSnapAnalyzer.deinit();
//            barcodeAnalyzer.deinit();
        } else if (CameraApi.PARAMETER_CAPTURE_MODE_AUTO == event.mode
                && !camParamsMgr.isCurrentModeVideo()) {
            miSnapAnalyzer.init();
//            barcodeAnalyzer.init();
        }

        super.onEvent(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (barcodeAnalyzer != null) {
            barcodeAnalyzer.updateOrientation(newConfig.orientation, getBarcodeDocumentOrientation());
        }
    }

    protected int cameraRotationToNativeOrientation(int orientation) {
        int result;
        switch (orientation){
            case 90:
                result =  MiSnapScience.Orientation.PORTRAIT;
                break;
            case 0:
                result = MiSnapScience.Orientation.LANDSCAPE_LEFT;
                break;
            case 270:
                result = MiSnapScience.Orientation.REVERSE_PORTRAIT;
                break;
            case 180:
                result = MiSnapScience.Orientation.LANDSCAPE_RIGHT;
                break;
            default:
                result = MiSnapScience.Orientation.LANDSCAPE_LEFT;
                break;
        }
        return result;
    }

    protected int deviceOrientationToNativeOrientation(int orientation) {
        int result;
        switch (orientation) {
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                result = MiSnapScience.Orientation.LANDSCAPE_LEFT;
                break;
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                result = MiSnapScience.Orientation.PORTRAIT;
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                result = MiSnapScience.Orientation.LANDSCAPE_RIGHT;
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                result = MiSnapScience.Orientation.REVERSE_PORTRAIT;
                break;
            default:
                result = MiSnapScience.Orientation.LANDSCAPE_LEFT;
                break;
        }
        return result;
    }

    private void handleFinalFrame(MiSnapHybridControllerResult result) {
        String resultCode;
        if (camParamsMgr.isCurrentModeVideo()) {
            resultCode = MiSnapApi.RESULT_SUCCESS_VIDEO;
            mWarnings.clear();
        } else {
            resultCode = MiSnapApi.RESULT_SUCCESS_STILL;
        }


        Intent returnIntent = buildReturnIntent(Activity.RESULT_OK, result.getFinalFrame(), resultCode, result.getFourCorners());
        if (!result.getExtractedBarcode().isEmpty()) {
            returnIntent.putExtra(BarcodeApi.RESULT_PDF417_DATA, result.getExtractedBarcode());
        }
        EventBus.getDefault().post(new OnCapturedFrameEvent(returnIntent));
    }

    private void setOrientationListener(Context context) {
        orientationEventListener = new OrientationEventListener(
                context, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int angle) {
                int deviceOrientation = deviceOrientationToNativeOrientation(Utils.getDeviceOrientation(getActivity()));

                if (deviceOrientation != lastOrientation && camParamsMgr != null) {
                    lastOrientation = deviceOrientation;
                    if (miSnapAnalyzer != null) {
                        miSnapAnalyzer.setOrientation(getDlDocumentOrientation(), getDeviceOrientation());
                    }
                }
            }
        };
        orientationEventListener.enable();
    }

    private void stopOrientationListener() {
        if (null != orientationEventListener) {
            orientationEventListener.disable();
            orientationEventListener = null;
        }
    }

    private int getDlDocumentOrientation() {

        int orientation = cameraRotationToNativeOrientation(Utils.getCameraRotationDegrees(getActivity()));
        if (shouldRotateOrientation90()) {
            orientation = (orientation + 1) % MISNAP_ORIENTATION_COUNT;  //rotate 90 degrees
        }
        return orientation;
    }

    private int getBarcodeDocumentOrientation() {
        int result = Configuration.ORIENTATION_LANDSCAPE;
        if (Utils.getDeviceBasicOrientation(getActivity().getApplicationContext()) == Configuration.ORIENTATION_PORTRAIT) {
            if (camParamsMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_FREE_DOCUMENT_ALIGNED_WITH_DEVICE ||
                    camParamsMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_PORTRAIT_DOCUMENT_PORTRAIT) {
                result = Configuration.ORIENTATION_PORTRAIT;
            }
        }
        return result;
    }

    private int getDeviceOrientation() {
        return cameraRotationToNativeOrientation(Utils.getCameraRotationDegrees(getActivity()));
    }

    private boolean shouldRotateOrientation90() {
        CameraParamMgr cameraParamMgr = new CameraParamMgr(miSnapParams);
        return ((cameraParamMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_PORTRAIT_DOCUMENT_PORTRAIT
                || cameraParamMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_FREE_DOCUMENT_ALIGNED_WITH_DEVICE)
                && Utils.getDeviceBasicOrientation(getActivity().getApplicationContext()) == Configuration.ORIENTATION_PORTRAIT);
    }
}
