package com.miteksystems.misnap.misnapworkflow_UX2.ui.animation;

import android.content.Context;
import android.content.res.TypedArray;
import android.widget.ImageView;

import com.miteksystems.misnap.misnapworkflow_UX2.R;

public class MiSnapAnimation  {
    public int FPS = 30;  // animation FPS

    public static FrameSequenceAnimation createBugAnim(ImageView imageView, Context context) {
        TypedArray lXmlRes =
                context.getResources().obtainTypedArray(R.array.bug_animation_ux2);
        int[] bugAnimation = new int[lXmlRes.length()];
        for(int i = 0; i < lXmlRes.length(); i++) {
            try {
                bugAnimation[i] = lXmlRes.getResourceId(i,-1);
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return new FrameSequenceAnimation(imageView, bugAnimation, 25);	// rough FPS
    }

    public static FrameSequenceAnimation createBugStill(ImageView imageView, Context context) {
        TypedArray lXmlRes =
                context.getResources().obtainTypedArray(R.array.bug_still);
        int[] bugStill = new int[lXmlRes.length()];
        for(int i = 0; i < lXmlRes.length(); i++) {
            try {
                bugStill[i] = lXmlRes.getResourceId(i,-1);
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return new FrameSequenceAnimation(imageView, bugStill, 1);
    }
}
