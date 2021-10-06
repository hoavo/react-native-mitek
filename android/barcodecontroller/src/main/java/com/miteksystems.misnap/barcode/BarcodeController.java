package com.miteksystems.misnap.barcode;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.miteksystems.imaging.JPEGProcessor;
import com.miteksystems.misnap.ICamera;
import com.miteksystems.misnap.IFrameHandler;
import com.miteksystems.misnap.analyzer.IAnalyzeResponse;
import com.miteksystems.misnap.barcode.analyzer.BarcodeAnalyzer;
import com.miteksystems.misnap.barcode.events.BarcodeAnalyzerResult;
import com.miteksystems.misnap.barcode.events.OnCapturedBarcodeEvent;
import com.miteksystems.misnap.params.BarcodeApi;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class BarcodeController implements IFrameHandler {

    private ICamera camera;
    private BarcodeAnalyzer analyzer;
    private ExecutorService executorService;
    private CameraParamMgr cameraParamMgr;
    private AtomicBoolean analyzingInProgress = new AtomicBoolean();
    private MutableLiveData<BarcodeAnalyzerResult> liveDataMiSnapAnalyzerResult = new MutableLiveData<>();
    private MutableLiveData<byte[]> liveDataFinalFrame = new MutableLiveData<>();

    public BarcodeController(@NonNull ICamera camera, @NonNull BarcodeAnalyzer analyzer, JSONObject miSnapSettings) {
        this.camera = camera;
        this.analyzer = analyzer;
        cameraParamMgr = new CameraParamMgr(miSnapSettings);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @VisibleForTesting
    public BarcodeController(@NonNull ICamera camera, @NonNull BarcodeAnalyzer analyzer, JSONObject miSnapSettings, @NonNull ExecutorService executorService) {
        this(camera, analyzer, miSnapSettings);
        this.executorService = executorService;
    }

    public void start() {
        camera.addFrameHandler(this);
    }

    public void end() {
        if (analyzer != null) {
            analyzer.deinit();
        }
    }

    @Override
    public void handlePreviewFrame(final byte[] frame, final int width, final int height, final int colorSpace, final int deviceOrientation, final int cameraRotationDegrees) {
        if (analyzingInProgress.get()) {
            return;
        }

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    analyzingInProgress.set(true);
                    BarcodeAnalyzerResult miSnapAnalyzerResult = analyzer.analyze(frame, width, height);
                    handleResults(miSnapAnalyzerResult, width, height, deviceOrientation, frame, cameraRotationDegrees, cameraParamMgr.getImageQuality(), cameraParamMgr.getImageDimensionMax(), 1 == cameraParamMgr.getUseFrontCamera());
                } finally {
                    analyzingInProgress.set(false);
                }
            }
        });
    }

    @Override
    public void handleManuallyCapturedFrame(byte[] jpegImage, int width, int height, int deviceOrientation, int cameraRotationDegrees) {
        //todo barcode has no manual option, right?
        throw new UnsupportedOperationException("BarcodeAnalyzer doesn't support onManualPictureTaken()");
    }


    private void handleResults(BarcodeAnalyzerResult barcodeAnalyzerResult,
                               int width,
                               int height,
                               int deviceOrientation,
                               byte[] frameBytes,
                               int cameraRotationDegrees,
                               int imageQuality,
                               int imageDimensionMax,
                               boolean useFrontCamera) {

        if (barcodeAnalyzerResult.getRunError() == IAnalyzeResponse.ANALYZER_FRAME_IS_GOOD) {
            Intent intent = new Intent();
            intent.putExtra(BarcodeApi.RESULT_PDF417_DATA, barcodeAnalyzerResult.getExtractedBarcode());
            intent.putExtra(MiSnapApi.RESULT_CODE, barcodeAnalyzerResult.getResultCode());

            // TODO KW:  workflow needs to know about the barcode event, but that's cuz it's not handling the return from analyzer.  can it handle it, though?
            EventBus.getDefault().post(new OnCapturedBarcodeEvent(intent)); // Tell the calling application to shut us down
        }

        // post to workflow
        EventBus.getDefault().post(barcodeAnalyzerResult); // TODO KW:  this is all temporary.  enables workflow to handle hint bubbles.

        if (barcodeAnalyzerResult.getResultCode().equals(MiSnapApi.RESULT_SUCCESS_PDF417)) {
            // add mibi/uxp data
            //TODO do we want this for barcode?
//            logMibiAndUxp(isManualCapture, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE == deviceOrientation);

            // deinit analyzer; camera is stopped by the manager, for better or worse, via CameraManager.receivedGoodFrame()
            analyzer.deinit();

            // call MiSnapFragment.processGoodFrameMessage()
            // call CameraManager.receivedGoodFrame()
            liveDataMiSnapAnalyzerResult.postValue(barcodeAnalyzerResult);

            // transform frame and save as jpeg
            boolean usePortraitOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT == deviceOrientation || ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT == deviceOrientation;
            int rotationAdjustment = JPEGProcessor.getRotationAngle(Utils.needToRotateFrameBy180Degrees(cameraRotationDegrees), usePortraitOrientation, useFrontCamera);

            byte[] finalFrame = JPEGProcessor.getFinalJpegFromPreviewFrame(frameBytes,
                    width,
                    height,
                    imageQuality,
                    imageDimensionMax,
                    rotationAdjustment);


            // call MiSnapFragment.processFinalFrameMessage()
            liveDataFinalFrame.postValue(finalFrame);

            // all done, no need to handle further requests
            executorService.shutdownNow();
        }
    }

    LiveData<BarcodeAnalyzerResult> getLiveDataBarcodeAnalyzerResult() {
        return liveDataMiSnapAnalyzerResult;
    }

    LiveData<byte[]> getLiveDataFinalFrame() {
        return liveDataFinalFrame;
    }
}
