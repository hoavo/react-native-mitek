package com.miteksystems.misnap.misnapworkflow_UX2.ui.overlay;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.miteksystems.misnap.barcode.events.SetBarcodeSpeedEvent;
import com.miteksystems.misnap.events.OnTorchStateEvent;
import com.miteksystems.misnap.events.ScaledPreviewSizeStickyEvent;
import com.miteksystems.misnap.events.TextToSpeechEvent;
import com.miteksystems.misnap.events.TorchStateEvent;
import com.miteksystems.misnap.misnapworkflow_UX2.R;
import com.miteksystems.misnap.params.BarcodeApi;
import com.miteksystems.misnap.params.CameraParamMgr;
import com.miteksystems.misnap.params.MiSnapApi;
import com.miteksystems.misnap.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;


/**
 * The barcode reader activity itself. This is loosely based on the
 * CameraPreview example included in the Android SDK.
 */
public class BarcodeOverlayFragment extends Fragment implements View.OnClickListener {

	private static final int SEC_TO_MS = 1000;
	private static final int TTS_DELAY_MS = 2000;
	private static final int GHOST_IMAGE_TIMEOUT = 10 * SEC_TO_MS;
	private static final boolean enableGhostImage = true;

	private ImageButton buttonFlash;
	private ImageButton buttonFlashLandscape;
	private View topGhostView;
	private View bottomGhostView;
	private Handler timeoutHandler;
	private CameraParamMgr paramMgr;

	TextView mTopBarHelpButton;
	TextView mFrontCaptureHintTextView;

	private View leftGhostView;
	private View rightGhostView;

	private ConstraintLayout backConstraintLayout;
	private ConstraintLayout helpConstraintLayout;
	private ConstraintLayout topBarConstraintLayout;

	boolean flashOn = false;
	boolean flashSupported;

	Runnable mBarcodeGhostImageTimeout = new Runnable() {
		@Override
		public void run() {
			setOverlay(true);
			EventBus.getDefault().post(new TextToSpeechEvent(R.string.misnap_ghost_barcode_tooltip_ux2, TTS_DELAY_MS));
			// Since the device struggled to read the barcode, give the analyzer more time to process each frame
			EventBus.getDefault().post(new SetBarcodeSpeedEvent(BarcodeApi.BARCODE_SPEED_SLOW));
		}
	};

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JSONObject params;
        try {
            String jobSettings = getActivity().getIntent().getStringExtra(MiSnapApi.JOB_SETTINGS);
            params = new JSONObject(jobSettings);
        } catch (Exception e) {
            params = new JSONObject();
            e.printStackTrace();
        }

