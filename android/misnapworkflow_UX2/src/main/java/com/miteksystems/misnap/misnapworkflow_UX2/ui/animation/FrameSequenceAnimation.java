package com.miteksystems.misnap.misnapworkflow_UX2.ui.animation;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;

import java.lang.ref.SoftReference;

/**
 * Created by ash on 4/26/15.
 */
public class FrameSequenceAnimation {

    private int[] mFrames; // animation frames
    private int mIndex; // current frame
    private boolean mShouldRun; // true if the animation should continue running. Used to stop the animation
    private boolean mIsRunning; // true if the animation currently running. prevents starting the animation twice
    private SoftReference<ImageView> mSoftReferenceImageView; // Used to prevent holding ImageView when it should be dead.
    private Handler mHandler;
    private int mDelayMillis;
    private long mStartTime;
//	private OnAnimationStoppedListener mOnAnimationStoppedListener;

    private Bitmap mBitmap = null;
    private BitmapFactory.Options mBitmapOptions;

    public boolean isRunning() {
        return mIsRunning;
    }



    FrameSequenceAnimation(ImageView imageView, int[] frames, int fps) {
        mHandler = new Handler();
        mFrames = frames;
        mIndex = 0;
        mSoftReferenceImageView = new SoftReference<ImageView>(imageView);
        mShouldRun = false;
        mIsRunning = false;
        mDelayMillis = 1000 / fps;
        mStartTime = System.currentTimeMillis();

        // use in place bitmap to save GC work (when animation images are the same size & type)
        if (Build.VERSION.SDK_INT >= 11) {
            mBitmapOptions = new BitmapFactory.Options();
            // setup bitmap reuse options.
            mBitmapOptions.inBitmap = mBitmap;
            mBitmapOptions.inInputShareable = true;
            mBitmapOptions.inPurgeable = true;
            mBitmapOptions.inSampleSize = 1;
            mBitmap = BitmapFactory.decodeResource(imageView.getResources(), mFrames[0],
                    mBitmapOptions);
            imageView.setImageBitmap(mBitmap);
        } else {
            imageView.setImageResource(mFrames[0]);	// this will work for all older Android OSs
        }
    }

    private int getNext() {
        if (++mIndex >= mFrames.length) {	// NOTE: pre-increment check
            mShouldRun = false;
            return -1;
        }

        // check for expected index, and skip frames if the expected index is more than 2 ahead
        int expectedIndex = (int)(System.currentTimeMillis() - mStartTime) / mDelayMillis;

        // the frame-skipping part: should mostly always allow drawing of last 2 frames
        if (mIndex < mFrames.length - 2 && expectedIndex - 2 > mIndex) {
            int ni = (expectedIndex <= mFrames.length) ? expectedIndex - 2 : mFrames.length - 2;
            Log.d("MiSnapAnim", "skipping " + (ni - mIndex) + " frames");	// removed by proguard
            mIndex = ni;
        }

        return mFrames[mIndex];
    }

    synchronized private void showNextFrameInSequence(final Runnable frameSequenceRunnable) {
        int currentImageRes = mFrames[mIndex];
        int nextImageRes = getNext();
        if (!mShouldRun) {
            mIsRunning = false;
            return;
        }

        ImageView imageView = mSoftReferenceImageView.get();
        if (null == imageView) {
            mIsRunning = false;
// TODO for later ... if we decide to attach listeners to the animation stop "event"
//					if (null != mOnAnimationStoppedListener) {
//					    mOnAnimationStoppedListener.AnimationStopped();
//					}
            return;
        }

        // fudge factor - cut skipped frames
        mHandler.postDelayed(frameSequenceRunnable, mDelayMillis-10);

        Resources r = imageView.getResources();
        if (currentImageRes == nextImageRes) {
//            if (MiSnapContext.TEST_MODE)
            {
                LogR(r, nextImageRes, "same res id, no need to re-decode/redraw -> ");
            }
            return;
        }

        try {
            if (null != mBitmap) { // so Build.VERSION.SDK_INT >= 11
//                if (MiSnapContext.TEST_MODE)
                {
                    LogR(r, nextImageRes, "decodeResource");
                }
                mBitmap = BitmapFactory.decodeResource(r, nextImageRes, mBitmapOptions);
                imageView.setImageBitmap(mBitmap);
                imageView.postInvalidate();
            }
        } catch (Exception e) {
//            if (MiSnapContext.TEST_MODE)
            {
                e.printStackTrace();
                LogR(r, nextImageRes, "unable to decodeResource");
            }
            if (null != mBitmap) {
                mBitmap.recycle();
                mBitmap = null;
            }
        } finally {
            if (null == mBitmap) {
//                if (MiSnapContext.TEST_MODE)
                {
                    LogR(r, nextImageRes, "setImageResource");
                }
                imageView.setImageResource(nextImageRes);
            }
        }
    }

    synchronized private void showLastFrame() {
        if(mIndex >= mFrames.length) {
            //meaning that we have already displayed all the frames;
            return;
        }


        int currentImageRes = mFrames[mIndex];
        int nextImageRes = mFrames[mFrames.length-1];

        ImageView imageView = mSoftReferenceImageView.get();
        if (null == imageView) {
            mIsRunning = false;
// TODO for later ... if we decide to attach listeners to the animation stop "event"
//					if (null != mOnAnimationStoppedListener) {
//					    mOnAnimationStoppedListener.AnimationStopped();
//					}
            return;
        }

        Resources r = imageView.getResources();
        if (currentImageRes == nextImageRes) {
//            if (MiSnapContext.TEST_MODE)
            {
                LogR(r, nextImageRes, "same res id, no need to re-decode/redraw -> ");
            }
            return;
        }
        try {
            if (null != mBitmap) { // so Build.VERSION.SDK_INT >= 11
//                if (MiSnapContext.TEST_MODE)
                {
                    LogR(r, nextImageRes, "decodeResource");
                }
                mBitmap = BitmapFactory.decodeResource(r, nextImageRes, mBitmapOptions);
                imageView.setImageBitmap(mBitmap);
                imageView.postInvalidate();
            }
        } catch (Exception e) {
//            if (MiSnapContext.TEST_MODE)
            {
                e.printStackTrace();
                LogR(r, nextImageRes, "unable to decodeResource");
            }
            if (null != mBitmap) {
                mBitmap.recycle();
                mBitmap = null;
            }
        } finally {
            if (null == mBitmap) {
//                if (MiSnapContext.TEST_MODE)
                {
                    LogR(r, nextImageRes, "setImageResource");
                }
                imageView.setImageResource(nextImageRes);
            }
        }
    }

    /**
     * Starts the animation
     */
    public synchronized void start() {
        mShouldRun = true;
        if (mIsRunning) {
            return;
        }
        mIsRunning = true;

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                showNextFrameInSequence(this);
            }
        };

        mHandler.post(runnable);
    }

    /**
     * Stops the animation
     */
    public synchronized void stop() {
        mIsRunning = false;
        mShouldRun = false;
        //Bug Fix: there were situations when only few initial frames were displayed before animation was stopped; making sure that we display the last frame
        showLastFrame();
    }

    private void LogR(Resources r, int imageRes, String subTag) {	// removed by proguard
        try {
            TypedValue outValue = new TypedValue();
            r.getValue(imageRes, outValue, true);
            Log.v("MiSnapAnim", subTag + "(" + outValue.string + ")");
        } catch (Resources.NotFoundException nfe) {
            Log.d("MiSnapAnim", subTag + "(image[" + mIndex + "])");
        }
    }
}
