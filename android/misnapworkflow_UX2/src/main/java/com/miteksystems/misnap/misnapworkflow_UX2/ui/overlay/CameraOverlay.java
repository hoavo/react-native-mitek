package com.miteksystems.misnap.misnapworkflow_UX2.ui.overlay;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.SensorManager;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.miteksystems.imaging.JPEGProcessor;
import com.miteksystems.misnap.analyzer.MiSnapAnalyzerResult;
import com.miteksystems.misnap.events.OnCapturedFrameEvent;
import com.miteksystems.misnap.events.OnStartedEvent;
import com.miteksystems.misnap.events.OnTorchStateEvent;
import com.miteksystems.misnap.events.ScaledPreviewSizeStickyEvent;
import com.miteksystems.misnap.events.TextToSpeechEvent;
import com.miteksystems.misnap.events.TorchStateEvent;
import com.miteksystems.misnap.mibidata.MibiData;
import com.miteksystems.misnap.misnapworkflow_UX2.R;
import com.miteksystems.misnap.misnapworkflow_UX2.params.UxpConstants;
import com.miteksystems.misnap.misnapworkflow_UX2.params.WorkflowConstants;
import com.miteksystems.misnap.misnapworkflow_UX2.params.WorkflowParamManager;
import com.miteksystems.misnap.misnapworkflow_UX2.ui.AutoResizeTextView;
import com.miteksystems.misnap.misnapworkflow_UX2.ui.animation.FrameSequenceAnimation;
import com.miteksystems.misnap.misnapworkflow_UX2.ui.animation.MiSnapAnimation;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.DocType;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.params.ScienceParamMgr;
import com.miteksystems.misnap.storage.CameraInfoCacher;
import com.miteksystems.misnap.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CameraOverlay extends RelativeLayout {
    private static final String TAG = "CameraOverlay";
    // If true, allow the user to manually capture even in auto-capture mode
    private static final boolean ALWAYS_SHOW_MANUAL_CAPTURE_BUTTON = false;
    // Replaces MiSnapAnimatedBug - If true, animate the bug
    private static final boolean ANIMATED_BUG = true;
    // Replaces MiSnapSmartHintEnabled
    private static boolean SMART_HINT_ENABLED = false;
    // Replaces MiSnapCameraVignetteImageEnabled
    private static final boolean VIGNETTE_IMAGE_ENABLED = false;
    // Replaces MiSnapCameraGuideImageEnabled, which was mis-named and actually refers to the Ghost image
    private static final boolean GHOST_IMAGE_ENABLED = true;
    // Replaces MiSnapGhostImageAlwaysOn
    private static final boolean GHOST_IMAGE_ALWAYS_ON = false;
    private static final double GHOST_IMAGE_ADDITIONAL_SCALE_FACTOR = 1.0d; // e.g. 1.15d means 1.15 - 1 = 15%
    // New workflow param for MiSnapGlareTracking
    private boolean DRAW_REALTIME_GLARE_OUTLINE;
    private static final boolean DRAW_REALTIME_DOC_OUTLINE = false;
    List<Point> debugOutlineCorners = new ArrayList<>();
    Context mAppContext;
    TextView mHelpButton;
    TextView mTopBarHelpButton;
    TextView mFrontCaptureHintTextView;
	ImageButton mCaptureButton;
    Button mFlashToggle;
    ImageButton mPoweredLogo;
	ImageView mGhostImage;
    TextView mBalloonImage;
    AutoResizeTextView mGhostText;
    ImageView mBugImage;
    TextView mBugText;
    LinearLayout mBugBackground;
    ImageView mVignetteImage;
    ImageView instructionText;
    int balloonBackgroundDrawableResId;

    private View topGhostView;
    private View bottomGhostView;
    private View leftGhostView;
    private View rightGhostView;

    private ConstraintLayout backConstraintLayout;
    private ConstraintLayout helpConstraintLayout;
    private ConstraintLayout topBarConstraintLayout;

    CameraParamMgr mCameraParamMgr;
    ScienceParamMgr mScienceParamMgr;
    WorkflowParamManager mWorkflowParamMgr;
    Bitmap mGhostBitmap;
    Bitmap mSnappedDoc;
	Animation mAnimationFadeOut, mAnimationFadeIn, mDocAnimation;
    protected List<HintBubble> mHintBubbles;
    protected HintBubble mCurrentHintBubble;
    boolean mGhostAnimationRunning;
    boolean drawDetectedRectangle;
    boolean drawFinalFrame;
    boolean mBubblesDelayInProgress;
    boolean mAllDone;
    boolean mGhostMsgTTSPlayed;
    boolean mTorchStatus;
    boolean wasGlareFound;
    private boolean mFrameCapturedIgnoreUXP;
    private FrameSequenceAnimation mBugSequence;
	TextView testText;
    // Setup the asynchronous messaging threads for user help
    Handler mHandler = new Handler();
    private static final int RECTANGLE_ANIMATION_INTERVAL_MILLIS = 70;
    private static final int RECTANGLE_ANIMATION_LENGTH_MILLIS = 600;
    private static final float RECTANGLE_ANIMATION_MIN_SCALE = 0.9f;
    private static final int BUG_ANIMATION_TIME_MS = 1600;
    private static final int GHOST_IMAGE_DELAY = 50;
    private float mDisplayWidth, mDisplayHeight;
    List<Point> detectedDocumentPoints = new ArrayList<>();
    List<Point> mRectangleAnimationPoints;
    int mPreviewWidth, mPreviewHeight;
    private Point mTarget = new Point();
    Paint mDetectedRectanglePaint;
    Path mDetectedRectanglePath;
    long mRectangleAnimationStart;
    int mBalloonStringResIdThatIsShowing;
    private OrientationEventListener orientationEventListener;
    private int mCurrentRotation;
    final static String TAG_TORCH_ON = "on";
    final static String TAG_TORCH_OFF = "off";
    byte[] mFinalFrameArray;
    int[] mDocumentCorner1;
    int[] mDocumentCorner2;
    int[] mDocumentCorner3;
    int[] mDocumentCorner4;
    Rect glareBox;
    private View.OnClickListener mOnClickListener;
    private ProgressDialog mManualCapturePleaseWaitDialog;
    private DocType mDocType;

	public CameraOverlay(Context context, AttributeSet attrs, JSONObject params, OnClickListener onClickListener) {
		this(context, attrs, 0, params, onClickListener);
	}
	
	public CameraOverlay(Context context, JSONObject params, OnClickListener onClickListener) {
		this(context, null, params, onClickListener);
	}

    public CameraOverlay(Context context, AttributeSet attrs, int defStyle, JSONObject params, OnClickListener onClickListener) {
        super(context, attrs, defStyle);
        mAppContext = context.getApplicationContext();
        mCameraParamMgr = new CameraParamMgr(params);
        mScienceParamMgr = new ScienceParamMgr(params);
        mWorkflowParamMgr = new WorkflowParamManager(params);
        loadWorkflowParameters();
        mOnClickListener = onClickListener;
        mFrameCapturedIgnoreUXP = false;

        mDocType = new DocType(mWorkflowParamMgr.getRawDocumentType());
        View.inflate(context, R.layout.misnap_your_camera_overlay_ux2, this);
        setupButtons();
        setupPaintObj();
        setupHintBubbles();
        // initialize mPreviewWidth and mPreviewHeight
        setPreviewParameters();
        EventBus.getDefault().register(this);
        // CameraOverlay needs to know if the torch is on or not
        EventBus.getDefault().post(new TorchStateEvent("GET"));
    }

    private void loadWorkflowParameters() {
        DRAW_REALTIME_GLARE_OUTLINE = mWorkflowParamMgr.useGlareTracking();
    }

    private void setupPaintObj() {
        // setup rectangle paint
        mDetectedRectanglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDetectedRectanglePaint.setDither(true);
        mDetectedRectanglePaint.setStyle(Paint.Style.STROKE);
        mDetectedRectanglePaint.setStrokeJoin(Paint.Join.ROUND);
        mDetectedRectanglePaint.setStrokeCap(Paint.Cap.ROUND);
        mDetectedRectanglePaint.setPathEffect(new CornerPathEffect(mWorkflowParamMgr.getAnimationRectangleCornerRadius()));
        mDetectedRectanglePaint.setAntiAlias(true);
        mDetectedRectanglePaint.setStrokeWidth(mWorkflowParamMgr.getAnimationRectangleStrokeWidth());
        mDetectedRectanglePaint.setColor(mWorkflowParamMgr.getAnimationRectangleColor());
        //set the path obj
        mDetectedRectanglePath = new Path();
    }

    private void setupHintBubbles() {
        balloonBackgroundDrawableResId = R.drawable.misnap_error_message_background_ux2;
        mHintBubbles = new ArrayList<>();
        if (mDocType.isPassport()) {
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.GLARE, R.string.misnap_glare_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.WRONG_DOCUMENT, R.string.misnap_wrong_doc_generic_ux2));
            mHintBubbles.add(new CornerConfusionHintBubble(MiSnapAnalyzerResult.FrameChecks.LOW_CONTRAST, R.string.misnap_low_contrast_ux2));
            mHintBubbles.add(new CornerConfusionHintBubble(MiSnapAnalyzerResult.FrameChecks.BUSY_BACKGROUND, R.string.misnap_busy_background_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.ROTATION_ANGLE, R.string.misnap_hold_center_generic_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.MAX_SKEW_ANGLE, R.string.misnap_hold_center_generic_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.HORIZONTAL_MINFILL, R.string.misnap_get_closer_generic_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.MIN_PADDING, R.string.misnap_too_close_generic_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.MAX_BRIGHTNESS, R.string.misnap_less_light_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.MIN_BRIGHTNESS, R.string.misnap_more_light_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.SHARPNESS, R.string.misnap_hold_steady_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.FOUR_CORNER_CONFIDENCE, 0));
        } else {
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.GLARE, R.string.misnap_glare_ux2));
            mHintBubbles.add(new CornerConfusionHintBubble(MiSnapAnalyzerResult.FrameChecks.LOW_CONTRAST, R.string.misnap_low_contrast_ux2));
            mHintBubbles.add(new CornerConfusionHintBubble(MiSnapAnalyzerResult.FrameChecks.BUSY_BACKGROUND, R.string.misnap_busy_background_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.ROTATION_ANGLE, R.string.misnap_hold_center_generic_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.MAX_SKEW_ANGLE, R.string.misnap_hold_center_generic_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.HORIZONTAL_MINFILL, R.string.misnap_get_closer_generic_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.MIN_PADDING, R.string.misnap_too_close_generic_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.MAX_BRIGHTNESS, R.string.misnap_less_light_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.MIN_BRIGHTNESS, R.string.misnap_more_light_ux2));
            int wrongDocSpeechId = mDocType.isCheck() ? (mDocType.isCheckBack() ? R.string.misnap_wrong_doc_check_back_expected_ux2 : R.string.misnap_wrong_doc_check_front_expected_ux2) : R.string.misnap_wrong_doc_generic_ux2;
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.WRONG_DOCUMENT, wrongDocSpeechId));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.SHARPNESS, R.string.misnap_hold_steady_ux2));
            mHintBubbles.add(new HintBubble(MiSnapAnalyzerResult.FrameChecks.FOUR_CORNER_CONFIDENCE, 0));
        }
    }

    private void setupButtons() {

        // so that onDraw will be called and redraws occur
        setWillNotDraw(false);

		//help button
//		mHelpButton = findViewById(R.id.misnap_overlay_help_button);
        mTopBarHelpButton = findViewById(R.id.topBarHelpTextView);
        mFrontCaptureHintTextView = findViewById(R.id.frontCaptureHintTextView);
		if(mHelpButton != null) {
//            mHelpButton.setImageResource(R.drawable.misnap_button_help_ux2);
			mHelpButton.setOnClickListener(mOnClickListener);
		}

        topGhostView = findViewById(R.id.topGhostView);
		bottomGhostView = findViewById(R.id.bottomGhostView);
        leftGhostView = findViewById(R.id.leftGhostView);
        rightGhostView = findViewById(R.id.rightGhostView);

        backConstraintLayout = findViewById(R.id.backConstraintLayout);
        helpConstraintLayout = findViewById(R.id.helpConstraintLayout);
        topBarConstraintLayout = findViewById(R.id.topBarConstraintLayout);

		if (mTopBarHelpButton != null) {
            mTopBarHelpButton.setOnClickListener(mOnClickListener);
        }

        findViewById(R.id.landscapeHelpTextView).setOnClickListener(mOnClickListener);

		//flash button
		mFlashToggle = findViewById(R.id.overlay_flash_toggle);
		try {
			if(mFlashToggle != null) {
			    if(mDocType.isIdDocument()) {
			        mFlashToggle.setVisibility(View.INVISIBLE);
                } else {
                    mFlashToggle.setVisibility(View.VISIBLE);
                }
                mFlashToggle.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.misnap_icon_flash_off_ux2), null, null, null);
                mFlashToggle.setText(getResources().getText(R.string.flash_off_ux2));
                mFlashToggle.setTextColor(getResources().getColor(R.color.misnap_flash_off_gray_ux2));
//				mFlashToggle.setImageResource(R.drawable.misnap_icon_flash_off_ux2);
				mFlashToggle.setOnClickListener(mOnClickListener);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//cancel button
//		mCancelButton = (ImageButton) findViewById(R.id.overlay_cancel_button);
//		if(mCancelButton != null) {
//            mCancelButton.setImageResource(R.drawable.camera_cancel_icon_ux2);
//			mCancelButton.setOnClickListener(mOnClickListener);
//		}
		//capture button
		mCaptureButton = findViewById(R.id.misnap_overlay_capture_button);
		if(mCaptureButton != null) {
//            mCaptureButton.setImageResource(R.drawable.misnap_camera_shutter_icon_ux2);
			mCaptureButton.setOnClickListener(mOnClickListener);
		}
		
		testText = findViewById(R.id.misnap_overlay_test_text);
		testText.setVisibility(View.VISIBLE);

        mVignetteImage = findViewById(R.id.misnap_vignette);
        setVignette();

        mPoweredLogo = findViewById(R.id.misnap_overlay_mitek_logo);

        //if accessibility is enabled, disable it on the following buttons
        postInvalidate();
    }

    void updateUI(boolean hasTorchSupport) {
        if (hasTorchSupport) {
            mFlashToggle.setVisibility(View.VISIBLE);
            mFlashToggle.setClickable(true);
        } else {
//            mHelpButton.setLayoutParams(mFlashToggle.getLayoutParams());
            mFlashToggle.setClickable(false);
            mFlashToggle.setVisibility(View.GONE);
        }
    }

    private void setOrientationListener(Context context) {
        orientationEventListener = new OrientationEventListener(
                context, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int angle) {
                onOrientationChange(angle);
            }
        };
        orientationEventListener.enable();
    }

    private void onOrientationChange(int angle) {

        if(mAppContext==null || mFrameCapturedIgnoreUXP) {
            return;
        }

        // Check if an orientation switch occurred (i.e. the user rotated the device)
        int rotation = Utils.getDeviceOrientation(mAppContext);
        if (rotation != mCurrentRotation) {
            Log.i(TAG, "Rotate from " + mCurrentRotation + " to " + rotation);
            mCurrentRotation = rotation;

            postInvalidate();
        }
    }

    private void stopOrientationListener() {
        if (null != orientationEventListener) {
            orientationEventListener.disable();
            orientationEventListener = null;
        }
    }

    public void initialize() {
        // set dimensions for the first time since the initial global layout listener calls this method
        updateDisplayDimensions();

        //set the right ghost image id
		initGhostImage();

        mBalloonImage = findViewById(R.id.misnap_balloon);
        mBalloonImage.setVisibility(View.INVISIBLE);

        mBugImage = findViewById(R.id.misnap_bug);
        mBugText = findViewById(R.id.misnap_bug_text);
        mBugBackground = findViewById(R.id.misnap_bug_background);
        mBugBackground.setVisibility(View.INVISIBLE);

        instructionText = findViewById(R.id.misnap_check_text_ux2);
        //US2582 - configurable check front/back text
        String overlayText = mWorkflowParamMgr.getTextPrompt(
                "",
                mAppContext.getString(R.string.misnap_check_front_text_ux2),
                mAppContext.getString(R.string.misnap_check_back_text_ux2));
        // TODO: Make instructionText a TextView
//        instructionText.setText(overlayText);
        instructionText.setContentDescription(overlayText);

        if (mDocType.isCheckFront()) {
            instructionText.setImageResource(R.drawable.misnap_en_android_check_heading_front_ux2);
        } else if(mDocType.isCheckBack()){
            instructionText.setImageResource(R.drawable.misnap_en_android_check_heading_back_ux2);
        } else {
            instructionText.setImageResource(android.R.color.transparent);
        }

        setOrientationListener(mAppContext);

        // For ID documents, based on user testing, we hide the top and bottom gray bars.
        // TODO KW 2017-09-26:  why are these bars needed at all ever?
        if (mDocType.isIdDocument()) {
            // For ID documents, based on user testing, we hide the top and bottom gray bars.
            LinearLayout topBar = findViewById(R.id.misnap_topbar);
            LinearLayout bottomBar = findViewById(R.id.misnap_bottombar);
            mPoweredLogo.setVisibility(VISIBLE);
            topBar.setVisibility(GONE);
            bottomBar.setVisibility(GONE);
        }


        if (mDocType.equals(MiSnapApi.PARAMETER_DOCTYPE_DRIVER_LICENSE)) {
            final ConstraintLayout driverLicenseFrontConstraintLayout = findViewById(R.id.driverLicenseFrontConstraintLayout);
            if (driverLicenseFrontConstraintLayout != null) {
                driverLicenseFrontConstraintLayout.setVisibility(View.VISIBLE);
                final Activity activity = (Activity) driverLicenseFrontConstraintLayout.getContext();

                findViewById(R.id.backImageView).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.onBackPressed();
                    }
                });

                findViewById(R.id.landscapeBackImageView).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.onBackPressed();
                    }
                });
            }
        }

        //update UI if device does not support the torch
        CameraInfoCacher cacher = new CameraInfoCacher(mAppContext, mCameraParamMgr.getUseFrontCamera());
		if(mCameraParamMgr != null && !cacher.hasTorch()) {
            updateUI(false);
		}
        postInvalidate();
        //others
        resetVariables();

        // This used to be called by MiSnapFragment:previewStartedState thru the UIManager.
        // Now we should call it ourselves.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showGhostImage();
                }
        }, GHOST_IMAGE_DELAY);

        SMART_HINT_ENABLED = false;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SMART_HINT_ENABLED = true;
            }
        }, mWorkflowParamMgr.getSmartHintInitialDelay());

        setUpManualCapturePleaseWaitDialog();
    }

    private void setUpManualCapturePleaseWaitDialog() {
        mManualCapturePleaseWaitDialog = new ProgressDialog(getContext(), R.style.MiSnapProgressDialog);
        mManualCapturePleaseWaitDialog.setMessage(mAppContext.getString(R.string.misnap_manual_capture_please_wait_ux2));
        mManualCapturePleaseWaitDialog.setCancelable(false);
        mManualCapturePleaseWaitDialog.setIndeterminateDrawable(ContextCompat.getDrawable(mAppContext, R.drawable.misnap_icon));
        Window window = mManualCapturePleaseWaitDialog.getWindow();
        if (null != window) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.gravity = Gravity.CENTER;
            window.setAttributes(layoutParams);
        }
    }

    private void setVignette() {
        if (VIGNETTE_IMAGE_ENABLED) {
            int vignetteResID = 0;

            if (mDocType.isBillPay()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_remittance");
            } else if (mDocType.isCheckBack()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_checkback");
            } else if (mDocType.isCheckFront()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_checkfront");
            } else if (mDocType.isBalanceTransfer()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_balance_transfer");
            } else if (mDocType.isLicense()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_driver_license");
            } else if (mDocType.isBusinessCard()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_business_card");
            } else if (mDocType.isAutoInsurance()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_auto_insurance");
            } else if (mDocType.isVin()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_vin");
            } else if (mDocType.isW2()) {
                vignetteResID = getVignetteResourceID("mitek_vignette_w2");
            } else if (mDocType.isPassport()){
                vignetteResID = getVignetteResourceID("mitek_vignette_passport");
            }

            try {
                // No custom vignette image i Drawable src. Use default Mitek image
                if (0 == vignetteResID) {
                    vignetteResID = getVignetteResourceID("misnap_mitek_vignette_ux2");
                }

                if (0 != vignetteResID) {
                    mVignetteImage.setBackgroundResource(vignetteResID);
                    mVignetteImage.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // if going from video mode-show vignette,
            // 	       to manual mode-hide vignette
            // need to hide view
            mVignetteImage.setVisibility(View.INVISIBLE);
        }
    }

    private int getVignetteResourceID(String specificVignetteName) {
        return mAppContext.getResources().getIdentifier(specificVignetteName,
                "drawable",
                getContext().getPackageName());
    }

    @SuppressWarnings({"IntegerDivisionInFloatingPointContext", "SuspiciousNameCombination"})
    public void initGhostImage() {

		if(mGhostImage == null) {
			mGhostImage = findViewById(R.id.misnap_ghost_image);
		}
		if (mGhostText == null){
            mGhostText = findViewById(R.id.misnap_ghost_text);
        }
		//get the guide image handle
		int lGhostImageId = getGhostImageDrawableId();
		//set it
		if(mGhostImage != null) {
			mGhostImage.setVisibility(View.INVISIBLE); //this call is must otherwise animation won't work as the view had never been rendered
			if(lGhostImageId > 0) {
				//we need to show the guide image
                try {
                    Bitmap lScaledGhostImage = BitmapFactory.decodeResource(mAppContext.getResources(), lGhostImageId);
                    if(lScaledGhostImage != null) {
                        mGhostBitmap = scaleWithAspectRatio(lScaledGhostImage);
                        if(mGhostBitmap != null) {
                            if (shouldRotateGhostImage()) {
//                                mTopBarHelpButton.setVisibility(View.GONE);
                                mFrontCaptureHintTextView.setVisibility(View.VISIBLE);
                                topGhostView.setVisibility(View.VISIBLE);
                                bottomGhostView.setVisibility(View.VISIBLE);
                                leftGhostView.setVisibility(View.GONE);
                                rightGhostView.setVisibility(View.GONE);

                                backConstraintLayout.setVisibility(View.GONE);
                                helpConstraintLayout.setVisibility(View.GONE);
                                topBarConstraintLayout.setVisibility(View.VISIBLE);
                            } else {
                                mGhostBitmap = JPEGProcessor.rotateBitmap(mGhostBitmap, -90);
//                                mTopBarHelpButton.setVisibility(View.VISIBLE);
                                mFrontCaptureHintTextView.setVisibility(View.GONE);
                                topGhostView.setVisibility(View.GONE);
                                bottomGhostView.setVisibility(View.GONE);
                                leftGhostView.setVisibility(View.VISIBLE);
                                rightGhostView.setVisibility(View.VISIBLE);

                                backConstraintLayout.setVisibility(View.VISIBLE);
                                helpConstraintLayout.setVisibility(View.VISIBLE);
                                topBarConstraintLayout.setVisibility(View.GONE);
                            }

                            //adjust capture button
                            mGhostImage.setImageBitmap(mGhostBitmap);

//                            // make capture button a third of the ghost image's height, because aesthetics.
//                            int adjustedHeight = (int)(Math.min(mGhostBitmap.getHeight(), mGhostBitmap.getWidth()) * .25);
//                            RelativeLayout.LayoutParams currentParams = (RelativeLayout.LayoutParams)mCaptureButton.getLayoutParams();
//                            currentParams.width = adjustedHeight;
//                            currentParams.height = adjustedHeight;
//                            mCaptureButton.setLayoutParams(currentParams);
//
//                            if (Utils.getDeviceBasicOrientation(mAppContext) == Configuration.ORIENTATION_LANDSCAPE) {
//                                double offset = ((mDisplayWidth/2)-(mGhostBitmap.getWidth()/2))-(adjustedHeight/2);
//                                LayoutParams params = (RelativeLayout.LayoutParams)mCaptureButton.getLayoutParams();
//                                double bottomOffset = ((double)mDisplayHeight / 2) - ((double)params.height / 2);
//                                params.setMargins(0, 0, (int)offset, (int)bottomOffset);
////                                params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE); // TODO KW 2018-06-06:  why doesn't this work?
//                                mCaptureButton.setLayoutParams(params);
//                            } else {
//                                LayoutParams params = (RelativeLayout.LayoutParams)mCaptureButton.getLayoutParams();
//                                RelativeLayout.LayoutParams logoLayoutParams = (LayoutParams) mPoweredLogo.getLayoutParams();
//                                int topOfLogo = logoLayoutParams.height;
//                                double bottomAreaHeight = (double)(mDisplayHeight - mGhostBitmap.getHeight()) / 2;
//                                double bottomOfButtonOffset = ((bottomAreaHeight - topOfLogo) / 2) - ((double)params.height / 2) + topOfLogo;
//                                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
////                                params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE); // TODO KW 2018-06-06:  why doesn't this work?
//                                double rightOffset = (double)mDisplayWidth / 2 - ((double)params.width / 2);
//                                params.setMargins(0, 0, (int)rightOffset, (int)bottomOfButtonOffset);
//                                mCaptureButton.setLayoutParams(params);
//                            }
//
//                            //whether to show the MiSnap button
//                            if (!mCameraParamMgr.isCurrentModeVideo() || ALWAYS_SHOW_MANUAL_CAPTURE_BUTTON) {
//                                mCaptureButton.setVisibility(View.VISIBLE);
//                            } else {
//                                mCaptureButton.setVisibility(View.GONE);
//                            }
                        }
                        lScaledGhostImage = null;
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                    mGhostImage.setImageDrawable(mAppContext.getResources().getDrawable(lGhostImageId));
                }
                //add the animation effects to it
				mAnimationFadeOut = AnimationUtils.loadAnimation(mAppContext, R.anim.misnap_fadeout);
				mAnimationFadeIn = AnimationUtils.loadAnimation(mAppContext, R.anim.misnap_fadein);
                mDocAnimation = AnimationUtils.loadAnimation(mAppContext, R.anim.misnap_balloon_animation);
			}
            if (mGhostText != null){
                mGhostText.setVisibility(View.INVISIBLE); //this call is must otherwise animation won't work as the view had never been rendered
                //set content description for talk back
                Spanned lGhostImageTxt = Html.fromHtml(mAppContext.getResources().getString(getGhostImageAccessibilityTextId()));
                if (lGhostImageTxt != null) {
                    mGhostImage.setContentDescription(lGhostImageTxt.toString());

                    if (!mDocType.isPassport()) {
                        mGhostText.setText(lGhostImageTxt);
                    }
                }
            }
		}		
	}

    private int getGhostImageDrawableId() {
        int rGhostImageId=-1;
        boolean fileFound=true;

        if (mDocType.isBillPay()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_remittance_ux2;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_remittance_ux2;
            }
        } else if (mDocType.isCheckBack()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_checkback_ux2;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_checkback_ux2;
            }
        } else if (mDocType.isCheckFront()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_checkfront_ux2;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_checkfront_ux2;
            }
        } else if (mDocType.isBalanceTransfer()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_balance_transfer_ux2;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_balance_transfer_ux2;
            }
        } else if (mDocType.isLicense()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.ghost_id_card_portrait;
            } else {
                rGhostImageId = R.drawable.ghost_id_card_portrait;
            }
        } else if (mDocType.isIdCardFront()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.ghost_id_card_portrait;
            } else {
                rGhostImageId = R.drawable.ghost_id_card_portrait;
            }
        } else if (mDocType.isIdCardBack()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.ghost_id_card_portrait;
            } else {
                rGhostImageId = R.drawable.ghost_id_card_portrait;
            }
        } else if (mDocType.isAutoInsurance()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_auto_insurance_card_ux2;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_auto_insurance_card_ux2;
            }
        } else if (mDocType.isW2()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_w2_ux2;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_w2_ux2;
            }
        } else if (mDocType.isPassport()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                rGhostImageId = R.drawable.misnap_ghost_passport_ux2;
            } else {
                rGhostImageId = R.drawable.misnap_manual_ghost_passport_ux2;
            }
        } else if (mDocType.isBusinessCard()) {
            rGhostImageId = R.drawable.misnap_ghost_business_card_ux2;
        } else if (mDocType.isVin()) {
            rGhostImageId = R.drawable.misnap_manual_ghost_vin_ux2;
        } else {
            rGhostImageId = R.drawable.misnap_ghost_check_blank_ux2;
        }

        if(rGhostImageId <= 0){//if the drawable doesn't exists, use the standard one
            rGhostImageId = R.drawable.misnap_ghost_check_blank_ux2;
        }

        return rGhostImageId;
    }

    private Bitmap scaleWithAspectRatio(Bitmap image){
        if (image != null) {
            // Scale the bitmap width based on the parameter, and maintain the image aspect ratio.
            int minFill;
            int longSide;

            if (shouldRotateGhostImage()) {
                minFill = getGhostLength();
                longSide = getHeight();
            } else {
                if (Utils.getDeviceBasicOrientation(mAppContext) == Configuration.ORIENTATION_PORTRAIT) {
                    minFill = mScienceParamMgr.getPortraitHorizontalFillMin();
                    longSide = getWidth();
                }else {
                    minFill = getGhostLength();
                    longSide = getWidth();
                }
            }

            double scaledImageWidth = longSide * minFill / 1000;
            scaledImageWidth *= GHOST_IMAGE_ADDITIONAL_SCALE_FACTOR;
            image = Bitmap.createScaledBitmap(
                    image,
                    (int) scaledImageWidth,
                    (int) scaledImageWidth * image.getHeight() / image.getWidth(),
                    true);
        }

        return image;
    }

    private int getGhostLength() {
        if (mDocType.isIdDocument()) {
            return 610;
        } else if (mDocType.isPassport()) {
            return 660;
        } else {
            return mScienceParamMgr.getHorizontalFillMin();
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    void setPreviewParameters() {
        try {
            CameraInfoCacher cacher = new CameraInfoCacher(mAppContext, mCameraParamMgr.getUseFrontCamera());
            mPreviewWidth = Integer.parseInt(cacher.getPreviewWidth());
            mPreviewHeight = Integer.parseInt(cacher.getPreviewHeight());

            // For portrait mode, we need to switch the width and height,
            // since preference does not know orientation
            if (Utils.getDeviceBasicOrientation(mAppContext) == Configuration.ORIENTATION_PORTRAIT) {
                int temp = mPreviewWidth;
                mPreviewWidth = mPreviewHeight;
                mPreviewHeight = temp;
            }
        } catch (NumberFormatException e) {
            // Swallow NumberFormatExceptions that occur in unit tests because ParameterManager is mocked
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Only draw the glare outline while the hint message is appearing
        if (DRAW_REALTIME_GLARE_OUTLINE && wasGlareFound) {
            boolean isGlareHintMessageShowing = (mBalloonStringResIdThatIsShowing == R.string.misnap_glare_ux2)
                    && (mBalloonImage != null)
                    && (mBalloonImage.getVisibility() == VISIBLE);
            if (isGlareHintMessageShowing) {
                drawGlareRectangle(canvas);
            }
        }

        if(drawFinalFrame) {
            drawFinalFrameOnCanvas(canvas);
        }

        if (drawDetectedRectangle) {
            //draw the rectangle
            drawQuoteRectangleUnquote(canvas);
        }
    }

    private void drawFinalFrameOnCanvas(Canvas lCanvas) {
        if(mFinalFrameArray != null && mFinalFrameArray.length > 0 && lCanvas != null) {
            try {
                Rect destRect = new Rect(0, 0, lCanvas.getWidth(), lCanvas.getHeight());
                if(mSnappedDoc == null) {
                    mSnappedDoc = BitmapFactory.decodeByteArray(mFinalFrameArray, 0, mFinalFrameArray.length);
                }
                Rect srcRect = new Rect(0, 0, mSnappedDoc.getWidth(), mSnappedDoc.getHeight());
                if(mSnappedDoc != null) {
                    lCanvas.drawBitmap(mSnappedDoc, srcRect, destRect, mDetectedRectanglePaint);
                }
            } catch (Exception e) {
//                e.printStackTrace();
            }
        } else {
            if (lCanvas != null) {
                drawDetectedRectangle = false;
                // TODO: Why does the final camera preview image stay on screen? Overwrite it w/ black.
                Paint blackPaint = new Paint();
                blackPaint.setColor(0xFF000000);
                lCanvas.drawRect(0, 0, lCanvas.getWidth(), lCanvas.getHeight(), blackPaint);
            }
        }
    }

    private void drawQuoteRectangleUnquote(Canvas canvas) {
        composeRectanglePath(mRectangleAnimationPoints);
        if (mManualCapturePleaseWaitDialog != null && mManualCapturePleaseWaitDialog.isShowing()) {
            return;
        }
        canvas.drawPath(mDetectedRectanglePath, mDetectedRectanglePaint);
    }

    private void composeRectanglePath(List<android.graphics.Point> rect) {
        mDetectedRectanglePath.reset();

        if(rect == null || rect.size() <= 3) {		// Eliminate the rectangle
            mDetectedRectanglePath.moveTo(0f, 0f);
            mDetectedRectanglePath.lineTo(0f, 0f);
            return; // Can't make a polygon with less than 3 points
        }

        mDetectedRectanglePath.moveTo((float) rect.get(0).x, (float) rect.get(0).y);
        for(int i = 1; i < rect.size(); i++) {
            mDetectedRectanglePath.lineTo((float) rect.get(i).x, (float) rect.get(i).y);
        }
        mDetectedRectanglePath.close();
    }

    public void showGhostImage() {
        if (mGhostImage == null || mGhostText == null) {
            return;
        }
        Log.d(TAG, "showGhostImage(): " + mGhostAnimationRunning + "-mGhostImage.isShown():" + mGhostImage.isShown());

        //check conditions valid to show the ghost image
		if (!mGhostImage.isShown() && !mGhostText.isShown() && !mGhostAnimationRunning &&
                (GHOST_IMAGE_ENABLED || GHOST_IMAGE_ALWAYS_ON)) {

            Log.d(TAG, "ghost image was not showing;");
			mGhostImage.startAnimation(mAnimationFadeIn);
            // use the drawable's dimensions instead of the view's because the view never updates its width/height on rotation
            mGhostText.setWidth(mGhostImage.getDrawable().getIntrinsicWidth());
            mGhostText.setHeight(mGhostImage.getDrawable().getIntrinsicHeight());
            mGhostText.setMaxLines(2);
            mGhostText.setMaxTextSize((int) getResources().getDimension(R.dimen.misnapworkflow_help_font_size));
            mGhostText.startAnimation(mAnimationFadeIn);
            Log.d(TAG, "mGhostAnimationRunning set to true");
			mGhostAnimationRunning = true;

            if (mGhostImage != null) {
                mGhostImage.setVisibility(View.VISIBLE);
            }
            if (mGhostText != null){
                mGhostText.setVisibility(View.VISIBLE);
            }

            //uxp event
            MibiData.getInstance().addUXPEvent(UxpConstants.MISNAP_UXP_GHOST_IMAGE_BEGINS);
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    mGhostAnimationRunning = false;
                    Log.d(TAG, "mGhostAnimationRunning set to false");

                    //play the Accessibility message after 2 seconds
                    mHandler.postDelayed(mGhostImageAccessMessage, 2000);
                }
            }, mAnimationFadeIn.getDuration());

            postInvalidate();
        }
    }

    public void removeGhostImage(boolean bImmediateHide) {

        if (mGhostImage == null || mGhostText == null ||
                mAnimationFadeOut == null) {
            return;
        }

        Log.d(TAG, "mGhostImage.isShown(): " + mGhostImage.isShown() + ":mGhostAnimationRunning:" + mGhostAnimationRunning);

        if (mGhostImage.isShown() && mGhostText.isShown() && !mGhostAnimationRunning &&
                (!GHOST_IMAGE_ALWAYS_ON)) {

            Log.d(TAG, "removeGhostImage():mGhostAnimationRunning:" + mGhostAnimationRunning);        // removed by proguard

            //if visible, only then hide it
            if (bImmediateHide) {
                //hide it
                mGhostImage.setVisibility(View.INVISIBLE);
                mGhostText.setVisibility(View.INVISIBLE);
            } else {
                if (!mGhostAnimationRunning) {
                    mGhostImage.startAnimation(mAnimationFadeOut);
                    mGhostText.startAnimation(mAnimationFadeOut);
                    mGhostAnimationRunning = true;
                    Log.d(TAG, "mGhostAnimationRunning set to true2");
                    //uxp event
                    MibiData.getInstance().addUXPEvent(UxpConstants.MISNAP_UXP_GHOST_IMAGE_ENDS);
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            Log.d(TAG, "mGhostAnimationRunning set to false2");
                            mGhostAnimationRunning = false;
                            if (mGhostImage != null) {
                                mGhostImage.setVisibility(View.INVISIBLE);
                            }
                            if (mGhostText != null){
                                mGhostText.setVisibility(View.INVISIBLE);
                            }
                        }
                    }, mAnimationFadeOut.getDuration());
                }
            }

            postInvalidate();
        }
    }

    // Timeout for final animation to complete - remove with real animation
    Runnable mGhostImageAccessMessage = new Runnable() {
        @Override
        public void run() {
            Log.d("MiSnapAnim", "playGuideImgTalkBackMsg");
            // In case Accessbility is turned on
            if(!mGhostMsgTTSPlayed) {
                playGuideImgTalkBackMsg();
                mGhostMsgTTSPlayed = true;
            }
        }
    };

    public void resetVariables() {
        //stop all animations
        if(mBugSequence != null){
            mBugSequence.stop();
        }
        mGhostAnimationRunning=false;
        drawDetectedRectangle = false;
        mBubblesDelayInProgress=false;
        mGhostMsgTTSPlayed = false;
        if (mBalloonImage != null) {
            mBalloonImage.setVisibility(View.INVISIBLE);
        }
    }

    public void cleanup() {
        postInvalidate(); // Prevents a black box from appearing where the bug animation was on the Galaxy S3
		mGhostAnimationRunning = false;
        stopOrientationListener();
        resetVariables();
        if (mCurrentHintBubble != null) {
            mCurrentHintBubble.clearBubbleAnimation();
        }
        if (mManualCapturePleaseWaitDialog != null) {
            mManualCapturePleaseWaitDialog.dismiss();
        }
        mHandler.removeCallbacksAndMessages(null); // Specifying a null token removes ALL callbacks and messages
        if(EventBus.getDefault().isRegistered(this)) {
            //unregister the event bus
            try {
                EventBus.getDefault().unregister(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(mGhostBitmap != null) {
//            mGhostBitmap.recycle();
            mGhostBitmap = null;
        }
        if(mGhostImage != null) {
            mGhostImage.setImageResource(0);
            mGhostImage.setImageDrawable(null);
            mGhostImage.setImageResource(android.R.color.transparent);
            mGhostImage = null;
        }
        if(mSnappedDoc != null) {
            mSnappedDoc = null;
        }
        if(mVignetteImage != null) {
            mVignetteImage.setImageResource(0);
            mVignetteImage.setImageDrawable(null);
            mVignetteImage.setImageResource(android.R.color.transparent);
            mVignetteImage = null;
        }
        if(mBalloonImage != null) {
            mBalloonImage.setText("");
            mBalloonImage = null;
        }

        if(mBugImage != null) {
            mBugImage = null;
        }
        if(mBugBackground != null){
            mBugBackground = null;
        }
        if(mFlashToggle != null) {
            mFlashToggle.clearComposingText();
            mFlashToggle.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            mFlashToggle.setBackgroundColor(Color.TRANSPARENT);
//            mFlashToggle.setImageResource(0);
//            mFlashToggle.setImageDrawable(null);
//            mFlashToggle.setImageResource(android.R.color.transparent);
            mFlashToggle = null;
        }

        if(mHelpButton != null) {
//            mHelpButton.setImageResource(0);
//            mHelpButton.setImageDrawable(null);
//            mHelpButton.setImageResource(android.R.color.transparent);
            mHelpButton = null;
        }

//        if(mCaptureButton != null) {
//            mCaptureButton.setImageResource(0);
//            mCaptureButton.setImageDrawable(null);
//            mCaptureButton.setImageResource(android.R.color.transparent);
//            mCaptureButton = null;
//        }
        if(mPoweredLogo != null) {
            mPoweredLogo.setImageResource(0);
            mPoweredLogo.setImageDrawable(null);
            mPoweredLogo.setImageResource(android.R.color.transparent);
            mPoweredLogo = null;
        }

        // Moved from seeIfAnimationsDone
        mBugSequence = null;
        mFinalFrameArray = null;
        //clear the Activity handles
        //don't need to null out the application context, can't leak it
//        mAppContext=null;
        //TODO:clean up instructionText
        System.gc();    //immediately clears the fragmented and unreferenced memory chunks out
	}

	public void toggleTorch(boolean bTorchState) {
		if (bTorchState) {
            mFlashToggle.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.misnap_icon_flash_on_ux2), null, null, null);
            mFlashToggle.setText(getResources().getText(R.string.flash_on_ux2));
            mFlashToggle.setTextColor(getResources().getColor(R.color.misnap_flash_on_red_ux2));
//			mFlashToggle.setImageResource(R.drawable.misnap_icon_flash_on_ux2);
            //some customers are requesting that flash state should also be read
            mFlashToggle.setContentDescription(mAppContext.getString(R.string.misnap_overlay_flash_on_ux2));
            mFlashToggle.setTag(TAG_TORCH_ON);
            EventBus.getDefault().post(new TextToSpeechEvent(R.string.misnap_overlay_flash_on_ux2));
		} else {
            mFlashToggle.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.misnap_icon_flash_off_ux2), null, null, null);
            mFlashToggle.setText(getResources().getText(R.string.flash_off_ux2));
            mFlashToggle.setTextColor(getResources().getColor(R.color.misnap_flash_off_gray_ux2));
//			mFlashToggle.setImageResource(R.drawable.misnap_icon_flash_off_ux2);
            mFlashToggle.setContentDescription(mAppContext.getString(R.string.misnap_overlay_flash_off_ux2));
            mFlashToggle.setTag(TAG_TORCH_OFF);
            EventBus.getDefault().post(new TextToSpeechEvent(R.string.misnap_overlay_flash_off_ux2));
        }
	}

	public void showFPSData(String sData) {
		if (sData != null && testText != null) {
			testText.setVisibility(View.VISIBLE);
			testText.setText(sData);
		}
	}

    public void hideButtons() {
        if(mFlashToggle != null) {
            mFlashToggle.setVisibility(View.GONE);
        }
        if(mHelpButton != null) {
            mHelpButton.setVisibility(View.GONE);
        }
//        if(mCancelButton != null) {
//            mCancelButton.setVisibility(View.GONE);
//        }
//        if(mCaptureButton != null) {
//            mCaptureButton.setVisibility(View.GONE);
//        }
        if(mPoweredLogo != null) {
            mPoweredLogo.setVisibility(View.GONE);
        }
    }

    private List<Point> clonePoints(List<Point> points) {
        List<Point> out = new ArrayList<>();
        for(int i=0; i<4; i++) {
            Point p = points.get(i);
            out.add(new Point(p.x, p.y));
        }
        return out;
    }

    private Point _mTarget = null;
    private Point docCenter(List<android.graphics.Point> points) {
        if (null == _mTarget)
            _mTarget = new Point();	// Reduce allocations during operation

        _mTarget.x
                = (points.get(0).x + points.get(1).x + points.get(2).x + points.get(3).x) >> 2;
        _mTarget.y
                = (points.get(0).y + points.get(1).y + points.get(2).y + points.get(3).y) >> 2;
        return _mTarget;
    }

    private void seeIfAnimationsDone() {
        if (!isBugAnimationRunning()&& !mAllDone) {
            mAllDone = true;
        } else {
            Log.d("MiSnapAnim", "waiting to be done");	// removed by proguard
        }
    }

    public void drawReplayFrame(byte[] finalFrame) {
        mFinalFrameArray = finalFrame;
        if (mFinalFrameArray != null) {
            mSnappedDoc = null; // reload the latest frame
            drawFinalFrame = true;
        }

        postInvalidate(); // Redraw needed
    }

    private void drawRectangle(@Nullable List<Point> points) {
        Log.d("MiSnapAnim", "drawRectangle - start");
        if(!mCameraParamMgr.isCurrentModeVideo()) {
            return;
        }

        if(mHandler != null) {
            if (mCurrentHintBubble != null) {
                mCurrentHintBubble.clearBubbleAnimation();
            }
            //clear the balloon animation callback
            mHandler.removeCallbacks(mBalloonCheckRunner);
        }

        int[][] lCorner = new int[4][2];
        if (null == points) {
            // If the four corners are null, set them to the outside of the camera preview
            lCorner[0] = new int[]{0, 0};
            lCorner[1] = new int[]{mPreviewWidth, 0};
            lCorner[2] = new int[]{mPreviewWidth, mPreviewHeight};
            lCorner[3] = new int[]{0, mPreviewHeight};
        } else {
            for (int i = 0; i < points.size(); ++i) {
                lCorner[i] = new int[]{points.get(i).x, points.get(i).y};
            }
        }

        double displayToPreviewRatioX = (double) mDisplayWidth / mPreviewWidth;
        double displayToPreviewRatioY = (double) mDisplayHeight / mPreviewHeight;

        detectedDocumentPoints.clear();
        for (int i = 0; i < 4; ++i) {
            detectedDocumentPoints.add(new Point(
                    (int) (lCorner[i][0] * displayToPreviewRatioX),
                    (int) (lCorner[i][1] * displayToPreviewRatioY)));
        }

        // Calculate document center
        mTarget = docCenter(detectedDocumentPoints);
        mRectangleAnimationPoints = clonePoints(detectedDocumentPoints);
        mRectangleAnimationStart = System.currentTimeMillis();
        mHandler.post(mRectangleAnimationRunner);
    }

    // Run the rectangle animation once
    private Runnable mRectangleAnimationRunner = new Runnable() {
        @Override
        public void run() {
            long delta = System.currentTimeMillis() - mRectangleAnimationStart;
            mHandler.postDelayed(mRectangleAnimationRunner, RECTANGLE_ANIMATION_INTERVAL_MILLIS);
            drawDetectedRectangle = true;
            float s = Math.abs(RECTANGLE_ANIMATION_LENGTH_MILLIS/2 - delta);
            s = Math.abs( s / (RECTANGLE_ANIMATION_LENGTH_MILLIS/2));
            if (delta < (RECTANGLE_ANIMATION_LENGTH_MILLIS)) {
                s = 1f - ((1f - s)*(1f - RECTANGLE_ANIMATION_MIN_SCALE));
                // slightly enlarge each call
                mRectangleAnimationPoints = zoomRectangle(detectedDocumentPoints, s);
                postInvalidate();
            }
            else {
                // Stop this animation
                mHandler.removeCallbacks(mRectangleAnimationRunner);
                s = 1f;
                // slightly enlarge each call
                mRectangleAnimationPoints = zoomRectangle(detectedDocumentPoints, s);
                postInvalidate();
            }
        }
    };

    private List<Point> zoomRectangle(List<Point> points, float scale) {
        List<Point> newPoints = clonePoints(points);
        // Find center
        mTarget = docCenter(newPoints);

        // Find 4 lines to corners and scale
        for(Point p: newPoints) {
            int offsetX = p.x - mTarget.x;
            int offsetY = p.y - mTarget.y;
            p.x = (int) (scale*offsetX + mTarget.x);
            p.y = (int) (scale*offsetY + mTarget.y);
        }

        return newPoints;
    }

    public void snapshotGood(byte[] finalFrame, List<Point> fourCorners) {
        mHandler.removeCallbacks(mBalloonCheckRunner);
        mFrameCapturedIgnoreUXP = true;

        //show the actual frame
        drawReplayFrame(finalFrame);

        if (mCameraParamMgr.isCurrentModeVideo()) {
            removeGhostImage(true); // New in UX2
            mBalloonImage.clearAnimation();
            drawRectangle(fourCorners);
        } else {
            showManualCapturePressedPleaseWait(false);
            removeGhostImage(true);
        }

        // Do final bug animation always
        startBugFinalAnimation();
    }

    public void drawDocumentCenter(int[][] l4Corners) {
        if(l4Corners != null) {
            mDocumentCorner1 = l4Corners[0];
            mDocumentCorner2 = l4Corners[1];
            mDocumentCorner3 = l4Corners[2];
            mDocumentCorner4 = l4Corners[3];

            double displayToPreviewRatioX = (double) mDisplayWidth / mPreviewWidth;
            double displayToPreviewRatioY = (double) mDisplayHeight / mPreviewHeight;
            if(detectedDocumentPoints != null) {
                detectedDocumentPoints.clear();
                //first point
                detectedDocumentPoints.add(new Point((int) (mDocumentCorner1[0] * displayToPreviewRatioX),
                        (int) (mDocumentCorner1[1] * displayToPreviewRatioY)));
                detectedDocumentPoints.add(new Point((int) (mDocumentCorner2[0] * displayToPreviewRatioX),
                        (int) (mDocumentCorner2[1] * displayToPreviewRatioY)));
                detectedDocumentPoints.add(new Point((int) (mDocumentCorner3[0] * displayToPreviewRatioX),
                        (int) (mDocumentCorner3[1] * displayToPreviewRatioY)));
                detectedDocumentPoints.add(new Point((int) (mDocumentCorner4[0] * displayToPreviewRatioX),
                        (int) (mDocumentCorner4[1] * displayToPreviewRatioY)));
            }

            // Calculate document center
            mTarget = docCenter(detectedDocumentPoints);
//            Log.i("Target","target.x:" +mTarget.x+ " :TArget.y:" + mTarget.y);
            //update the view
            postInvalidate();
        }
    }

    final Runnable mBalloonCheckRunner = new Runnable() {
        @Override
        public void run() {
            Log.d("MiSnapAnim", "mBalloonCheckRunner - baloon timer over");
            mBubblesDelayInProgress=false;
        }
    };

    private boolean isBugAnimationRunning() {
        return null != mBugSequence && mBugSequence.isRunning();
    }

    Runnable mBugAnimationRunner = new Runnable() {
        @Override
        public void run() {
            if (isBugAnimationRunning()) {
                mBugSequence.stop();
                Log.d("MiSnapAnim", "bugSequence.stop()");		// removed by proguard
            } else {
                Log.d("MiSnapAnim", "bugSequence finished");	// removed by proguard
            }
            seeIfAnimationsDone();
        }
    };

    private void startBugFinalAnimation() {
        if (isBugAnimationRunning()) {
            return;
        }
        mBugImage.setVisibility(View.VISIBLE);
        mBugBackground.setVisibility(View.VISIBLE);
        mHandler.postDelayed(mBugAnimationRunner, BUG_ANIMATION_TIME_MS);	// Trigger the stop

        if (ANIMATED_BUG) {
            mBugSequence = MiSnapAnimation.createBugAnim(mBugImage, mAppContext);
        } else {
            mBugSequence = MiSnapAnimation.createBugStill(mBugImage, mAppContext);
        }
        Log.d("MiSnapAnim", "bugSequence.start()");	// removed by proguard
        EventBus.getDefault().post(new TextToSpeechEvent(getResources().getString(R.string.misnap_bug_message_ux2)));
        mBugSequence.start();
    }

    void playGuideImgTalkBackMsg() {
        int spokenTextId = getGhostImageAccessibilityTextId();

        EventBus.getDefault().post(new TextToSpeechEvent(spokenTextId));
    }

    int getGhostImageAccessibilityTextId() {
        int spokenAccessibilityTextId;

        if (mDocType.isBillPay()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_remittance_ux2;
            } else {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_remittance_manual_ux2;
            }
        } else if (mDocType.isCheckBack()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_check_ux2;
            } else {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_check_manual_ux2;
            }
        } else if (mDocType.isCheckFront()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_check_ux2;
            } else {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_check_manual_ux2;
            }
        } else if (mDocType.isBalanceTransfer()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_remittance_ux2;
            } else {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_remittance_manual_ux2;
            }
        } else if (mDocType.isLicense()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_drivers_license_ux2;
            } else {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_drivers_license_manual_ux2;
            }
        } else if (mDocType.isIdCardFront()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_id_card_ux2;
            } else {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_id_card_manual_ux2;
            }
        } else if (mDocType.isIdCardBack()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_id_card_ux2;
            } else {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_id_card_manual_ux2;
            }
        } else if (mDocType.isAutoInsurance()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
            spokenAccessibilityTextId = R.string.misnap_ghost_image_insurance_card_ux2;
            } else {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_insurance_card_manual_ux2;
            }
        } else if (mDocType.isVin()) {
            spokenAccessibilityTextId = R.string.misnap_ghost_image_vin_manual_ux2;
        } else if (mDocType.isW2()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_w2_ux2;
            } else {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_w2_manual_ux2;
            }
        } else if (mDocType.isPassport()) {
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_passport_ux2;
            } else {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_passport_manual_ux2;
            }
        }else {
            //default case
            if (mCameraParamMgr.isCurrentModeVideo()) {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_document_portrait_ux2;
            } else {
                spokenAccessibilityTextId = R.string.misnap_ghost_image_document_portrait_manual_ux2;
            }
        }
        //set content description for talk back
        return spokenAccessibilityTextId;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OnTorchStateEvent event){
        if ("SET".equals(event.function) || "GET".equals(event.function)) {
            mTorchStatus = event.currentTorchState == 1;
            // use the correct ghost image
            toggleTorch(mTorchStatus);
        }
    }

    public boolean getTorchStatus() {
        return mTorchStatus;
    }

    // TODO KW 2017-08-14:  why is there no OnCapturedFrameEvent as in UX1?
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MiSnapAnalyzerResult event) {
        // Processing is done on the manually captured frame now. Don't show any UI for it tho.
        if (!mCameraParamMgr.isCurrentModeVideo()) {
            return;
        }

        if (DRAW_REALTIME_DOC_OUTLINE) {
            drawFourCorners(event.getFourCorners());
        }
        boolean fourCornersFound = event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.FOUR_CORNER_CONFIDENCE);
        boolean isCloseEnough = event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.HORIZONTAL_MINFILL);
        boolean isDocCentered = event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.MIN_PADDING);

        // Always show the doc center
        drawDocumentCenter(event.getFourCorners());
        // only display glare box if four corners were found
        if (fourCornersFound) {
            updateGlareRectangle(event.getGlareRect(), !event.getCheckPassed(MiSnapAnalyzerResult.FrameChecks.GLARE));
        }

        for (HintBubble hint : mHintBubbles) {
            // skip glare check if four corners weren't found
            if (MiSnapAnalyzerResult.FrameChecks.GLARE == hint.checkThatNeedsToFail && !fourCornersFound) {
                continue;
            }

            // the cases where we don't want to display a hint bubble are:
            // * it's a hint bubble for low contrast or busy background, AND:
            // * 4c confidence is high (since the reason to let the user know about low contrast or busy background is because 4c confidence is low), AND:
            // * the min padding threshold has been met
            if (hint.onlyCheckIfFourCornersNotFound && fourCornersFound && isDocCentered) {
                continue;
            }

            if (!event.getCheckPassed(hint.checkThatNeedsToFail)) {
                mCurrentHintBubble = hint;
                hint.startBalloonOpenAnimation();
                return;
            }
        }
        // All passed!
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OnCapturedFrameEvent event) {
        mFrameCapturedIgnoreUXP = true;

        hideButtons();

        // Pick apart the event result to find the captured image and potentially the four corners
        Intent returnIntent = event.returnIntent;
        byte[] capturedImage = returnIntent.getByteArrayExtra(MiSnapApi.RESULT_PICTURE_DATA);
        ArrayList<Point> points = returnIntent.getParcelableArrayListExtra(MiSnapApi.RESULT_FOUR_CORNERS);
        snapshotGood(capturedImage, points);

        // tell UI manager to do vibrate
        // TODO KW 2018-06-01:  need to remove sendMsgToUIFragment entirely; this is in camera, too.
        Utils.sendMsgToUIFragment(mAppContext,
                WorkflowConstants.UI_DO_VIBRATE, null, null, null, null, null);
    }

    final boolean SCALE = true;
    private void drawFourCorners(int[][] fourCorners) {
        debugOutlineCorners.clear();

        double displayToPreviewRatioX;
        double displayToPreviewRatioY;
        if (SCALE) {
            displayToPreviewRatioX = (double) mDisplayWidth / mPreviewWidth;
            displayToPreviewRatioY = (double) mDisplayHeight / mPreviewHeight;
        } else {
            displayToPreviewRatioX = 1.0d;
            displayToPreviewRatioY = 1.0d;
        }

        for (int i = 0; i < 4; i++) {
            debugOutlineCorners.add(new Point(
                    (int)(fourCorners[i][0] * displayToPreviewRatioX),
                    (int)(fourCorners[i][1] * displayToPreviewRatioY)));
        }

        Log.w("Scale", "X="+displayToPreviewRatioX + ", Y="+displayToPreviewRatioY);
        Log.w("Points", debugOutlineCorners.get(0).x + "," + debugOutlineCorners.get(0).y + "-" + debugOutlineCorners.get(2).x + "," + debugOutlineCorners.get(2).y);

        mHandler.post(mPerFrameFourCornersRunner);
    }

    private int[][] scaleGlareCoordinatesToScreen(int[][] unscaled){

        int[][] result = new int[2][2];
        double displayToPreviewRatioX = (double) mDisplayWidth / mPreviewWidth;
        double displayToPreviewRatioY = (double) mDisplayHeight / mPreviewHeight;

        for (int i = 0; i < 2; i++) {
            result[i][0] = (int)(unscaled[i][0] * displayToPreviewRatioX);
            result[i][1] = (int)(unscaled[i][1] * displayToPreviewRatioY);
        }

        return result;
    }

    private Runnable mPerFrameFourCornersRunner = new Runnable() {
        @Override
        public void run() {
            drawDetectedRectangle = true;
            mRectangleAnimationPoints = zoomRectangle(debugOutlineCorners, 1f);
            postInvalidate();
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OnStartedEvent event) {
        showGhostImage();
    }

    private void updateGlareRectangle(int[][] rect, boolean wasGlareFound){
        int[][] scaledRect = scaleGlareCoordinatesToScreen(rect);
        glareBox = new Rect(scaledRect[0][0], scaledRect[0][1], scaledRect[1][0], scaledRect[1][1]);
        glareBox.sort();
        this.wasGlareFound = wasGlareFound;
        postInvalidate();
    }

    private void drawGlareRectangle(Canvas canvas){
        if(glareBox != null){
            canvas.drawRect(glareBox, mDetectedRectanglePaint);
        }
    }

    @Deprecated
    protected void startBalloonOpenAnimation(int id) {
    }

    protected class HintBubble {
        MiSnapAnalyzerResult.FrameChecks checkThatNeedsToFail;
        int speechResId;
        boolean onlyCheckIfFourCornersNotFound; // this hint is relevant only if four corners weren't found

        public HintBubble(MiSnapAnalyzerResult.FrameChecks checkThatNeedsToFail, int balloonDrawableResId, int speechResId) {
            this.checkThatNeedsToFail = checkThatNeedsToFail;
            this.speechResId = speechResId;
            this.onlyCheckIfFourCornersNotFound = false;
        }

        public HintBubble(MiSnapAnalyzerResult.FrameChecks checkThatNeedsToFail, int speechResId) {
            this.checkThatNeedsToFail = checkThatNeedsToFail;
            this.speechResId = speechResId;
            this.onlyCheckIfFourCornersNotFound = false;
        }

        void startBalloonOpenAnimation() {
            // Check if bubbles are disabled or if the required delay to show the next bubble
            // has not timed out yet
            if ( !SMART_HINT_ENABLED ||
                    mBubblesDelayInProgress ||
                    mGhostAnimationRunning ) {
                return;
            }
            Log.i("Target","startBalloonOpenAnimation start");
            //get the right bubble to show
            mBalloonStringResIdThatIsShowing = speechResId;

            mBalloonImage.setText(getContext().getString(speechResId));
//            mBalloonImage.setImageResource(mBalloonStringResIdThatIsShowing);
//        MarginLayoutParams params=(MarginLayoutParams )mBalloonImage.getLayoutParams();
//        params.leftMargin=(int) xOffset;
//        //here 100 means 100px,not 80% of the width of the parent view
//        //you may need a calculation to convert the percentage to pixels.
//        mBalloonImage.setLayoutParams(params);
            // setup help animation
            mDocAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    mBalloonImage.setVisibility(View.VISIBLE);
                    //when the animation starts, if talkback is enabled, make the required sound
                    EventBus.getDefault().post(new TextToSpeechEvent(speechResId));
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mBalloonImage != null) {
                        mBalloonImage.setVisibility(View.INVISIBLE);
                    }
                    mHandler.postDelayed(mBalloonCheckRunner, mWorkflowParamMgr.getSmartHintUpdatePeriod());
                }

                @Override
                public void onAnimationRepeat(Animation arg0) {
                }
            });
            mBalloonImage.setAnimation(mDocAnimation);
            mBalloonImage.startAnimation(mDocAnimation);
            //refresh the view
