package com.miteksystems.facialcapture.controller;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;
import android.util.Log;

import com.miteksystems.facialcapture.science.analyzer.FacialCaptureAnalyzer;
import com.miteksystems.facialcapture.science.api.events.FacialCaptureAnalyzerResult;
import com.miteksystems.facialcapture.science.api.params.FacialCaptureApi;
import com.miteksystems.facialcapture.science.api.params.FacialCaptureParamMgr;
import com.miteksystems.imaging.JPEGProcessor;
import com.miteksystems.misnap.ICamera;
import com.miteksystems.misnap.IFrameHandler;
import com.miteksystems.misnap.analyzer.IAnalyzeResponse;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class FacialCaptureController implements IFrameHandler {

    private static final String TAG = FacialCaptureController.class.getName();
    private static final int NUM_FRAMES_TO_KEEP = 4; // yeah it's random but it's per ios' solution

    private ICamera camera;
    private FacialCaptureAnalyzer analyzer;
    private FacialCaptureParamMgr facialCaptureParamMgr;
    private CameraParamMgr cameraParamMgr;
    private MutableLiveData<Pair<byte[], String>> liveDataControllerResult = new MutableLiveData<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AtomicBoolean analyzingInProgress = new AtomicBoolean();
    private LimitedQueue<byte[]> last4Frames = new LimitedQueue<>(NUM_FRAMES_TO_KEEP);
    private AtomicBoolean livenessDetected = new AtomicBoolean(false);

    private boolean testEyesOpen;

    public static class LimitedQueue<E> extends LinkedList<E> {

        private final int limit;

        public LimitedQueue(int limit) {
            this.limit = limit;
        }

        @Override
        public boolean add(E o) {
            boolean added = super.add(o); // adds to end
            while (added && size() > limit) {
                super.remove(); // removes head (i.e., oldest)
            }
            return added;
        }
    }

    public FacialCaptureController(ICamera camera, FacialCaptureAnalyzer analyzer, JSONObject params) {
        facialCaptureParamMgr = new FacialCaptureParamMgr(params);
        cameraParamMgr = new CameraParamMgr(params);
        this.camera = camera;
        this.analyzer = analyzer;
    }

    @VisibleForTesting
    public FacialCaptureController(ICamera camera, FacialCaptureAnalyzer analyzer, JSONObject params, boolean testEyesOpen) {
        this(camera, analyzer, params);
        this.testEyesOpen = testEyesOpen; // tells controller to not compress final image.  useful for comparing input and output images.
    }

    public void start() {
        camera.addFrameHandler(this);
    }

    public void end() {
        if (null != analyzer) {
            analyzer.deinit();
        }

        executorService.shutdownNow();
    }

    @VisibleForTesting
    public void handlePreviewFrameSync(final byte[] frame, final int width, final int height, int colorSpace, final int deviceOrientation, final int cameraRotationDegrees) {
        FacialCaptureAnalyzerResult result = analyzer.analyze(frame, width, height);
        if (!cameraParamMgr.isCurrentModeVideo()) {
            result.setErrorCode(IAnalyzeResponse.ANALYZER_IS_DISABLED); //dont want to auto snap in manual mode...
        }
        livenessDetected.set(result.isLivenessDetected());
        handleResults(result, frame, false, width, height, cameraRotationDegrees);
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
                        handlePreviewFrameSync(frame, width, height, colorSpace, deviceOrientation, cameraRotationDegrees);
                    } catch (Exception e ) {
                        Log.e(TAG, e.toString());
                        e.printStackTrace();
                    } finally {
                        analyzingInProgress.set(false);
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            Log.d(TAG, e.toString());
        }
    }

    @VisibleForTesting
    public void handleManuallyCapturedFrameSync(final byte[] frame, final int width, final int height, int deviceOrientation, final int cameraRotationDegrees) {
        handleResults(new FacialCaptureAnalyzerResult(IAnalyzeResponse.ANALYZER_IS_DISABLED), frame, true, width, height, cameraRotationDegrees);
    }

    @Override
    public void handleManuallyCapturedFrame(final byte[] frame, final int width, final int height, final int deviceOrientation, final int cameraRotationDegrees) {
        try {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    handleManuallyCapturedFrameSync(frame, width, height, deviceOrientation, cameraRotationDegrees);
                }
            });
        } catch (RejectedExecutionException e) {
            Log.d(TAG, e.toString());
        }
    }

    private void handleResults(FacialCaptureAnalyzerResult result, byte[] frame, boolean isManualCapture, int width, int height, int cameraRotationDegrees) {
        // ios doesn't track eyes open or closed, they simply return the 4th oldest frame
        if (result.isFrameGood()
                && result.getEyesOpen()) {
            last4Frames.add(frame.clone());
        }
        // always post event
        EventBus.getDefault().post(result);

        if (isManualCapture) {
            startCaptureSequence(frame, true, width, height, cameraRotationDegrees,
                    livenessDetected.get() ? MiSnapApi.RESULT_SUCCESS_STILL : FacialCaptureApi.RESULT_UNVERIFIED_STILL);
        } else if (result.isFrameUnverified() && result.isBlinkDetected()) {
            startCaptureSequence(frame, false, width, height, cameraRotationDegrees, FacialCaptureApi.RESULT_UNVERIFIED);
        } else if (result.isFrameGood() && result.isBlinkDetected() && (testEyesOpen || result.isLivenessDetected())) {
            startCaptureSequence(frame, false, width, height, cameraRotationDegrees, MiSnapApi.RESULT_SUCCESS_VIDEO);
        }
    }

    private void startCaptureSequence(byte[] frame, boolean isManualCapture, int width, int height, int cameraRotationDegrees, String resultCode) {
        analyzer.deinit();

        byte[] finalFrame;
        if (testEyesOpen) {
            finalFrame = last4Frames.isEmpty() ? frame : last4Frames.remove();
        } else {
            int rotationAdjustment = JPEGProcessor.getRotationAngle(Utils.needToRotateFrameBy180Degrees(cameraRotationDegrees), true, true);
            if (isManualCapture) {
                finalFrame = JPEGProcessor.getFinalJpegFromManuallyCapturedFrame(frame, cameraParamMgr.getImageQuality(), cameraParamMgr.getImageDimensionMax(), rotationAdjustment);
            } else {
                // just get the oldest frame, even if list isn't full
                frame = last4Frames.isEmpty() ? frame : last4Frames.remove();
                finalFrame = JPEGProcessor.getFinalJpegFromPreviewFrame(frame, width, height, cameraParamMgr.getImageQuality(), cameraParamMgr.getImageDimensionMax(), rotationAdjustment);
            }
        }

        liveDataControllerResult.postValue(new Pair<>(finalFrame, resultCode));

        executorService.shutdownNow();
    }

    public LiveData<Pair<byte[], String>> getLiveDataControllerResult() {
        return liveDataControllerResult;
    }

    @VisibleForTesting
    @Deprecated
    public void setLiveDataControllerResult(MutableLiveData<Pair<byte[], String>> liveDataControllerResult) {
        this.liveDataControllerResult = liveDataControllerResult;
    }
}
