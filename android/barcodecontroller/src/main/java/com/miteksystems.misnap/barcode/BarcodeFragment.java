package com.miteksystems.misnap.barcode;

import androidx.lifecycle.Observer;
import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.ViewGroup;

import com.miteksystems.misnap.ControllerFragment;
import com.miteksystems.misnap.barcode.analyzer.BarcodeAnalyzer;
import com.miteksystems.misnap.barcode.events.BarcodeAnalyzerResult;
import com.miteksystems.misnap.camera.MiSnapCamera;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.utils.Utils;

import static com.miteksystems.misnap.params.MiSnapApi.RESULT_ERROR_SDK_STATE_ERROR;


/**
 * Created by awood on 6/7/16.
 */
public class BarcodeFragment extends ControllerFragment {

    private static final String TAG = BarcodeFragment.class.getName();
    private BarcodeAnalyzer analyzer;
    private BarcodeController barcodeController;

    @Override
    protected void deinit() {
        super.deinit();
        if (analyzer != null) {
            analyzer.deinit();
            //no longer nulling the analyzer to avoid crash where onConfigurationChanged() gets called in a different thread than deinit()
//            analyzer = null;
        }

        if (barcodeController != null) {
            barcodeController.end();
            barcodeController = null;
        }
    }

    @Override
    public void initializeController() {
        try {
            //set the surfaceview in the main activity
            MiSnapCamera camera = cameraMgr.getMiSnapCamera();
            camera.setScaleX(1.2f);
            camera.setScaleY(1.2f);
            if (camera != null) {
                analyzer = new BarcodeAnalyzer(getActivity(), miSnapParams, getActivity().getResources().getConfiguration().orientation, getDocumentOrientation());
                analyzer.init();
                barcodeController = new BarcodeController(camera, analyzer, miSnapParams);
                barcodeController.getLiveDataFinalFrame().observe(this, new Observer<byte[]>() {
                    @Override
                    public void onChanged(@Nullable byte[] bytes) {
                        processFinalFrameMessage(bytes, null);
                    }
                });
                barcodeController.getLiveDataBarcodeAnalyzerResult().observe(this, new Observer<BarcodeAnalyzerResult>() {
                    @Override
                    public void onChanged(@Nullable BarcodeAnalyzerResult barcodeAnalyzerResult) {
                        cameraMgr.receivedGoodFrame();
                    }
                });
                barcodeController.start();

                ((ViewGroup)getView()).addView(cameraMgr.getSurfaceView());

            } else {
                //send a message to MiSnap
                handleErrorState(RESULT_ERROR_SDK_STATE_ERROR);
            }
        } catch (Exception e) {
            //send a message to MiSnap
            Log.e(TAG, e.toString());
            handleErrorState(RESULT_ERROR_SDK_STATE_ERROR);
        }
    }

    @Override
    protected String buildMibiData(Context context, String resultCode) {
        return super.buildMibiData(context, resultCode);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (analyzer != null) {
            analyzer.updateOrientation(newConfig.orientation, getDocumentOrientation());
        }
    }

    private int getDocumentOrientation() {
        int result = Configuration.ORIENTATION_LANDSCAPE;
        if (Utils.getDeviceBasicOrientation(getActivity().getApplicationContext()) == Configuration.ORIENTATION_PORTRAIT) {
            if (camParamsMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_FREE_DOCUMENT_ALIGNED_WITH_DEVICE ||
                    camParamsMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_PORTRAIT_DOCUMENT_PORTRAIT) {
                result = Configuration.ORIENTATION_PORTRAIT;
            }
        }
        return result;
    }
}
