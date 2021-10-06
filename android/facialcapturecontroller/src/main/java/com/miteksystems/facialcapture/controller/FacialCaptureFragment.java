package com.miteksystems.facialcapture.controller;

import android.app.Activity;
import androidx.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import android.util.Log;
import android.view.ViewGroup;

import com.miteksystems.facialcapture.science.analyzer.FacialCaptureAnalyzer;
import com.miteksystems.facialcapture.science.analyzer.FacialCaptureUxp;
import com.miteksystems.facialcapture.science.api.params.FacialCaptureApi;
import com.miteksystems.facialcapture.science.api.params.FacialCaptureParamMgr;
import com.miteksystems.misnap.ControllerFragment;
import com.miteksystems.misnap.camera.MiSnapCamera;
import com.miteksystems.misnap.events.OnCapturedFrameEvent;
import com.miteksystems.misnap.mibidata.MibiData;
import com.miteksystems.misnap.mibidata.MibiDataException;
import com.miteksystems.misnap.params.BaseParamMgr;
import com.miteksystems.misnap.params.CameraApi;
import com.miteksystems.misnap.params.MiSnapApi;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by awood on 3/28/16.
 */
public class FacialCaptureFragment extends ControllerFragment {

    private static final String TAG = FacialCaptureFragment.class.getName();
    private static final int BAD_DAON_FRAMES = 0; // Daon results used to be unreliable for the first 6 frames or so
    FacialCaptureParamMgr mFacialCaptureParams; // TODO KW 2017-11-28:  as of today, controller/workflow and science have the same param mgr.  they shouldn't, though.
    private FacialCaptureAnalyzer analyzer;
    private FacialCaptureController controller;
    private boolean spoofWasDetected;

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);

        // Using the B-02178-FrontCamera_retry branch requires us to set the job type to Face Detection
        // in order to request the front-facing camera.
        if (null != getActivity()) {
            try {
                Intent intent = getActivity().getIntent();
                JSONObject jobJson = new JSONObject(intent.getStringExtra(MiSnapApi.JOB_SETTINGS));
                jobJson.put(MiSnapApi.MiSnapDocumentType, MiSnapApi.PARAMETER_DOCTYPE_CAMERA_ONLY);
                jobJson.put(CameraApi.MiSnapUseFrontCamera, 1);
                jobJson.put(MiSnapApi.MiSnapOrientation, MiSnapApi.PARAMETER_ORIENTATION_DEVICE_PORTRAIT_DOCUMENT_PORTRAIT);
                jobJson.put(CameraApi.MiSnapTorchMode, 0);

                mFacialCaptureParams = new FacialCaptureParamMgr(jobJson);

                intent.removeExtra(MiSnapApi.JOB_SETTINGS);
                intent.putExtra(MiSnapApi.JOB_SETTINGS, jobJson.toString());
                getActivity().setIntent(intent);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    @Override
    protected void deinit() {
        super.deinit();

        if (null != analyzer) {
            analyzer.deinit();
            analyzer = null;
        }

        if (null != controller) {
            controller.end();
            controller = null;
        }
    }

    @Override
    public void initializeController() {
        MiSnapCamera camera = cameraMgr.getMiSnapCamera();
        if (null != camera && null != getActivity()) {
            JSONObject params;
            try {
                Intent intent = getActivity().getIntent();
                params = new JSONObject(intent.getStringExtra(MiSnapApi.JOB_SETTINGS));
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                handleErrorState(MiSnapApi.RESULT_ERROR_INTENT_PARAMETERS);
                return;
            }
            analyzer = new FacialCaptureAnalyzer(getActivity().getApplicationContext(), params);
            analyzer.setNumFramesToIgnore(BAD_DAON_FRAMES);
            analyzer.init();

            controller = new FacialCaptureController(camera, analyzer, params);
            controller.getLiveDataControllerResult().observe(this, new Observer<Pair<byte[], String>>() {
                @Override
                public void onChanged(@Nullable Pair<byte[], String> controllerResult) {
                    if (null != controllerResult)
                        processFacialCaptureFinalFrameMessage(controllerResult.first, controllerResult.second);
                }
            });
            controller.start();

            ViewGroup rootView = (ViewGroup) getView();
            if (null != rootView) {
                rootView.addView(cameraMgr.getSurfaceView());
            } else {
                Log.e(TAG, "Root View is null, not adding Camera SurfaceView");
            }
        } else {
            handleErrorState(MiSnapApi.RESULT_ERROR_SDK_STATE_ERROR);
        }

        try {
            //prep mibi data to make sure that we can access the full mibi string
            MibiData.getInstance().setDocument(mFacialCaptureParams.getRawDocumentType())
                    .setAutocapture(camParamsMgr.isCurrentModeVideo() ? "1" : "0")
                    .setMiSnapVersion(getString(R.string.misnap_versionName));

            //use mibi singelton to track spoofs across multiple capture/retry sessions
            if (FacialCaptureUxp.spoofWasDetected(MibiData.getInstance().getMibiData())) {
                spoofWasDetected = true;
            }
        } catch (JSONException | MibiDataException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String buildMibiData(Context context, String resultCode) {
        String mibi = super.buildMibiData(context, resultCode); // Get all the underlying MiSnap MIBI data

        MibiData mibiBuilder = MibiData.getInstance();
        try {
            mibiBuilder.setSDKVersion(getString(R.string.sdk_versionName));
            mibiBuilder.removeParameter(FacialCaptureApi.FacialCaptureLicenseKey);
            mibiBuilder.addChangedParameters(BaseParamMgr.getChangedValues());
        } catch (JSONException e) {
            Log.e(TAG, "Unable to add MIBI data");
        }

        try {
            mibi = mibiBuilder.getMibiData();
        } catch (MibiDataException e) {
            e.printStackTrace();
        }

        return mibi;
    }

    private void processFacialCaptureFinalFrameMessage(final byte[] bFrameData, String resultCode) {
        if (camParamsMgr.isCurrentModeVideo()) {
            mWarnings.clear();
        }

        Intent returnIntent = buildReturnIntent(Activity.RESULT_OK, bFrameData, spoofWasDetected ? FacialCaptureApi.RESULT_SPOOF_DETECTED : resultCode, null); // null because no 4c
        EventBus.getDefault().post(new OnCapturedFrameEvent(returnIntent));
    }
}
