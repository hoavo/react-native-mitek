package com.miteksystems.misnaphybridcontroller;

import androidx.lifecycle.MutableLiveData;
import android.content.pm.ActivityInfo;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.util.Log;

import com.miteksystems.imaging.JPEGProcessor;
import com.miteksystems.misnap.ICamera;
import com.miteksystems.misnap.IFrameHandler;
import com.miteksystems.misnap.analyzer.IAnalyzeResponse;
import com.miteksystems.misnap.analyzer.MiSnapAnalyzer;
import com.miteksystems.misnap.analyzer.MiSnapAnalyzerResult;
import com.miteksystems.misnap.analyzer.MiSnapAnalyzerResultsProcessor;
import com.miteksystems.misnap.barcode.analyzer.BarcodeAnalyzer;
import com.miteksystems.misnap.barcode.analyzer.MiSnapBarcodeDetector;
import com.miteksystems.misnap.barcode.events.BarcodeAnalyzerResult;
import com.miteksystems.misnap.mibidata.MibiData;
import com.miteksystems.misnap.params.BarcodeUxpConstants;
import com.miteksystems.misnap.params.UxpConstants;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MiSnapHybridController implements IFrameHandler {

    private static final String TAG = MiSnapHybridController.class.getName();
    private ICamera camera;
    private MiSnapAnalyzer misnapAnalyzer;
    private BarcodeAnalyzer barcodeAnalyzer;
    private MiSnapBarcodeDetector miSnapBarcodeDetector;
    private CameraParamMgr cameraParamMgr;
    private ExecutorService executorService;
    private AtomicBoolean analyzingInProgress = new AtomicBoolean();
    private static long DELAY_BEFORE_ALLOWING_SNAP_IN_MS = 1000;
    private long lastGoodSnapTimeInMs;
    private String lastBarcodeValue = "";
    private MutableLiveData<MiSnapHybridControllerResult> result;

    public MiSnapHybridController(@NonNull ICamera camera, @NonNull MiSnapAnalyzer misnapAnalyzer, @NonNull BarcodeAnalyzer barcodeAnalyzer, @NonNull MiSnapBarcodeDetector miSnapBarcodeDetector, JSONObject miSnapSettings) {
        this.camera = camera;
        this.misnapAnalyzer = misnapAnalyzer;
        this.barcodeAnalyzer = barcodeAnalyzer;
        this.miSnapBarcodeDetector = miSnapBarcodeDetector;
        cameraParamMgr = new CameraParamMgr(miSnapSettings);
        executorService = Executors.newSingleThreadExecutor();
        result = new MutableLiveData<>();
    }

    @VisibleForTesting
    public MiSnapHybridController(@NonNull ICamera camera, @NonNull MiSnapAnalyzer misnapAnalyzer, @NonNull BarcodeAnalyzer barcodeAnalyzer, @NonNull MiSnapBarcodeDetector miSnapBarcodeDetector, JSONObject miSnapSettings, ExecutorService testExecutor) {
        this(camera, misnapAnalyzer, barcodeAnalyzer, miSnapBarcodeDetector, miSnapSettings);
        executorService = testExecutor;
    }

    public void start() {
        camera.addFrameHandler(this);
    }

    public void end() {
        if (misnapAnalyzer != null) {
            misnapAnalyzer.deinit();
        }
        if (barcodeAnalyzer != null) {
            barcodeAnalyzer.deinit();
        }
        if (miSnapBarcodeDetector != null) {
            miSnapBarcodeDetector.deinit();
        }

        lastBarcodeValue = "";
    }

    @Override
    public void handlePreviewFrame(final byte[] frame, final int width, final int height, final int colorSpace, final int deviceOrientation, final int cameraRotationDegrees) {
        if (!analyzingInProgress.get()) {
            try {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            analyzingInProgress.set(true);
                            //run barcode detector first if barcode is not yet processed
                            if (lastBarcodeValue.isEmpty() && miSnapBarcodeDetector.detectBarcode(frame, width, height, colorSpace)) {
                                BarcodeAnalyzerResult barcodeAnalyzerResult = barcodeAnalyzer.analyze(frame, width, height);
                                if (barcodeAnalyzerResult.getResultCode() != null && barcodeAnalyzerResult.getResultCode().equals(MiSnapApi.RESULT_SUCCESS_PDF417)) {
                                    lastBarcodeValue = barcodeAnalyzerResult.getExtractedBarcode();
                                    MibiData.getInstance().addUXPEvent(BarcodeUxpConstants.MISNAP_UXP_BARCODE_DETECTED);
                                }
                            }

                            // when misnap analyzer is not initialized, the analyze call will return with error code, so we just skip the rest
                            // TODO KW 2018-06-13:  it seems weird that analyzer doesn't get passed any orientation info here.  it implies that
                            // it's getting it from somewhere else.  in this case, analyzer gets it in its ctor, but the ctor isn't called in this
                            // class, so we're forced to assume that wherever it's getting that info from is the same as wherever misnapcamera gets it.
                            MiSnapAnalyzerResult miSnapAnalyzerResult = misnapAnalyzer.analyze(frame, width, height, colorSpace);
                            if (miSnapAnalyzerResult.analyzeSucceeded()) {
                                // We need to delay a bit to avoid blurry images on high-end devices
                                if (lastGoodSnapTimeInMs == 0 && DELAY_BEFORE_ALLOWING_SNAP_IN_MS > 0) {
                                    lastGoodSnapTimeInMs = System.currentTimeMillis();
                                    miSnapAnalyzerResult.setErrorCode(IAnalyzeResponse.ANALYZER_FRAME_IS_GOOD_BUT_WE_MUST_WAIT);
                                } else if (System.currentTimeMillis() - lastGoodSnapTimeInMs < DELAY_BEFORE_ALLOWING_SNAP_IN_MS) {
                                    miSnapAnalyzerResult.setErrorCode(IAnalyzeResponse.ANALYZER_FRAME_IS_GOOD_BUT_WE_MUST_WAIT);
                                }
                            }

                            // misnap analyzer is active, send its feedback to UI
                            if (miSnapAnalyzerResult.getErrorCode() != IAnalyzeResponse.ANALYZER_IS_NOT_INITIALIZED) {
                                sendResultToUi(miSnapAnalyzerResult, width, height, deviceOrientation, cameraRotationDegrees, 1 == cameraParamMgr.getUseFrontCamera());
                            }
                            if (miSnapAnalyzerResult.analyzeSucceeded()) {
                                handleResults(false, miSnapAnalyzerResult, width, height, deviceOrientation, frame, cameraRotationDegrees, cameraParamMgr.getImageQuality(), cameraParamMgr.getImageDimensionMax(), 1 == cameraParamMgr.getUseFrontCamera(), lastBarcodeValue);
                            }
                        } finally {
                            analyzingInProgress.set(false);
                        }
                    }
                });
            } catch (RejectedExecutionException e) {
                Log.d(TAG, e.toString());
            }
        }
    }

    @Override
    public void handleManuallyCapturedFrame(final byte[] jpegImage, final int width, final int height, final int deviceOrientation, final int cameraRotationDegrees) {

        try {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    MiSnapAnalyzerResult miSnapAnalyzerResult = misnapAnalyzer.onManualPictureTaken(jpegImage);
                    handleResults(true, miSnapAnalyzerResult, width, height, deviceOrientation, jpegImage, cameraRotationDegrees, cameraParamMgr.getImageQuality(), cameraParamMgr.getImageDimensionMax(), 1 == cameraParamMgr.getUseFrontCamera(), lastBarcodeValue);
                }
            });
        } catch (RejectedExecutionException e) {
            Log.d(TAG, e.toString());
        }

    }

    public MutableLiveData<MiSnapHybridControllerResult> getResult() {
        return result;
    }

    private void sendResultToUi(MiSnapAnalyzerResult miSnapAnalyzerResult,
                                int width,
                                int height,
                                int deviceOrientation,
                                int cameraRotationDegrees,
                                boolean useFrontCamera) {
        boolean usePortraitOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT == deviceOrientation || ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT == deviceOrientation;
        int rotationAdjustment = JPEGProcessor.getRotationAngle(Utils.needToRotateFrameBy180Degrees(cameraRotationDegrees), usePortraitOrientation, useFrontCamera);
        MiSnapAnalyzerResultsProcessor.updateCorners(miSnapAnalyzerResult,
                rotationAdjustment,
                width,
                height,
                useFrontCamera);
        EventBus.getDefault().post(miSnapAnalyzerResult); // TODO KW:  this is all temporary.  enables workflow to handle hint bubbles.
    }

    private void handleResults(boolean isManualCapture,
                               MiSnapAnalyzerResult miSnapAnalyzerResult,
                               int width,
                               int height,
                               int deviceOrientation,
                               byte[] frameBytes,
                               int cameraRotationDegrees,
                               int imageQuality,
                               int imageDimensionMax,
                               boolean useFrontCamera,
                               String barcode) {
        // update corners and post to workflow
        boolean usePortraitOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT == deviceOrientation || ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT == deviceOrientation;
        int rotationAdjustment = JPEGProcessor.getRotationAngle(Utils.needToRotateFrameBy180Degrees(cameraRotationDegrees), usePortraitOrientation, useFrontCamera);

        // add mibi/uxp data
        logMibiAndUxp(isManualCapture, deviceOrientation);

        // deinit analyzer; camera is stopped by the manager, for better or worse, via CameraManager.receivedGoodFrame()
        executorService.shutdown();
        misnapAnalyzer.deinit();
        barcodeAnalyzer.deinit();
        miSnapBarcodeDetector.deinit();

        // transform frame and save as jpeg
        byte[] finalFrame;
        if (isManualCapture) {
            EventBus.getDefault().post(miSnapAnalyzerResult); //needed to pass the final warnings to the ControllerFragment
            finalFrame = JPEGProcessor.getFinalJpegFromManuallyCapturedFrame(frameBytes,
                    imageQuality,
                    imageDimensionMax,
                    rotationAdjustment);
        } else {
            finalFrame = JPEGProcessor.getFinalJpegFromPreviewFrame(frameBytes,
                    width,
                    height,
                    imageQuality,
                    imageDimensionMax,
                    rotationAdjustment);
        }

        // call CameraManager.receivedGoodFrame()
        // call MiSnapFragment.processFinalFrameMessage()
        result.postValue(new MiSnapHybridControllerResult(finalFrame, miSnapAnalyzerResult.getFourCorners(), barcode));
    }

    private void logTheDeviceOrientation(int deviceOrientation) {
        String uxpDeviceOrientation;

        switch (deviceOrientation) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                uxpDeviceOrientation = UxpConstants.MISNAP_UXP_PORTRAIT_UP;
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                uxpDeviceOrientation = UxpConstants.MISNAP_UXP_PORTRAIT_DOWN;
                break;
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                uxpDeviceOrientation = UxpConstants.MISNAP_UXP_DEVICE_LANDSCAPE_LEFT;
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                uxpDeviceOrientation = UxpConstants.MISNAP_UXP_DEVICE_LANDSCAPE_RIGHT;
                break;
            default:
                uxpDeviceOrientation = UxpConstants.MISNAP_UXP_DEVICE_LANDSCAPE_LEFT;
        }

        MibiData.getInstance().addUXPEvent(uxpDeviceOrientation);
    }

    private void logMibiAndUxp(boolean isManualCapture, int deviceOrientation) {
        if (!isManualCapture) {
            MibiData.getInstance().addUXPEvent(UxpConstants.MISNAP_UXP_CAPTURE_TIME);
        } else {
            MibiData.getInstance().addUXPEvent(UxpConstants.MISNAP_UXP_CAPTURE_MANUAL);
        }

        logTheDeviceOrientation(deviceOrientation);
    }
}
