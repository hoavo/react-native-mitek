package com.miteksystems.facialcapture.workflow.screen;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.miteksystems.facialcapture.controller.FacialCaptureFragment;
import com.miteksystems.facialcapture.workflow.FragmentLoader;
import com.miteksystems.facialcapture.workflow.R;
import com.miteksystems.facialcapture.workflow.params.FacialCaptureWorkflowParameters;
import com.miteksystems.misnap.events.TextToSpeechEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by flee on 5/10/16.
 */
public class AutoModeHelpFragment extends Fragment {

    private static final String TAG = AutoModeHelpFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.facialcapture_fragment_auto_mode_help, container, false);
        
        Button buttonProcess = (Button) rootView.findViewById(R.id.auto_mode_help_process);
        buttonProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentLoader.showScreen(getFragmentManager(), new FacialCaptureFragment());
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().post(new TextToSpeechEvent(R.string.facialcapture_auto_mode_help_tts, FacialCaptureWorkflowParameters.getTTSDelayMS()));
    }
}
