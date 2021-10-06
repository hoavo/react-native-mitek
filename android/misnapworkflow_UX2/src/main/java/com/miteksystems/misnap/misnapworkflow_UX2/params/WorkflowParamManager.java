package com.miteksystems.misnap.misnapworkflow_UX2.params;

import com.miteksystems.misnap.mibidata.MibiData;
import com.miteksystems.misnap.params.ApiParameterBuilder;
import com.miteksystems.misnap.params.BaseParamMgr;
import com.miteksystems.misnap.params.DocType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by awood on 10/14/16.
 */

public class WorkflowParamManager extends BaseParamMgr {
    private String param;
    private String defaultStringValue;
    private int maxStringLen;
    private boolean isHex;
    private static final int MAX_LEN_DOC_TYPE_OVERLAY_LABEL = 40;

    private static String[] commonParameters = new String[] {
            WorkflowApi.MiSnapFailoverType, WorkflowApi.MiSnapInitialTimeout, WorkflowApi.MiSnapSubsequentTimeout,
            WorkflowApi.MiSnapMaxTimeouts, WorkflowApi.MiSnapSmartHintInitialDelay, WorkflowApi.MiSnapSmartHintUpdatePeriod,
            WorkflowApi.MiSnapAnimationRectangleColor, WorkflowApi.MiSnapAnimationRectangleStrokeWidth, WorkflowApi.MiSnapAnimationRectangleCornerRadius,
            WorkflowApi.MiSnapTrackGlare
    };

    public WorkflowParamManager(JSONObject params) {
        super(params);
    }

    public static JSONObject getDefaultParameters(DocType docType) {
        ApiParameterBuilder params = new ApiParameterBuilder();

        for (String param : commonParameters) {
            params.addParam(param, getDefaultIntThreshold(param, docType));
        }
        params.addParam(WorkflowApi.MiSnapTextOverlay, getDocSpecificTextOverlay(docType));
        params.addParam(WorkflowApi.MiSnapShortDescription, WorkflowApi.SHORT_DESCRIPTION_DEFAULT);
        params.addParam(WorkflowApi.MiSnapOverrideLocale, WorkflowApi.OVERRIDE_LOCALE_DEFAULT);

        return params.build();
    }

    public static int getDefaultIntThreshold(String parameter, DocType docType) {
        switch (parameter) {
            case WorkflowApi.MiSnapFailoverType:
                return WorkflowApi.MISNAP_FAILOVER_TYPE_DEFAULT;
            case WorkflowApi.MiSnapInitialTimeout:
                return WorkflowApi.INITIAL_TIMEOUT_DEFAULT;
            case WorkflowApi.MiSnapSubsequentTimeout:
                return WorkflowApi.SUBSEQUENT_TIMEOUT_DEFAULT;
            case WorkflowApi.MiSnapMaxTimeouts:
                return WorkflowApi.MAX_TIMEOUT_DEFAULT;
            case WorkflowApi.MiSnapSmartHintInitialDelay:
                return WorkflowApi.HINT_INITIAL_DELAY_DEFAULT;
            case WorkflowApi.MiSnapSmartHintUpdatePeriod:
                return WorkflowApi.HINT_UPDATE_DEFAULT;
            case WorkflowApi.MiSnapAnimationRectangleColor:
                return WorkflowApi.ANIMATION_RECT_COLOR_DEFAULT;
            case WorkflowApi.MiSnapAnimationRectangleStrokeWidth:
                return WorkflowApi.ANIMATION_RECT_STROKE_WIDTH_DEFAULT;
            case WorkflowApi.MiSnapAnimationRectangleCornerRadius:
                return WorkflowApi.ANIMATION_RECT_CORNER_RADIUS_DEFAULT;
            case WorkflowApi.MiSnapTrackGlare:
                return WorkflowApi.TRACK_GLARE_DEFAULT;
            default:
                return BaseParamMgr.getDefaultIntThreshold(parameter, docType);
        }
    }

    public static boolean getDefaultBooleanThreshold(String parameter, DocType docType) {
        switch (parameter) {
            //TODO make glare a boolean in 5.0
//            case WorkflowApi.MiSnapTrackGlare:
//                return WorkflowApi.TRACK_GLARE_DEFAULT;
            default:
                return false;
        }
    }

    static String getDocSpecificTextOverlay(DocType docType) {
        String overlay = "";
        if (docType.isCheckBack()) {
            overlay = WorkflowConstants.TEXT_OVERLAY_CHECK_BACK;
        } else if (docType.isCheckFront()) {
            overlay = WorkflowConstants.TEXT_OVERLAY_CHECK_FRONT;
        }
        return overlay;
    }

    public boolean useGlareTracking() {
        String param = WorkflowApi.MiSnapTrackGlare;
        return getIntParameterValueInRange(
                param,
                WorkflowApi.TRACK_GLARE_DISABLED,
                WorkflowApi.TRACK_GLARE_ENABLED,
                getDefaultIntThreshold(param, docType)) == WorkflowApi.TRACK_GLARE_ENABLED;
    }

