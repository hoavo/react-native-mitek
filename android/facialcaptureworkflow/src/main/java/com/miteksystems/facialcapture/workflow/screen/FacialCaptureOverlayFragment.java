package com.miteksystems.facialcapture.workflow.screen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.miteksystems.facialcapture.science.api.events.FacialCaptureAnalyzerResult;
import com.miteksystems.facialcapture.workflow.FragmentLoader;
import com.miteksystems.facialcapture.workflow.R;
import com.miteksystems.facialcapture.workflow.accessibility.MiSound;
import com.miteksystems.facialcapture.workflow.params.FacialCaptureWorkflowParameters;
import com.miteksystems.misnap.events.CaptureCurrentFrameEvent;
import com.miteksystems.misnap.events.OnCapturedFrameEvent;
import com.miteksystems.misnap.events.ScaledPreviewSizeStickyEvent;
import com.miteksystems.misnap.events.ShutdownEvent;
import com.miteksystems.misnap.events.TextToSpeechEvent;
import com.miteksystems.misnap.params.CameraApi;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.MiSnapApi;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import static android.view.Gravity.CENTER;

/**
 * Created by awood on 4/11/16.
 */
public class FacialCaptureOverlayFragment extends Fragment {
    private static final String TAG = FacialCaptureOverlayFragment.class.getSimpleName();
    private static final boolean SHOW_SELFIE = false;

    TextView mTextViewDebug;
    TextView mTextViewMessages;
    ImageButton mImageButtonCancel;
    TextView mImageButtonHelp;
    ImageButton mCaptureButton;

    TextView mTopBarHelpButton;


    private CameraParamMgr mCameraParamMgr;
    int mPreviousAccessibilityResId;
    Bitmap mCapturedBitmap;
    long mFpsStartTime;
    long mFpsNumFrames;

    //Settings
    private int mSelectedMessageDelay;
    private static long MESSAGE_LAST_DISPLAYED_TIME = System.currentTimeMillis();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.facialcapture_fragment_facialcapture_overlay, null, false);

        Intent intent = new Intent();
        if (null != getActivity()) {
            intent = getActivity().getIntent();
        } else {
            Log.e(TAG, "host activity was null");
        }
        JSONObject jobSettings;
        try {
            jobSettings = new JSONObject(intent.getStringExtra(MiSnapApi.JOB_SETTINGS));
        } catch (JSONException e) {
            jobSettings = new JSONObject();
        }
        mCameraParamMgr = new CameraParamMgr(jobSettings);

        mTextViewDebug = rootView.findViewById(R.id.facialcapture_overlay_debug);
        mTextViewMessages = rootView.findViewById(R.id.facialcapture_overlay_messages);
        mImageButtonCancel = rootView.findViewById(R.id.facialcapture_overlay_cancel_button);
//        mImageButtonHelp = rootView.findViewById(R.id.facialcapture_overlay_help_button);
        mCaptureButton = rootView.findViewById(R.id.facialcapture_overlay_capture_button);

        mTopBarHelpButton = rootView.findViewById(R.id.facialcapture_overlay_help_button);

        if (mTopBarHelpButton != null) {
            mTopBarHelpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    helpHandler();
                }
            });
        }

