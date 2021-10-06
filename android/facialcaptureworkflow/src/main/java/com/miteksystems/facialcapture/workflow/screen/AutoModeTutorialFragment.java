package com.miteksystems.facialcapture.workflow.screen;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.miteksystems.facialcapture.controller.FacialCaptureFragment;
import com.miteksystems.facialcapture.workflow.FragmentLoader;
import com.miteksystems.facialcapture.workflow.R;
import com.miteksystems.facialcapture.workflow.params.FacialCaptureWorkflowParameters;
import com.miteksystems.misnap.events.TextToSpeechEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by awood on 4/25/16.
 */
public class AutoModeTutorialFragment extends Fragment {

    private static final String TAG = AutoModeTutorialFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.facialcapture_fragment_auto_mode_tutorial, container, false);

        ImageView imageBg = (ImageView) rootView.findViewById(R.id.auto_mode_tutorial_blinking_image);


        AnimationDrawable animation = (AnimationDrawable) imageBg.getDrawable();
        animation.start();

        Button buttonStart = (Button) rootView.findViewById(R.id.auto_mode_tutorial_button_start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentLoader.showScreen(getFragmentManager(), new FacialCaptureFragment());
            }
        });

        Button buttonCancel = (Button) rootView.findViewById(R.id.auto_mode_tutorial_button_cancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });


        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().post(new TextToSpeechEvent(R.string.facialcapture_auto_mode_tutorial_tts,
                FacialCaptureWorkflowParameters.getTTSDelayMS()));
    }
}
