package com.miteksystems.misnapcontroller;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.content.pm.ActivityInfo;
import androidx.annotation.NonNull;
import android.util.Log;

import com.miteksystems.imaging.JPEGProcessor;
import com.miteksystems.misnap.ICamera;
import com.miteksystems.misnap.IFrameHandler;
import com.miteksystems.misnap.analyzer.IAnalyzeResponse;
import com.miteksystems.misnap.analyzer.MiSnapAnalyzer;
import com.miteksystems.misnap.analyzer.MiSnapAnalyzerResult;
import com.miteksystems.misnap.analyzer.MiSnapAnalyzerResultsProcessor;
import com.miteksystems.misnap.mibidata.MibiData;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.UxpConstants;
import com.miteksystems.misnap.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by kwu on 3/13/18.
 *
 * This class hooks up a camera and a science.  It processes results from science.  Then it gives results back to whomever is using this class.
 */

class MiSnapController implements IFrameHandler {
    private ICamera camera;
    private MiSnapAnalyzer analyzer;
    private ExecutorService executorService;
    private CameraParamMgr cameraParamMgr;
    private AtomicBoolean analyzingInProgress = new AtomicBoolean();
    static long DELAY_BEFORE_ALLOWING_SNAP_IN_MS = 1000;
    private long lastGoodSnapTimeInMs;

    private static final String TAG = MiSnapController.class.getName();
    private MutableLiveData<MiSnapControllerResult> liveDataResult = new MutableLiveData<>();

    public MiSnapController(@NonNull ICamera camera, @NonNull MiSnapAnalyzer analyzer, JSONObject miSnapSettings) {
        this.camera = camera;
        this.analyzer = analyzer;
        cameraParamMgr = new CameraParamMgr(miSnapSettings);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public MiSnapController(@NonNull ICamera camera, @NonNull MiSnapAnalyzer analyzer, JSONObject miSnapSettings, @NonNull ExecutorService executorService) {
        this(camera, analyzer, miSnapSettings);
        this.executorService = executorService;
    }

    public void start() {
        camera.addFrameHandler(this);
    }

    public void end() {
        if (analyzer != null){
            analyzer.deinit();
        }
        executorService.shutdownNow();
    }

    @Override
    public void handlePreviewFrame(final byte[] frame, final int width, final int height, final int colorSpace, final int deviceOrientation, final int cameraRotationDegrees) {
        if (analyzingInProgress.get()) {
            return;
        }

        try {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        analyzingInProgress.set(true);
                        // TODO KW 2018-06-13:  it seems weird that analyzer doesn't get passed any orientation info here.  it implies that
                        // it's getting it from somewhere else.  in this case, analyzer gets it in its ctor, but the ctor isn't called in this
                        // class, so we're forced to assume that wherever it's getting that info from is the same as wherever misnapcamera gets it.
                        MiSnapAnalyzerResult miSnapAnalyzerResult = analyzer.analyze(frame, width, height, colorSpace);
                        if (miSnapAnalyzerResult.analyzeSucceeded()) {
                            // We need to delay a bit to avoid blurry images on high-end devices
                            if (lastGoodSnapTimeInMs == 0 && DELAY_BEFORE_ALLOWING_SNAP_IN_MS > 0) {
                                lastGoodSnapTimeInMs = System.currentTimeMillis();
                                miSnapAnalyzerResult.setErrorCode(IAnalyzeResponse.ANALYZER_FRAME_IS_GOOD_BUT_WE_MUST_WAIT);
                            } else if (System.currentTimeMillis() - lastGoodSnapTimeInMs < DELAY_BEFORE_ALLOWING_SNAP_IN_MS) {
                                miSnapAnalyzerResult.setErrorCode(IAnalyzeResponse.ANALYZER_FRAME_IS_GOOD_BUT_WE_MUST_WAIT);
                            }
                        }
                        handleResults(false, miSnapAnalyzerResult, width, height, deviceOrientation, frame, cameraRotationDegrees, cameraParamMgr.getImageQuality(), cameraParamMgr.getImageDimensionMax(), 1 == cameraParamMgr.getUseFrontCamera());
                    } finally {
                        analyzingInProgress.set(false);
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            Log.d(TAG, e.toString());
        }
    }

    @Override
    public void handleManuallyCapturedFrame(final byte[] jpegImage, final int width, final int height, final int deviceOrientation, final int cameraRotationDegrees) {
        try {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    MiSnapAnalyzerResult miSnapAnalyzerResult = analyzer.onManualPictureTaken(jpegImage);
                    handleResults(true, miSnapAnalyzerResult, width, height, deviceOrientation, jpegImage, cameraRotationDegrees, cameraParamMgr.getImageQuality(), cameraParamMgr.getImageDimensionMax(), 1 == cameraParamMgr.getUseFrontCamera());
                }
            });
        } catch (RejectedExecutionException e) {
            Log.d(TAG, e.toString());
        }
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

    public void logMibiAndUxp(boolean isManualCapture, int deviceOrientation) {
        if (!isManualCapture) {
            MibiData.getInstance().addUXPEvent(UxpConstants.MISNAP_UXP_CAPTURE_TIME);
        } else {
            MibiData.getInstance().addUXPEvent(UxpConstants.MISNAP_UXP_CAPTURE_MANUAL);
        }

        logTheDeviceOrientation(deviceOrientation);
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
                               boolean useFrontCamera) {
        // update corners and post to workflow
        boolean usePortraitOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT == deviceOrientation || ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT == deviceOrientation;
        int rotationAdjustment = JPEGProcessor.getRotationAngle(Utils.needToRotateFrameBy180Degrees(cameraRotationDegrees), usePortraitOrientation, useFrontCamera);
        MiSnapAnalyzerResultsProcessor.updateCorners(miSnapAnalyzerResult,
                rotationAdjustment,
                width,
                height,
                useFrontCamera);
        EventBus.getDefault().post(miSnapAnalyzerResult); // TODO KW:  this is all temporary.  enables workflow to handle hint bubbles.

        if (isManualCapture || miSnapAnalyzerResult.analyzeSucceeded()) {
            // add mibi/uxp data
            logMibiAndUxp(isManualCapture, deviceOrientation);

            // deinit analyzer; camera is stopped by the manager, for better or worse, via CameraManager.receivedGoodFrame()
            analyzer.deinit();

            // transform frame and save as jpeg
            byte[] finalFrame;
            if (isManualCapture) {
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
            liveDataResult.postValue(new MiSnapControllerResult(finalFrame, miSnapAnalyzerResult.getFourCorners()));

            // all done, no need to handle further requests
            executorService.shutdownNow();
        }
    }

    public LiveData<MiSnapControllerResult> getLiveDataResult() {
        return liveDataResult;
    }
}