//        mImageButtonHelp.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                helpHandler();
//            }
//        });

        mImageButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backHandler();
            }
        });

        rootView.findViewById(R.id.backImageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backHandler();
            }
        });

        // Show/Hide manual capture button
        if (mCameraParamMgr.getmCaptureMode() == CameraApi.PARAMETER_CAPTURE_MODE_MANUAL) {
//                || ParamsHelper.getCaptureMode(mFacialCaptureIntent) == MiSnapApiConstants.PARAMETER_CAPTURE_MODE_MANUAL) { // TODO KW 2017-11-20:  ???
            mCaptureButton.setVisibility(View.VISIBLE);
            mCaptureButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideOverlayButtons(); // Prevent multiple taps
                    EventBus.getDefault().post(new CaptureCurrentFrameEvent());
                }
            });
        } else {
            mCaptureButton.setVisibility(View.INVISIBLE);
        }

        // Settings
        getCustomerUIPreference();

        return rootView;
    }

    private void backHandler() {
        hideOverlayButtons(); // Prevent multiple taps
        EventBus.getDefault().post(new ShutdownEvent(ShutdownEvent.CANCELLED));
        getActivity().finish();
    }

    private void helpHandler() {
        EventBus.getDefault().post(new ShutdownEvent(ShutdownEvent.HELP));
        hideOverlayButtons(); // Prevent multiple taps
        if (mCameraParamMgr.getCaptureMode() == CameraApi.PARAMETER_CAPTURE_MODE_MANUAL) {
            FragmentLoader.showScreen(getFragmentManager(), new ManualModeHelpFragment());
        } else {
            hideOverlayButtons(); // Prevent multiple taps
            FragmentLoader.showScreen(getFragmentManager(), new AutoModeHelpFragment());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mTextViewDebug = null;
        mTextViewMessages = null;
        mCapturedBitmap = null;

        mImageButtonCancel.setImageResource(0);
        mImageButtonCancel.setImageDrawable(null);
        mImageButtonCancel.setImageResource(android.R.color.transparent);
        mImageButtonCancel = null;

//        mImageButtonHelp.setImageResource(0);
//        mImageButtonHelp.setImageDrawable(null);
//        mImageButtonHelp.setImageResource(android.R.color.transparent);
//        mImageButtonHelp = null;

        mCaptureButton.setImageResource(0);
        mCaptureButton.setImageDrawable(null);
        mCaptureButton.setImageResource(android.R.color.transparent);
        mCaptureButton = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        EventBus.getDefault().post(new TextToSpeechEvent(R.string.facialcapture_overlay_tts,
                FacialCaptureWorkflowParameters.getTTSDelayMS()));
    }

    @Override
    public void onPause() {
        super.onPause();

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FacialCaptureAnalyzerResult event) {
        // Show debug messages
        double fps = 0.0d;
        if (mFpsStartTime == 0) {
            // Initialize
            mFpsStartTime = System.currentTimeMillis();
            mFpsNumFrames = 0;
        } else {
            mFpsNumFrames++;
            long curTime = System.currentTimeMillis();
            long elapsedTime = curTime - mFpsStartTime;
            fps = 1000.0d * mFpsNumFrames / elapsedTime; // 1000 is to convert ms to s.
            // Reset every two seconds for up-to-date accuracy
            if (elapsedTime >= 2000) {
                mFpsStartTime = curTime;
                mFpsNumFrames = 0;
            }
        }

        StringBuffer sb = new StringBuffer();
        sb.append(getColoredString("Device upright: ", event.isDeviceUpright()));
        sb.append(getColoredString("<br />Face found: ", event.isFaceFound()));
        String eyePos = event.isFaceDistanceGood() ? "Good" : (event.isFaceTooFarAway() ? "Too Close" : "Too far");
        sb.append(getColoredString("<br />Eye distance: " + eyePos, event.isFaceDistanceGood()));
        sb.append(getColoredString("<br />Sharpness: ", event.isSharpnessGood()));
        sb.append(getColoredString("<br />Uniform lighting: " + event.isLightingUniform()));
        sb.append(getColoredString("<br />Blinking: ", event.isBlinkDetected()));
        sb.append(getColoredString("<br />Passive: ", event.isLivenessDetected()));
        sb.append(getColoredString("<br />FPS: " + fps));
        mTextViewDebug.setText(Html.fromHtml(sb.toString()));

        // Show user help messages
        long currentTime = System.currentTimeMillis();

        if (currentTime - MESSAGE_LAST_DISPLAYED_TIME > mSelectedMessageDelay) {
            if (!event.isDeviceUpright()) {
                showMessageToUser(R.string.facialcapture_overlay_message_hold_phone_upright);
            } else if (!event.isFaceFound()) {
                showMessageToUser(R.string.facialcapture_overlay_message_face_not_found);
            } else if (event.isFaceTooClose()) {
                showMessageToUser(R.string.facialcapture_overlay_message_face_move_further_away);
            } else if (event.isFaceTooFarAway()) {
                showMessageToUser(R.string.facialcapture_overlay_message_face_get_closer);
            } else if (!event.isLightingUniform()) {
                showMessageToUser(R.string.facialcapture_overlay_message_lighting_fail);
            } else if (!event.isSharpnessGood()) {
                showMessageToUser(R.string.facialcapture_overlay_message_sharpness_fail);
            } else if (event.isRunningPassiveLiveness()) {
                showMessageToUser(R.string.facialcapture_overlay_message_passive_liveness_in_progress);
            } else if (!event.isBlinkDetected()) {
                if (mCameraParamMgr.isCurrentModeVideo()) {
                    showMessageToUser(R.string.facialcapture_overlay_message_blink_now);
                } else {
                    showMessageToUser(R.string.facialcapture_overlay_message_tap_now);
                }
            } else {
                showMessageToUser(R.string.facialcapture_overlay_message_empty);
            }

            MESSAGE_LAST_DISPLAYED_TIME = currentTime;
        }

        // For testing: Show if Daon detected a blink, even if we didn't capture
        if (event.isBlinkDetected()) {
//            Toast.makeText(getActivity(), "Blink detected!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "FacialCapture detected a blink.");
        }
    }

    private void showMessageToUser(int stringResId) {
        mTextViewMessages.setText(stringResId);
        if (stringResId == R.string.facialcapture_overlay_message_empty) {
            mTextViewMessages.setVisibility(View.INVISIBLE);
        } else {
            mTextViewMessages.setVisibility(View.VISIBLE);
            // Speak accessibility text, if it changed
            if (stringResId != mPreviousAccessibilityResId) {
                mPreviousAccessibilityResId = stringResId;
                EventBus.getDefault().post(new TextToSpeechEvent(stringResId));
            }
        }
    }

    private void hideOverlayButtons() {
        mImageButtonCancel.setVisibility(View.INVISIBLE);
//        mImageButtonHelp.setVisibility(View.INVISIBLE);
        mCaptureButton.setVisibility(View.INVISIBLE);
    }

    // TODO KW 2018-06-17:  posted by ControllerFragment.  what's a better way?
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OnCapturedFrameEvent event) {
        byte[] capturedImage = event.returnIntent.getByteArrayExtra(MiSnapApi.RESULT_PICTURE_DATA);

        getActivity().setResult(Activity.RESULT_OK, event.returnIntent);
        EventBus.getDefault().post(new ShutdownEvent(ShutdownEvent.CAPTURED));

        // Clear the green face rectangle and buttons
        showMessageToUser(R.string.facialcapture_overlay_message_empty);
        mImageButtonCancel.setVisibility(View.INVISIBLE);
//        mImageButtonHelp.setVisibility(View.INVISIBLE);

        // Draw a white rectangle over everything on the screen.
        // Tested users preferred this over seeing their selfie.
        ImageView fullScreenOverlay = getView().findViewById(R.id.facialcapture_overlay_captured_animation_bg);
        if (SHOW_SELFIE) {
            mCapturedBitmap = BitmapFactory.decodeByteArray(capturedImage, 0, capturedImage.length);
            fullScreenOverlay.setImageBitmap(mCapturedBitmap);
            fullScreenOverlay.setVisibility(View.VISIBLE);
            fullScreenOverlay.invalidate();
        } else {
            fullScreenOverlay.setBackgroundColor(0xFFFFFFFF);
            fullScreenOverlay.setVisibility(View.VISIBLE);
        }

        // Draw the snap animation on the screen
        final ImageView snap = getView().findViewById(R.id.facialcapture_overlay_captured_animation);
        snap.setImageResource(R.drawable.facialcapture_bug_animation_40);
        Animation bugAnimation = AnimationUtils.loadAnimation(getActivity().getApplication(), R.anim.misnap_bug_animation);
        bugAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                snap.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
//                ImageView snap = (ImageView) getView().findViewById(R.id.facialcapture_overlay_captured_animation);
                snap.setVisibility(View.GONE);
                snap.setImageResource(0);
                snap.invalidate();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        snap.startAnimation(bugAnimation);

        // And fade-in the success checkmark image. Finish the Activity when the animation is done.
        final ImageView successCheckmark = getView().findViewById(R.id.facialcapture_overlay_captured_success_checkmark);
        final TextView successTextView = getView().findViewById(R.id.textView);
        Animation successAnimation = AnimationUtils.loadAnimation(getActivity().getApplication(), R.anim.facialcapture_success_animation);
        successAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                successCheckmark.setVisibility(View.VISIBLE);
                successTextView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
//                ImageView checkmark = (ImageView) getView().findViewById(R.id.facialcapture_overlay_captured_success_checkmark);
                successCheckmark.setVisibility(View.GONE);
                successTextView.setVisibility(View.GONE);
//                successLayout.setImageResource(0);
                successCheckmark.invalidate();
                successTextView.invalidate();

                // Finish the Activity when the animation is done.
                getActivity().finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        successCheckmark.startAnimation(successAnimation);
        successTextView.startAnimation(successAnimation);

        // Play a shutter click sound, vibrate, and possibly do TTS.
        MiSound.playCameraClickSound(getActivity().getApplicationContext());
        MiSound.vibrate(getActivity().getApplicationContext());
        EventBus.getDefault().post(new TextToSpeechEvent(R.string.facialcapture_overlay_success_text));
    }

    // Uses green if value is true, or red if value is false
    String getColoredString(String prefix, boolean value) {
        return (value ? "<font color=#00FF00>" : "<font color=#FF0000>") + prefix + value + "</font>";
    }

    // Always uses green
    String getColoredString(String prefix) {
        return "<font color=#00FF00>" + prefix + "</font>";
    }

    private class RectangleView extends View {
        Paint mPaint;
        Rect mRect;

        public RectangleView(Context context) {
            super(context);
            mPaint = new Paint();
            mPaint.setColor(Color.GREEN);
            mPaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (mRect != null) {
                Log.i(TAG, "drawing..");
                canvas.drawRect(mRect, mPaint);
            }
        }

        public void setOutlineRect(Rect rect) {
            mRect = rect;
        }

        public void clearOutlineRect() {
            mRect = null;
        }
    }

    private void getCustomerUIPreference() {
        mSelectedMessageDelay = FacialCaptureWorkflowParameters.getMessageDelayMs(getActivity().getIntent());
    }

    @Subscribe(sticky = true)
    public void onEstablishedPreviewSizeStickyEvent(ScaledPreviewSizeStickyEvent event) {
        View rootView = getView();
        if (null == rootView) {
            return;
        }

        ConstraintLayout constraintLayout = rootView.findViewById(R.id.facialcapture_overlay_layout);

        // recall that getLayoutParams() actually returns the LayoutParams for its parent, which is a FrameLayout in this case
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) constraintLayout.getLayoutParams();
        layoutParams.gravity = CENTER;
        layoutParams.width = event.getWidth();
        layoutParams.height = event.getHeight();

        constraintLayout.setLayoutParams(layoutParams);
        constraintLayout.requestLayout();
    }
}
