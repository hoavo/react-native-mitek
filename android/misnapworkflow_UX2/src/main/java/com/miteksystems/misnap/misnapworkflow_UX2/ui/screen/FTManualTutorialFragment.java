package com.miteksystems.misnap.misnapworkflow_UX2.ui.screen;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.miteksystems.misnap.events.TextToSpeechEvent;
import com.miteksystems.misnap.misnapworkflow_UX2.R;
import com.miteksystems.misnap.misnapworkflow_UX2.storage.MiSnapPreferencesManager;
import com.miteksystems.misnap.params.DocType;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.utils.Utils;

import org.greenrobot.eventbus.EventBus;


public class FTManualTutorialFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    private static final String KEY_DOC_CHECKER = "KEY_DOC_CHECKER";
    private static final String KEY_ORIENTATION = "KEY_ORIENTATION";
    private static final int TTS_DELAY_MS = 2000;
    private OnFragmentInteractionListener mListener;
    private DocType mDocType;
    private int mRequestedOrientation;
    private boolean mButtonPressed;
    private ImageView mTutorialImage;

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean dontShowAgain) {
        MiSnapPreferencesManager.setIsFirstTimeUserManual(getContext(), !dontShowAgain, mDocType);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFTManualTutorialDone();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PlaceholderFragment.
     */
    public static FTManualTutorialFragment newInstance(@NonNull DocType docChecker, int requestedOrientation) {
    	FTManualTutorialFragment fragment = new FTManualTutorialFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_DOC_CHECKER, docChecker);
        args.putInt(KEY_ORIENTATION, requestedOrientation);

        fragment.setArguments(args);
        return fragment;
    }

    public FTManualTutorialFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDocType = (DocType) (getArguments().getSerializable(KEY_DOC_CHECKER));
        mRequestedOrientation = getArguments().getInt(KEY_ORIENTATION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment based on doc type
        View rootView;
        if (mDocType.isIdDocument() || mDocType.isBarcode()) {
            rootView = inflater.inflate(R.layout.manual_first_time_tutorial_ids_ux2, container, false);
            TextView message = (TextView) rootView.findViewById(R.id.misnap_manual_ft_message_ux2);
            message.setText(Html.fromHtml(getString(R.string.misnap_manual_tutorial_message_3_document_ux2)));

            mTutorialImage = (ImageView) rootView.findViewById(R.id.misnap_tutorial_image_ux2);
            loadTutorialImage();
        } else {
            rootView = inflater.inflate(R.layout.manual_first_time_tutorial_non_id_ux2, container, false);
            TextView message1 = (TextView) rootView.findViewById(R.id.misnap_manual_tutorial_message_1);
            TextView message2 = (TextView) rootView.findViewById(R.id.misnap_manual_tutorial_message_2);
            TextView message3 = (TextView) rootView.findViewById(R.id.misnap_manual_tutorial_message_3);

            message2.setText(getString(R.string.misnap_manual_tutorial_message_2_ux2));
            if (mDocType.isCheck()) {
                message1.setText(getString(R.string.misnap_manual_tutorial_message_1_check_ux2));
                message3.setText(getString(R.string.misnap_manual_tutorial_message_3_check_ux2));
            } else {
                message1.setText(getString(R.string.misnap_manual_tutorial_message_1_document_ux2));
                message3.setText(getString(R.string.misnap_manual_tutorial_message_3_document_ux2));
            }
        }

        //get the confirmation button handle
        Button bFTConfirmationBtn = (Button)rootView.findViewById(R.id.ft_manual_tut_btn);
        bFTConfirmationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //pass the control to the activity
                if (mListener != null && !mButtonPressed) {
                    mButtonPressed = true;
                    mListener.onFTManualTutorialDone();
                }
            }
        });

        CheckBox checkBoxDontShowAgain = (CheckBox) rootView.findViewById(R.id.checkbox_dont_show_again);
        if (!(mDocType.isIdDocument() || mDocType.isBarcode())) {
            checkBoxDontShowAgain.setVisibility(View.GONE);
        } else {
            checkBoxDontShowAgain.setOnCheckedChangeListener(this);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        StringBuilder stringBuilder = new StringBuilder();
        if (mDocType.isCheck()) {
            stringBuilder.append(getString(R.string.misnap_manual_tutorial_message_1_check_ux2));
            stringBuilder.append(getString(R.string.misnap_manual_tutorial_message_2_ux2));
            stringBuilder.append(getString(R.string.misnap_manual_tutorial_message_3_check_ux2));
        } else if (mDocType.isIdDocument()) {
            stringBuilder.append(Html.fromHtml(getString(R.string.misnap_manual_tutorial_message_3_document_ux2)));
        } else {
            stringBuilder.append(getString(R.string.misnap_manual_tutorial_message_1_document_ux2));
            stringBuilder.append(getString(R.string.misnap_manual_tutorial_message_2_ux2));
            stringBuilder.append(getString(R.string.misnap_manual_tutorial_message_3_document_ux2));
        }

        // Initial TTS interferes with the starting TTS messages, so give a slight delay
        EventBus.getDefault().post(new TextToSpeechEvent(stringBuilder.toString(), TTS_DELAY_MS));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDocType.isIdDocument()  || mDocType.isBarcode()) {
            loadTutorialImage();
        }
    }

    private void loadTutorialImage() {
        int orientation = Utils.getDeviceBasicOrientation(getContext());
        if (mDocType.isPassport()) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTutorialImage.setImageResource(R.drawable.misnap_help_passport_plain_ux2);
            } else {
                if (isVerticalPortrait()) {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_passport_plain_vertical_portrait_ux2);
                } else {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_passport_plain_horizontal_portrait_ux2);
                }
            }
        } else if (mDocType.isIdCardFront() || mDocType.isLicense()) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTutorialImage.setImageResource(R.drawable.misnap_help_id_plain_ux2);
            } else {
                if (isVerticalPortrait()) {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_id_plain_vertical_portrait_ux2);
                } else {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_id_plain_horizontal_portrait_ux2);
                }
            }
        } else if (mDocType.isIdCardBack()) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTutorialImage.setImageResource(R.drawable.misnap_help_id_back_plain_ux2);
            } else {
                if (isVerticalPortrait()) {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_id_back_plain_vertical_portrait_ux2);
                } else {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_id_back_plain_horizontal_portrait_ux2);
                }
            }
        } else if (mDocType.isBarcode()) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTutorialImage.setImageResource(R.drawable.misnap_help_dl_back_plain_ux2);
            } else {
                if (isVerticalPortrait()) {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_dl_back_plain_vertical_portrait_ux2);
                } else {
                    mTutorialImage.setImageResource(R.drawable.misnap_help_dl_back_plain_horizontal_portrait_ux2);
                }
            }
        }
    }

    private boolean isVerticalPortrait() {
        return (mRequestedOrientation == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_FREE_DOCUMENT_ALIGNED_WITH_DEVICE ||
                mRequestedOrientation == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_PORTRAIT_DOCUMENT_PORTRAIT);
    }
}
