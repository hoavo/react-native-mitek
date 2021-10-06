package com.miteksystems.misnapcontroller;

import androidx.lifecycle.Observer;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.ViewGroup;

import com.miteksystems.misnap.ControllerFragment;
import com.miteksystems.misnap.analyzer.AnalyzerFactory;
import com.miteksystems.misnap.analyzer.MiSnapAnalyzer;
import com.miteksystems.misnap.camera.MiSnapCamera;
import com.miteksystems.misnap.events.SetCaptureModeEvent;
import com.miteksystems.misnap.natives.MiSnapScience;
import com.miteksystems.misnap.params.CameraApi;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.DocType;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.params.ScienceParamMgr;
import com.miteksystems.misnap.utils.Utils;

import org.json.JSONObject;

import static com.miteksystems.misnap.params.MiSnapApi.RESULT_ERROR_SDK_STATE_ERROR;

/**
 * Created by jlynch on 11/21/17.
 */

public class MiSnapFragment extends ControllerFragment {

    private static final String TAG = MiSnapFragment.class.getName();
    private static final int MISNAP_ORIENTATION_COUNT = 4;
    private MiSnapController miSnapController;
    private MiSnapAnalyzer analyzer;
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
        if (miSnapController != null) {
            miSnapController.end();
            miSnapController = null;
        }

        if (analyzer != null) {
            analyzer.deinit();
            //no longer null because onOrientationChanged() could be called in a different thread than deinit()
//            analyzer = null;
        }

        stopOrientationListener();
    }

    private MiSnapAnalyzer createAnalyzer(JSONObject parameters) {
        int whichAnalyzer;
        ScienceParamMgr paramMgr = new ScienceParamMgr(parameters);
        if (paramMgr.isTestScienceCaptureMode()) {
            // Only used for building a test deck
            Log.e("Analyzer", "Creating TEST_SCIENCE_CAPTURE_ANALYZER");
            whichAnalyzer = AnalyzerFactory.TEST_SCIENCE_CAPTURE_ANALYZER;
        } else if (paramMgr.isTestScienceReplayMode()) {
            // Only used for analyzing a test deck
            Log.e("Analyzer", "Creating TEST_SCIENCE_REPLAY_ANALYZER");
            whichAnalyzer = AnalyzerFactory.TEST_SCIENCE_REPLAY_ANALYZER;
        } else if (new DocType(paramMgr.getRawDocumentType()).isCameraOnly()) {
            whichAnalyzer = AnalyzerFactory.NO_ANALYZER;
        } else {
            // Video capture mode
            Log.e("Analyzer", "Creating MISNAP_ANALYZER");
            whichAnalyzer = AnalyzerFactory.MISNAP_ANALYZER;
        }

        return AnalyzerFactory.createAnalyzer(whichAnalyzer, getActivity(), getDocumentOrientation(), getDeviceOrientation(), parameters);
    }

    @Override
    public void initializeController() {
        try {
            //set the surfaceview in the main activity
            MiSnapCamera camera = cameraMgr.getMiSnapCamera();
            camera.setScaleX(1.2f);
            camera.setScaleY(1.2f);

            if (camera != null) {
                analyzer = createAnalyzer(miSnapParams);
                analyzer.init();
                miSnapController = new MiSnapController(camera, analyzer, miSnapParams);

                // this is needed so that the frame can be returned in return intent
                miSnapController.getLiveDataResult().observe(this, new Observer<MiSnapControllerResult>() {
                    @Override
                    public void onChanged(@Nullable MiSnapControllerResult result) {
                        if (null == result) {
                            Log.w(TAG, "empty result");
                            return;
                        }

                        processFinalFrameMessage(result.getFinalFrame(), result.getFourCorners());

                        // it's possible for this fragment to go away while analyzer finishes processing a frame
                        if (null != cameraMgr) {
                            cameraMgr.receivedGoodFrame();
                        }
                    }
                });
                miSnapController.start();

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
            analyzer.deinit();
        } else if (CameraApi.PARAMETER_CAPTURE_MODE_AUTO == event.mode
                && !camParamsMgr.isCurrentModeVideo()) {
            analyzer.init();
        }

        super.onEvent(event);
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

    private void setOrientationListener(Context context) {
        orientationEventListener = new OrientationEventListener(
                context, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int angle) {
                int deviceOrientation = deviceOrientationToNativeOrientation(Utils.getDeviceOrientation(getActivity()));

                if (deviceOrientation != lastOrientation && camParamsMgr != null && analyzer != null) {
                    lastOrientation = deviceOrientation;
                    analyzer.setOrientation(getDocumentOrientation(), getDeviceOrientation());
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

    private int getDocumentOrientation() {

        int orientation = cameraRotationToNativeOrientation(Utils.getCameraRotationDegrees(getActivity()));
        if (shouldRotateOrientation90()) {
            orientation = (orientation + 1) % MISNAP_ORIENTATION_COUNT;  //rotate 90 degrees
        }
        return orientation;
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