        paramMgr = new CameraParamMgr(params);
    }

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.misnap_barcode_overlay_ux2, container, false);

		buttonFlash = (ImageButton) rootView.findViewById(R.id.flashButton);
		buttonFlash.setOnClickListener(this);

		buttonFlashLandscape = rootView.findViewById(R.id.flashLandscapeButton);
		buttonFlashLandscape.setOnClickListener(this);

		topGhostView = rootView.findViewById(R.id.topGhostView);
		bottomGhostView = rootView.findViewById(R.id.bottomGhostView);

		leftGhostView = rootView.findViewById(R.id.leftGhostView);
		rightGhostView = rootView.findViewById(R.id.rightGhostView);

		backConstraintLayout = rootView.findViewById(R.id.backConstraintLayout);
		helpConstraintLayout = rootView.findViewById(R.id.helpConstraintLayout);
		topBarConstraintLayout = rootView.findViewById(R.id.topBarConstraintLayout);

		mTopBarHelpButton = rootView.findViewById(R.id.topBarHelpTextView);

		mFrontCaptureHintTextView = rootView.findViewById(R.id.frontCaptureHintTextView);


		rootView.findViewById(R.id.backImageView).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().onBackPressed();
			}
		});

		rootView.findViewById(R.id.landscapeBackImageView).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().onBackPressed();
			}
		});

		final ConstraintLayout driverLicenseBackConstraintLayout = rootView.findViewById(R.id.driverLicenseBackConstraintLayout);
		if (driverLicenseBackConstraintLayout != null) {
			driverLicenseBackConstraintLayout.setVisibility(View.VISIBLE);
		}

		setBorderConfiguration(rootView, shouldRotateBorder());
        setGhostImageConfiguration(rootView, shouldRotateGhostImage());

		return rootView;
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.flashButton || view.getId() == R.id.flashLandscapeButton) {
			toggleFlash();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		Log.i("TAG", "onResume");

		setOverlay(false);

		if (enableGhostImage) {
			timeoutHandler = new Handler();
			timeoutHandler.postDelayed(mBarcodeGhostImageTimeout, GHOST_IMAGE_TIMEOUT);
		}
        // one reason for registering in onResume() is that it allows us to use the posted preview size and add black bars to an inflated root View
        EventBus.getDefault().register(this);
		EventBus.getDefault().post(new TorchStateEvent("GET"));
	}

	private void setOverlay(boolean timedOut) {
		getView().findViewById(R.id.viewport_border).setVisibility(timedOut ? View.GONE : View.VISIBLE);
		getView().findViewById(R.id.imageOverlay).setVisibility(timedOut ? View.VISIBLE : View.GONE);
		getView().findViewById(R.id.misnap_barcode_tooltip).setVisibility(timedOut ? View.VISIBLE : View.GONE);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.i("TAG", "onPause");

		if (EventBus.getDefault().isRegistered(this)) {
			EventBus.getDefault().unregister(this);
		}

		if (enableGhostImage) {
			timeoutHandler.removeCallbacksAndMessages(null);
			timeoutHandler = null;
		}

		flashOn = false;

		updateFlash();
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEventMainThread(OnTorchStateEvent event) {
		switch (event.currentTorchState) {
			case -1:
				flashOn = false;
				flashSupported = false;
				break;
			case 0:
				flashOn = false;
				flashSupported = true;
				break;
			case 1:
				flashOn = true;
				flashSupported = true;
				break;
		}

		updateFlash();
	}

	private void toggleFlash() {
		flashOn = !flashOn;
		EventBus.getDefault().post(new TorchStateEvent("SET", flashOn));
	}

	private void updateFlash() {
		if (!flashSupported) {
			buttonFlash.setVisibility(View.GONE);
			buttonFlashLandscape.setVisibility(View.GONE);
			return;
		} else {
			buttonFlash.setVisibility(View.VISIBLE);
			buttonFlashLandscape.setVisibility(View.VISIBLE);
		}

		if (flashOn) {
			buttonFlash.setImageResource(R.drawable.misnap_barcode_flashbuttonon_ux2);
			buttonFlashLandscape.setImageResource(R.drawable.misnap_barcode_flashbuttonon_ux2);
		} else {
			buttonFlash.setImageResource(R.drawable.misnap_barcode_flashbuttonoff_ux2);
			buttonFlashLandscape.setImageResource(R.drawable.misnap_barcode_flashbuttonoff_ux2);
		}

		buttonFlash.postInvalidate();
		buttonFlashLandscape.postInvalidate();
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        boolean shouldRotateImage = shouldRotateGhostImage();
        setBorderConfiguration(getView(), shouldRotateBorder());
        setGhostImageConfiguration(getView(), shouldRotateGhostImage());

		if (shouldRotateImage) {
//			mTopBarHelpButton.setVisibility(View.GONE);
			mFrontCaptureHintTextView.setVisibility(View.VISIBLE);
		} else {
//			mTopBarHelpButton.setVisibility(View.VISIBLE);
			mFrontCaptureHintTextView.setVisibility(View.GONE);
		}
    }

    private void setGhostImageConfiguration(View rootView, boolean shouldRotate) {
        ImageView failoverGhostImage = (ImageView) rootView.findViewById(R.id.imageOverlay);
        if (shouldRotate) {
            failoverGhostImage.setRotation(90);
        } else {
            failoverGhostImage.setRotation(0);
        }
    }

    private void setBorderConfiguration(View rootView, boolean isWide) {
        ConstraintLayout constraintLayout = (ConstraintLayout) rootView.findViewById(R.id.viewport_border);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.connect(R.id.viewport_border_panel_left, ConstraintSet.RIGHT, R.id.guideline_left, ConstraintSet.LEFT);
        constraintSet.connect(R.id.viewport_border_panel_right, ConstraintSet.LEFT, R.id.guideline_right, ConstraintSet.RIGHT);

        ImageView viewFinderImageView = rootView.findViewById(R.id.misnap_barcode_rectangle);
		if (Utils.getDeviceBasicOrientation(getActivity().getApplicationContext()) == Configuration.ORIENTATION_PORTRAIT) {
			viewFinderImageView.setImageResource(R.drawable.ghost_id_card_portrait);
			topGhostView.setVisibility(View.VISIBLE);
			bottomGhostView.setVisibility(View.VISIBLE);
			leftGhostView.setVisibility(View.GONE);
			rightGhostView.setVisibility(View.GONE);

			backConstraintLayout.setVisibility(View.GONE);
			helpConstraintLayout.setVisibility(View.GONE);
			topBarConstraintLayout.setVisibility(View.VISIBLE);
		} else {
			viewFinderImageView.setImageResource(R.drawable.ghost_id_card_landscape);
			topGhostView.setVisibility(View.GONE);
			bottomGhostView.setVisibility(View.GONE);
			leftGhostView.setVisibility(View.VISIBLE);
			rightGhostView.setVisibility(View.VISIBLE);

			backConstraintLayout.setVisibility(View.VISIBLE);
			helpConstraintLayout.setVisibility(View.VISIBLE);
			topBarConstraintLayout.setVisibility(View.GONE);
		}

        if (isWide) {
//            constraintSet.connect(R.id.misnap_barcode_rectangle, ConstraintSet.TOP, R.id.guideline_inner_top, ConstraintSet.BOTTOM);
//            constraintSet.connect(R.id.misnap_barcode_rectangle, ConstraintSet.BOTTOM, R.id.guideline_inner_bottom, ConstraintSet.TOP);
			constraintSet.connect(R.id.misnap_barcode_rectangle, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
			constraintSet.connect(R.id.misnap_barcode_rectangle, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);


            constraintSet.connect(R.id.viewport_border_panel_top, ConstraintSet.BOTTOM, R.id.guideline_inner_top, ConstraintSet.BOTTOM);
            constraintSet.connect(R.id.viewport_border_panel_bottom, ConstraintSet.TOP, R.id.guideline_inner_bottom, ConstraintSet.TOP);
        } else {
//            constraintSet.connect(R.id.misnap_barcode_rectangle, ConstraintSet.TOP, R.id.guideline_outer_top, ConstraintSet.BOTTOM);
//            constraintSet.connect(R.id.misnap_barcode_rectangle, ConstraintSet.BOTTOM, R.id.guideline_outer_bottom, ConstraintSet.TOP);
			constraintSet.connect(R.id.misnap_barcode_rectangle, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
			constraintSet.connect(R.id.misnap_barcode_rectangle, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);


            constraintSet.connect(R.id.viewport_border_panel_top, ConstraintSet.BOTTOM, R.id.guideline_outer_top, ConstraintSet.BOTTOM);
            constraintSet.connect(R.id.viewport_border_panel_bottom, ConstraintSet.TOP, R.id.guideline_outer_bottom, ConstraintSet.TOP);
        }

        constraintSet.applyTo(constraintLayout);
    }

    private boolean shouldRotateBorder() {
        return ((paramMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_PORTRAIT_DOCUMENT_LANDSCAPE
                || paramMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_FREE_DOCUMENT_LANDSCAPE)
                && Utils.getDeviceBasicOrientation(getActivity().getApplicationContext()) == Configuration.ORIENTATION_PORTRAIT);
    }

    private boolean shouldRotateGhostImage() {
        return ((paramMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_PORTRAIT_DOCUMENT_PORTRAIT
                || paramMgr.getRequestedOrientation() == MiSnapApi.PARAMETER_ORIENTATION_DEVICE_FREE_DOCUMENT_ALIGNED_WITH_DEVICE)
                && Utils.getDeviceBasicOrientation(getActivity().getApplicationContext()) == Configuration.ORIENTATION_PORTRAIT);
    }

    private void addBlackBarsIfNecessary(View rootView, ScaledPreviewSizeStickyEvent event) {
//        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)rootView.getLayoutParams();
//        layoutParams.gravity = Gravity.CENTER;
//        layoutParams.width = event.getWidth();
//        layoutParams.height = event.getHeight();
//
//        rootView.setLayoutParams(layoutParams);
//        rootView.requestLayout();
    }

    @Subscribe(sticky = true)
    public void onEstablishedPreviewSizeStickyEvent(ScaledPreviewSizeStickyEvent event) {
        addBlackBarsIfNecessary(getView(), event);
    }

}