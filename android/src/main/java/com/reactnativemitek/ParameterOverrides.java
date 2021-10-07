package com.reactnativemitek;

import android.content.Context;
import android.content.SharedPreferences;

import com.miteksystems.facialcapture.science.api.params.FacialCaptureApi;
import com.miteksystems.facialcapture.science.api.params.FacialCaptureParamMgr;
import com.miteksystems.misnap.params.CameraApi;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.DocType;
import com.miteksystems.misnap.params.MiSnapApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by awood on 11/22/17.
 */

public class ParameterOverrides {

    public static final String FACIAL_CAPTURE_PREFERENCE_FILE_NAME = "FacialCaptureCustomPreference";

    private Context context;
    private Map<String, Integer> params;
    private static String[] configurableParameters = new String[] {
            CameraApi.MiSnapCaptureMode,
            FacialCaptureApi.BlinkThreshold, FacialCaptureApi.EyeMinDistance, FacialCaptureApi.EyeMaxDistance,
            FacialCaptureApi.LightingMinThreshold, FacialCaptureApi.SharpnessMinThreshold, FacialCaptureApi.CaptureEyesOpen
    };

    public ParameterOverrides(Context context) {
        this.context = context.getApplicationContext();
        params = new HashMap<>();
        JSONObject defaultFaceParams = FacialCaptureParamMgr.getDefaultParameters(new DocType(MiSnapApi.PARAMETER_DOCTYPE_CAMERA_ONLY));
        JSONObject defaultMiSnapParams = CameraParamMgr.getDefaultParameters(new DocType(MiSnapApi.PARAMETER_DOCTYPE_CAMERA_ONLY));
        for (String key : configurableParameters) {
            try {
                if (defaultFaceParams.has(key)) {
                    params.put(key, defaultFaceParams.getInt(key));
                } else if (defaultMiSnapParams.has(key)) {
                    params.put(key, defaultMiSnapParams.getInt(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        SharedPreferences sharedPref = context.getSharedPreferences(FACIAL_CAPTURE_PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();

        for (Map.Entry<String, Integer> entry : params.entrySet()) {
            if (Arrays.asList(configurableParameters).contains(entry.getKey())) {
                setPreferenceValueButDoNotSave(editor, entry.getKey(), entry.getValue());
            }
        }

        editor.commit();

    }

    private void setPreferenceValueButDoNotSave(SharedPreferences.Editor editor, String prefName, int valueToSave) {
        editor.putInt(prefName, valueToSave);
    }

    public Map<String, Integer> load() {
        SharedPreferences sharedPref = context.getSharedPreferences(FACIAL_CAPTURE_PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        Map<String, ?> prefs = sharedPref.getAll();
        for (Map.Entry<String, ?> pref : prefs.entrySet()) {
            try {
                if (pref.getValue() instanceof String) {
                    params.put(pref.getKey(), Integer.valueOf((String) pref.getValue()));
                } else if (pref.getValue() instanceof Integer) {
                    params.put(pref.getKey(), (Integer)pref.getValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return params;
    }

    /**
     *
     * @param param
     * @param value
     * @return True if the parameter is configurable and was set
     */
    public boolean setParamValue(String param, Integer value) {
        if (Arrays.asList(configurableParameters).contains(param)) {
            params.put(param, value);
            return true;
        }
        return false;
    }
}