    public int getFailoverType() {
        String param = WorkflowApi.MiSnapFailoverType;
        return getIntParameterValueInRange(
                param,
                WorkflowApi.FAILOVER_TYPE_LOWER_BOUND,
                WorkflowApi.FAILOVER_TYPE_UPPER_BOUND,
                getDefaultIntThreshold(param, docType));
    }

    public int getInitialTimeOut() {
        String param = WorkflowApi.MiSnapInitialTimeout;
        return getIntParameterValueInRange(
                param,
                WorkflowApi.INITIAL_TIMEOUT_LOWER_BOUND,
                WorkflowApi.INITIAL_TIMEOUT_UPPER_BOUND,
                getDefaultIntThreshold(param, docType));
    }

    public int getSubsequentTimeOut() {
        String param = WorkflowApi.MiSnapSubsequentTimeout;
        return getIntParameterValueInRange(
                param,
                WorkflowApi.SUBSEQUENT_TIMEOUT_LOWER_BOUND,
                WorkflowApi.SUBSEQUENT_TIMEOUT_UPPER_BOUND,
                getDefaultIntThreshold(param, docType));
    }

    public int getMaxTimeouts() {
        String param = WorkflowApi.MiSnapMaxTimeouts;
        return getIntParameterValueInRange(
                param,
                WorkflowApi.MAX_TIMEOUT_LOWER_BOUND,
                WorkflowApi.MAX_TIMEOUT_UPPER_BOUND,
                getDefaultIntThreshold(param, docType));
    }

    public String optLocaleOverride() {
        String param = WorkflowApi.MiSnapOverrideLocale;
        return getStringParameter(param, WorkflowApi.OVERRIDE_LOCALE_DEFAULT);
    }

    public String getTextPrompt(String defaultText, String defaultCheckFrontText, String defaultCheckBackText) {
        param = WorkflowApi.MiSnapTextOverlay;
        maxStringLen = MAX_LEN_DOC_TYPE_OVERLAY_LABEL;
        if (docType.isCheckBack()) {
            defaultStringValue = defaultCheckBackText;
        } else if (docType.isCheckFront()) {
            defaultStringValue = defaultCheckFrontText;
        } else {
            defaultStringValue = defaultText;
        }
        return getCroppedStringParameter(param, maxStringLen, defaultStringValue);
    }

    public String getShortDescription() {
        param = WorkflowApi.MiSnapShortDescription;
        maxStringLen = MAX_LEN_DOC_TYPE_OVERLAY_LABEL;
        defaultStringValue = "";
        return getCroppedStringParameter(param, maxStringLen, defaultStringValue);
    }

    public int getSmartHintInitialDelay() {
        String param = WorkflowApi.MiSnapSmartHintInitialDelay;
        return getIntParameterValueInRange(
                param,
                WorkflowApi.HINT_INITIAL_DELAY_LOWER_BOUND,
                WorkflowApi.HINT_INITIAL_DELAY_UPPER_BOUND,
                getDefaultIntThreshold(param, docType));
    }

    public int getSmartHintUpdatePeriod() {
        String param = WorkflowApi.MiSnapSmartHintUpdatePeriod;
        return getIntParameterValueInRange(
                param,
                WorkflowApi.HINT_UPDATE_LOWER_BOUND,
                WorkflowApi.HINT_UPDATE_UPPER_BOUND,
                getDefaultIntThreshold(param, docType));
    }

    public int getAnimationRectangleColor() {
        String param = WorkflowApi.MiSnapAnimationRectangleColor;
        return getIntParameterValueInRange(
                param,
                WorkflowApi.ANIMATION_RECT_COLOR_LOWER_BOUND,
                WorkflowApi.ANIMATION_RECT_COLOR_UPPER_BOUND,
                getDefaultIntThreshold(param, docType));
    }

    public int getAnimationRectangleStrokeWidth() {
        String param = WorkflowApi.MiSnapAnimationRectangleStrokeWidth;
        return getIntParameterValueInRange(
                param,
                WorkflowApi.ANIMATION_RECT_STROKE_WIDTH_LOWER_BOUND,
                WorkflowApi.ANIMATION_RECT_STROKE_WIDTH_UPPER_BOUND,
                getDefaultIntThreshold(param, docType));
    }

    public int getAnimationRectangleCornerRadius() {
        String param = WorkflowApi.MiSnapAnimationRectangleCornerRadius;
        return getIntParameterValueInRange(
                param,
                WorkflowApi.ANIMATION_RECT_CORNER_RADIUS_LOWER_BOUND,
                WorkflowApi.ANIMATION_RECT_CORNER_RADIUS_UPPER_BOUND,
                getDefaultIntThreshold(param, docType));
    }
}
