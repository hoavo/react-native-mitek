package com.miteksystems.facialcapture.workflow.params;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by awood on 5/5/16.
 *
 * This file provides an example of one way to customize the FacialCapture workflow at runtime.
 */
public class FacialCaptureWorkflowParameters {
    private static final String TAG = FacialCaptureWorkflowParameters.class.getSimpleName();
    public static final String EXTRA_WORKFLOW_PARAMETERS = "FacialCapture.WORKFLOW.EXTRA_WORKFLOW_PARAMETERS";

    private Intent mIntent;
    private String mParam;
    private int mMinValue;
    private int mMaxValue;
    private int mDefaultValue;

    // Default values and ranges
    static final int SKIP_TUTORIAL_SCREEN_DEFAULT = 0;
    static final int SKIP_TUTORIAL_SCREEN_MIN = 0;
    static final int SKIP_TUTORIAL_SCREEN_MAX = 1;
    static final int MESSAGE_DELAY_DEFAULT = 100;
    static final int MESSAGE_DELAY_MIN = 0;
    static final int MESSAGE_DELAY_MAX = 3000;
    static final int TIMEOUT_MS_DEFAULT = 10000;
    static final int TIMEOUT_MS_MIN = 10000;
    static final int TIMEOUT_MS_MAX = 90000;

    // TTS delay
    private static final int TTS_DELAY_MS = 4000;


    private FacialCaptureWorkflowParameters(Intent intent, String param, int minValue, int maxValue, int defaultValue) {
        mIntent = intent;
        mParam = param;
        mMinValue = minValue;
        mMaxValue = maxValue;
        mDefaultValue = defaultValue;
    }


    /**
     * This parameter specifies the timeout period for auto-capture mode (blink detection).
     * After this period elapses, the timeout screen will be shown, allowing the user
     * to choose to retry auto-capture mode, or skip to manual capture.
     * <p>
     * <b>Values:</b>
     * <br>Range: {@value #TIMEOUT_MS_MIN} - {@value #TIMEOUT_MS_MAX}
     * <br>Default: {@value #TIMEOUT_MS_DEFAULT}
     */
    public static final String FACIALCAPTURE_WORKFLOW_TIMEOUT = "FACIALCAPTURE_WORKFLOW_TIMEOUT";

    // Parameters
    /**
     * This parameter changes the workflow to include or exclude the Auto-capture tutorial screen
     * as the first screen a user sees.
     * <p>
     * <b>Values:</b>
     * <b>Values:</b>
     * <br>Range: {@value #SKIP_TUTORIAL_SCREEN_MIN} - {@value #SKIP_TUTORIAL_SCREEN_MAX}
     * <br>Default: {@value #SKIP_TUTORIAL_SCREEN_DEFAULT} (i.e. always start with the tutorial screen)
     */
    public static final String FACIALCAPTURE_WORKFLOW_SKIP_TUTORIAL_SCREEN = "FACIALCAPTURE_WORKFLOW_SKIP_TUTORIAL_SCREEN";

    /**
     * This parameter sets the delay (in milliseconds) between guide messages displayed to the user.
     * <p>
     * <b>Values:</b>
     * <br>Range: {@value #MESSAGE_DELAY_MIN} - {@value #MESSAGE_DELAY_MAX}
     * <br>Default: {@value #MESSAGE_DELAY_DEFAULT} (i.e. almost continuously update the messages)
     */
    public static final String FACIALCAPTURE_WORKFLOW_MESSAGE_DELAY = "FACIALCAPTURE_WORKFLOW_MESSAGE_DELAY";


    // Getters
    public static int getTimeoutDelayMs(Intent intent) {
        return new FacialCaptureWorkflowParameters(intent,
                FACIALCAPTURE_WORKFLOW_TIMEOUT,
                TIMEOUT_MS_MIN,
                TIMEOUT_MS_MAX,
                TIMEOUT_MS_DEFAULT).getParameterValue();
    }

    public static boolean getSkipTutorialScreen(Intent intent) {
        return new FacialCaptureWorkflowParameters(intent,
                FACIALCAPTURE_WORKFLOW_SKIP_TUTORIAL_SCREEN,
                SKIP_TUTORIAL_SCREEN_MIN,
                SKIP_TUTORIAL_SCREEN_MAX,
                SKIP_TUTORIAL_SCREEN_DEFAULT).getParameterValue() != 0;
    }

    public static int getMessageDelayMs(Intent intent) {
        return new FacialCaptureWorkflowParameters(intent,
                FACIALCAPTURE_WORKFLOW_MESSAGE_DELAY,
                MESSAGE_DELAY_MIN,
                MESSAGE_DELAY_MAX,
                MESSAGE_DELAY_DEFAULT).getParameterValue();
    }


    // Helper methods
    private int getParameterValue() {
        int value = mDefaultValue;
        if (parameterHasBeenSet()) {
            try {
                value = readParameter();
                value = cropToRange(value);
            } catch (Exception e) {
                Log.d(TAG, "Using default value of " + mDefaultValue + " for parameter " + mParam);
            }
        }
        return value;
    }

    private boolean parameterHasBeenSet() {
        return getWorkflowParameters().has(mParam);
    }

    private int readParameter() throws JSONException {
        return getWorkflowParameters().getInt(mParam);
    }

    private JSONObject getWorkflowParameters() {
        String extras = mIntent.getStringExtra(EXTRA_WORKFLOW_PARAMETERS);
        if (extras == null) {
            return new JSONObject();
        }
        JSONObject workflowParams;
        try {
            workflowParams = new JSONObject(extras);
        } catch (JSONException e) {
            workflowParams = new JSONObject();
        }
        return workflowParams;
    }

    private final int cropToRange(int value) {
        if (value < mMinValue) {
            return mMinValue;
        } else if (value > mMaxValue) {
            return mMaxValue;
        } else {
            return value;
        }
    }

    public static int getTTSDelayMS() {
        return TTS_DELAY_MS;
    }
}
