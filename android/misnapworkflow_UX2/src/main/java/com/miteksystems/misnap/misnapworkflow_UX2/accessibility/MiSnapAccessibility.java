package com.miteksystems.misnap.misnapworkflow_UX2.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.miteksystems.misnap.events.TextToSpeechEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;
import java.util.Locale;


@SuppressLint("NewApi")
public class MiSnapAccessibility {
	private Context mAppContext;
    private MiSnapTts mMiSnapTts;

    public static boolean isTalkbackEnabled(Context context) {
        if (VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            try {
                // Check if talkback is enabled
                AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
                if (accessibilityManager != null) {
                    List<AccessibilityServiceInfo> accessList = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN);
                    if (accessList != null) {
                        for (AccessibilityServiceInfo serviceInfo : accessList) {
                            //Found that in Samsung S6 device, Samsung has created an enhanced version of Talkback service and Google Talkback is never returned
                            //so we will use the generic way of checking if there is any enabled TAalkBack service
                            if (serviceInfo != null && serviceInfo.getSettingsActivityName() != null) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Talkback is disabled
            }
        }

        return false;
    }

    // Uses default Locale
    public MiSnapAccessibility(Context context) {
        this(context, null);
    }
	
	public MiSnapAccessibility(Context context, String locale) {
        mAppContext = context.getApplicationContext();
        mMiSnapTts = new MiSnapTts(locale);
        EventBus.getDefault().register(this);
	}

    public void shutdown() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

        if (mMiSnapTts != null) {
            mMiSnapTts.shutDown();
            mMiSnapTts = null;
        }

        mAppContext = null;
    }
	
	public void setDescription(View view, int resourceId) {
		try {
			view.setContentDescription(mAppContext.getString(resourceId));
		} catch (Exception e) {
			//do nothing 
		}
	}
	
	public void setDescription(View view, String resourceText) {
		try {
			view.setContentDescription(resourceText);
		} catch (Exception e) {
			//do nothing 
		}
	}
	
	public void disableAccessibiltyAction(View view) {
		if (VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			try {
				view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
			} catch(Exception e) {
				//do nothing 
			}
		}
	}
	
	public void enableAccessibiltyAction(View view) {
		if (VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			try {
				view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
			} catch(Exception e) {
				//do nothing 
			}
		}
	}

    public void speak(final int spokenTextId, int delayMs) {
        if (mMiSnapTts != null) {
            mMiSnapTts.speak(spokenTextId, delayMs);
        }
    }

    @Subscribe
    public void onEvent(TextToSpeechEvent event) {
        if (mMiSnapTts != null) {
            if (event.spokenTextId == TextToSpeechEvent.USE_STRING_INSTEAD_OF_ID) {
                mMiSnapTts.speak(event.spokenTextString, event.delayMs);
            } else {
                mMiSnapTts.speak(event.spokenTextId, event.delayMs);
            }
        }
    }


    private class MiSnapTts {
        private Handler mHandler;
        private TextToSpeech mTts;

        public MiSnapTts(final String locale) {
            mHandler = new Handler();
            mTts = new TextToSpeech(mAppContext, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (locale != null) {
                        mTts.setLanguage(new Locale(locale));
                    }
                }
            });
        }

        public void speak(final String spokenText, int delayMs) {
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null); // Specifying a null token removes ALL callbacks and messages
            }
            if (mTts != null && spokenText != null) {
                if (spokenText != null) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mTts.stop();
                            mTts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null);
                        }
                    }, delayMs);
                }
            }
        }

        /**
         * By using the resource ID, the customer can set the locale to a different
         * one than the system locale.
         * @param spokenTextId
         */
        public void speak(int spokenTextId, int delayMs) {
            final String spokenText = mAppContext.getResources().getString(spokenTextId);
            speak(spokenText, delayMs);
        }

        public void shutDown() {
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null); // Specifying a null token removes ALL callbacks and messages
                mHandler = null;
            }

            if (mTts != null) {
                mTts.stop();
                mTts.shutdown();
                mTts = null;
            }
        }
    }
}