//        postInvalidate();
            mBubblesDelayInProgress = true;
        }

        private void clearBubbleAnimation() {
            Log.d("clearBubbleAnimation", "clearBubbleAnimation - start");
            try {
                removeBubbleAnimation();

                //clear the animation listeners as well
                if(mDocAnimation != null) {
                    mDocAnimation.setAnimationListener(null);
                }
                CameraOverlay.this.postInvalidate();
            } catch (Exception e) {
                //log
            }
        }

        private void removeBubbleAnimation() {
            try {
                //remove any bubbles
                if(mBalloonImage != null) {
                    mBalloonImage.clearAnimation();
                    mBalloonImage.setVisibility(View.INVISIBLE);
                }
            } catch (Exception e) {
                //log
            }
        }
    }

    private class CornerConfusionHintBubble extends HintBubble {

        CornerConfusionHintBubble(MiSnapAnalyzerResult.FrameChecks checkThatNeedsToFail, int balloonDrawableResId, int speechResId) {
            super(checkThatNeedsToFail, balloonDrawableResId, speechResId);
            this.onlyCheckIfFourCornersNotFound = true;
        }

        CornerConfusionHintBubble(MiSnapAnalyzerResult.FrameChecks checkThatNeedsToFail, int speechResId) {
            super(checkThatNeedsToFail, speechResId);
            this.onlyCheckIfFourCornersNotFound = true;
        }
    }

    void showManualCapturePressedPleaseWait(boolean show) {
        if (show) {
            mManualCapturePleaseWaitDialog.show();
        } else {
            mManualCapturePleaseWaitDialog.dismiss();
        }
    }

    public void onRotate(){
        if (mCurrentHintBubble != null) {
            mCurrentHintBubble.clearBubbleAnimation();
        }
        mBubblesDelayInProgress = false;
        mGhostAnimationRunning = false;

        updateDisplayDimensions();
    }

    private void updateDisplayDimensions() {
        mDisplayWidth = getWidth();
        mDisplayHeight = getHeight();
    }

    public void addBlackBarsIfNecessary(ScaledPreviewSizeStickyEvent event) {
//        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)getLayoutParams();
//        layoutParams.gravity = Gravity.CENTER;
//        layoutParams.width = event.getWidth();
//        layoutParams.height = event.getHeight();
//
//        setLayoutParams(layoutParams);
//        requestLayout();
//
//        mDisplayWidth = event.getWidth();
//        mDisplayHeight = event.getHeight();
    }

    private boolean shouldRotateGhostImage() {
        return ((mWorkflowParamMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_PORTRAIT_DOCUMENT_PORTRAIT
                || mWorkflowParamMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_FREE_DOCUMENT_ALIGNED_WITH_DEVICE)
                && Utils.getDeviceBasicOrientation(mAppContext) == Configuration.ORIENTATION_PORTRAIT);
    }

    @VisibleForTesting
    public Paint getAnimationRectanglePaint() {
        return mDetectedRectanglePaint;
    }
}
