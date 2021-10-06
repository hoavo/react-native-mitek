package com.miteksystems.misnap.misnapworkflow_UX2.params;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.util.Locale;

/**
 * Created by awood on 10/23/2015.
 */
public class LocaleHelper {

    /**
     * After changing the language, you will need to restart the Activity, unless you call
     * this from your Activity's onResume() method.
     * @param context
     * @param newLocaleString The language code to use. Enter a blank string
     *                        to use the user's current system language.
     * @return true if the language needed to be, and was, changed
     */
    public static boolean changeLanguage(final Context context, String newLocaleString) {
        if (context == null || newLocaleString == null || newLocaleString.isEmpty()) {
            return false;
        }

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        if (config == null
                || config.locale.getLanguage().equals(newLocaleString)) {
            // The language is already set to this language
            return false;
        }

        newLocaleString = newLocaleString.toLowerCase();
        Locale newLocale = new Locale(newLocaleString);
        config.locale = newLocale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        return true;
    }
}
