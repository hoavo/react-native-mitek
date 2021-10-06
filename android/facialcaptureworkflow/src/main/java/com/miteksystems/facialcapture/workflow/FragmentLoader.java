package com.miteksystems.facialcapture.workflow;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.List;

/**
 * Created by flee on 5/4/16.
 */
public class FragmentLoader {

    private static final String TAG_OVERLAY = "TAG_OVERLAY";

    public static boolean showScreen(FragmentManager fm, Fragment nextFragment) {
        if (fm == null || nextFragment == null) {
            return false;
        }

        removeOverlayScreens(fm);

        // We identify fragments by their tags to help with unit and regression testing.
        fm.beginTransaction()
                .replace(R.id.facialcaptureWorkflowFragmentContainer, nextFragment, nextFragment.getClass().getSimpleName())
                .commit();

        return true;
    }

    public static boolean overlayScreen(FragmentManager fm, Fragment overlayFragment) {
        if (fm == null || overlayFragment == null) {
            return false;
        }

        // We identify fragments by their tags to help with unit and regression testing.
        fm.beginTransaction()
                .add(R.id.facialcaptureWorkflowFragmentContainer, overlayFragment, TAG_OVERLAY)
                .commit();

        return true;
    }

    public static void removeOverlayScreens(FragmentManager fm) {
        if (fm == null) {
            return;
        }

        List<Fragment> fragments = fm.getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                // Remove ONLY overlays
                if (fragment != null
                        && fragment.getTag().equals(TAG_OVERLAY)) {
                    fm.beginTransaction()
                            .remove(fragment)
                            .commit();
                }
            }
        }
    }
}