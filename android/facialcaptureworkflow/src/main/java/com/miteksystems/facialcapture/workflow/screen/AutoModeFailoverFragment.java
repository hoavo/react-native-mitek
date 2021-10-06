package com.miteksystems.facialcapture.workflow.screen;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.miteksystems.facialcapture.controller.FacialCaptureFragment;
import com.miteksystems.facialcapture.workflow.FragmentLoader;
import com.miteksystems.facialcapture.workflow.R;
import com.miteksystems.facialcapture.workflow.params.FacialCaptureWorkflowParameters;
import com.miteksystems.misnap.events.TextToSpeechEvent;
import com.miteksystems.misnap.params.CameraApi;
import com.miteksystems.misnap.params.MiSnapApi;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by awood on 4/25/16.
 */
public class AutoModeFailoverFragment extends Fragment {

    private static final String TAG = AutoModeTutorialFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.facialcapture_fragment_auto_mode_failover, container, false);

        Button buttonAuto= (Button) rootView.findViewById(R.id.failover_to_auto);
        buttonAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentLoader.showScreen(getFragmentManager(), new FacialCaptureFragment());
            }
        });

        Button buttonManual= (Button) rootView.findViewById(R.id.failover_to_manual);
        buttonManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JSONObject jobSettings = new JSONObject(getActivity().getIntent().getStringExtra(MiSnapApi.JOB_SETTINGS));
                    jobSettings.put(CameraApi.MiSnapCaptureMode, CameraApi.PARAMETER_CAPTURE_MODE_MANUAL);
                    Intent intent = getActivity().getIntent();
                    intent.putExtra(MiSnapApi.JOB_SETTINGS, jobSettings.toString());
                    getActivity().setIntent(intent);
                } catch (JSONException e) {
                    Log.e(TAG, "Malformed job settings in intent:  " + e.toString());
                }
                FragmentLoader.showScreen(getFragmentManager(), new FacialCaptureFragment());
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().post(new TextToSpeechEvent(R.string.facialcapture_timeout_tts,
                FacialCaptureWorkflowParameters.getTTSDelayMS()));
    }
}
