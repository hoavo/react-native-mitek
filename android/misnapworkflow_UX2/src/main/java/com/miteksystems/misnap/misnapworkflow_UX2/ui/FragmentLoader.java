package com.miteksystems.misnap.misnapworkflow_UX2.ui;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.List;

/**
 * Created by awood on 9/1/2015.
 *
 * This class keeps all the Fragment loading, UI-related code out of the UX workflow code.
 * It's swapping the current workflow fragment for another.
 * All transactions are executed immediately for robustness, so that calls to this class are atomic
 */
public class FragmentLoader {

    public static boolean showScreen(int containerViewId, String overlayTagPrefix, FragmentManager fm, Fragment nextFragment) {
        if (fm == null || nextFragment == null) {
            return false;
        }

        removeOverlaysWithPrefix(overlayTagPrefix, fm);

        // We identify Fragments by their tags to help with unit and regression testing
        fm.beginTransaction()
                .replace(containerViewId, nextFragment, getTag(nextFragment.getClass()))
                .commit();

        fm.executePendingTransactions();
        return true;
    }

    public static boolean showOverlay(int containerViewId, String overlayTagPrefix, FragmentManager fm, Fragment overlayFragment) {
        if (fm == null || overlayFragment == null) {
            return false;
        }

        String fragmentTag = overlayTagPrefix + getTag(overlayFragment.getClass());

        // don't overlay again if the fragment already exists
        // TODO KW 2017-05-25:  find some other way to get all fragments because FragmentManager.getFragments()
        // can only be called from within the same library group(groupId = com.android.support)
        List<Fragment> fragments = fm.getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null
                        && fragment.getTag().equals(fragmentTag)) {
                    return true;
                }
            }
        }

        // We identify fragments by their tags to help with unit and regression testing.
        fm.beginTransaction()
                .add(containerViewId, overlayFragment, fragmentTag)
                .commit();

        fm.executePendingTransactions();
        return true;
    }

    public static void removeOverlaysWithPrefix(String overlayTagPrefix, FragmentManager fm) {
        if (fm == null) {
            return;
        }

        // TODO KW 2017-05-25:  find some other way to get all fragments because FragmentManager.getFragments()
        // can only be called from within the same library group(groupId = com.android.support)
        List<Fragment> fragments = fm.getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                // Remove ONLY overlays
                if (fragment != null
                        && fragment.getTag().contains(overlayTagPrefix)) {
                    fm.beginTransaction()
                            .remove(fragment)
                            .commit();
                }
            }
        }

        fm.executePendingTransactions();
    }

    public static String getTag(Class<? extends Fragment> fragmentClass) {
        return fragmentClass.getName();
    }

    public static void removeFragment(FragmentManager supportFragmentManager, Fragment fragment) {
        supportFragmentManager.beginTransaction().remove(fragment).commit();
        supportFragmentManager.executePendingTransactions();
    }

    public static Fragment findFragmentByTag(FragmentManager fm, String tag) {
        return fm.findFragmentByTag(tag);
    }
}