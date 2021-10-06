package com.miteksystems.misnap.misnapworkflow_UX2.ui.overlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.miteksystems.misnap.misnapworkflow_UX2.params.WorkflowConstants;
import com.miteksystems.misnap.params.SDKConstants;


public class UIManager {
	Context mContext=null;
	CameraOverlay mCameraOverlay=null;
    static boolean mVibrated=false;
    static boolean mRegisteredRegister=false;

	public UIManager(Context lContext, CameraOverlay lCameraOverlay) {
		mContext = lContext.getApplicationContext();
		mVibrated = false;
        mCameraOverlay = lCameraOverlay;
	}

    public void initialize() {
        //register the broadcaster
        IntentFilter filter = new IntentFilter();
        filter.addAction(SDKConstants.UI_FRAGMENT_BROADCASTER);
        //register the BR
        LocalBroadcastManager.
                getInstance(mContext).
                registerReceiver(mUIBroadcastReceiver, filter);
        mRegisteredRegister = true;
    }

    public void cleanup() {
        //unregister the BR
        if(mUIBroadcastReceiver != null && mRegisteredRegister) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mUIBroadcastReceiver);
            mRegisteredRegister = false;
        }

        if(mCameraOverlay != null) {
            mCameraOverlay.cleanup();
            mCameraOverlay = null;
        }
        mVibrated = false;
    }

    private BroadcastReceiver mUIBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent arg1) {
            Log.d("UIManager", "mUIBroadcastReceiver message received.");

			int lMsgWhat = arg1.getIntExtra(SDKConstants.UI_FRAGMENT_BROADCAST_MESSAGE_ID, 0);
            Log.d("UIManager", "mUIBroadcastReceiver: " + lMsgWhat);
			switch(lMsgWhat) {

			case WorkflowConstants.UI_DO_VIBRATE:{
				doVibrate();
				break;
			}

			case SDKConstants.UI_FRAGMENT_DISPLAY_FPS_DATA:{
				processFPSData(arg1);
				break;
			}

            case WorkflowConstants.UI_FRAGMENT_SNAP_BUTTON_CLICKED:{
                processSnapButtonClick();
                break;
            }
		}
	}
    };

    private void processSnapButtonClick() {
        //disable the guide image
        processHideGhostImage(true);
        //disable the UI buttons
        hideOverlayButtons();
    }

    private void hideOverlayButtons() {
        if(mCameraOverlay != null) {
            mCameraOverlay.hideButtons();
        }
    }

	protected void processFPSData(Intent arg1) {
		if(mCameraOverlay != null) {
			//query the camera torch state and set in UI accordingly
			// use the correct ghost image
			mCameraOverlay.showFPSData(arg1.getStringExtra(SDKConstants.UI_FRAGMENT_INTENT_STRING_PARAM1));
		}
	}

	protected void doVibrate() {
        if(!mVibrated) {
            mVibrated = true;

            // vibrate
            snapShotSoundAndVibrate();
        }
	}

	protected void processHideGhostImage(final boolean lImmediateHide) {
		if(mCameraOverlay != null) {
			mCameraOverlay.removeGhostImage(lImmediateHide);
		}
	}

	 void snapShotSoundAndVibrate() {
         Log.w("UIManager", "snapShotSoundAndVibrate");
		// Play system sound
		 AudioManager meng = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		 final int volume = meng.getStreamVolume( AudioManager.STREAM_NOTIFICATION);

		 if (0 != volume) {
            MediaPlayer lMediaPlayer = MediaPlayer.create(mContext, SDKConstants.CAMERA_CLICK_SOUND); // can return null
            if (null != lMediaPlayer) {
				lMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						mp.release();
					}
				});

				lMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(MediaPlayer mp) {
						mp.setVolume(volume, volume);
						mp.start();
					}
				});
			} else {
				// TODO need to have alternative for sound
				Log.w("UIManager", SDKConstants.CAMERA_CLICK_SOUND + " still null after create()");
			}
		}

		// Get instance of Vibrator from current Context
		Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
		 
		// The following numbers represent millisecond lengths
		int dot = 100;	  // Length of a Morse Code "dot" in milliseconds
		int dash = 200;	 // Length of a Morse Code "dash" in milliseconds
		int short_gap = 100;	// Length of Gap Between dots/dashes
		long[] pattern = {0, dot, short_gap, dash};

		// Only perform this pattern one time (-1 means "do not repeat")
		v.vibrate(pattern, -1);
	}
}
