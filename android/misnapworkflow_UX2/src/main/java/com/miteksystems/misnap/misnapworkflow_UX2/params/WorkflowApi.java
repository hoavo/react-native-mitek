package com.miteksystems.misnap.misnapworkflow_UX2.params;

import com.miteksystems.misnap.params.ApiParameterBuilder;
import com.miteksystems.misnap.params.MiSnapApi;

import org.json.JSONObject;

/**
 * Created by awood on 10/14/16.
 */

public class WorkflowApi {

    /**
     * If enabled, a red outline will be drawn around any glare that is detected on the document.
     * The outline's lifecycle is tied to the glare hint bubble.
     * <p/>
     * Default: {@link #TRACK_GLARE_DEFAULT}
     */
    public static final String MiSnapTrackGlare = "MiSnapTrackGlare";

    /**
     * @deprecated In future MiSnap releases, the MiSnapTrackGlare parameter will transition to boolean instead of int to better reflect its function
     */
    @Deprecated
    public static final int TRACK_GLARE_DISABLED = 0;
    /**
     * @deprecated In future MiSnap releases, the MiSnapTrackGlare parameter will transition to boolean instead of int to better reflect its function
     */
    @Deprecated
    public static final int TRACK_GLARE_ENABLED = 1;
    static final int TRACK_GLARE_DEFAULT = TRACK_GLARE_ENABLED;

    /**
     * Sets what MiSnap does after failing over
     * <p>
     * By setting Failover Type to {@value SEAMLESS_FAILOVER}, MiSnap will immediately start in manual mode after timeout,
     * bypassing the failover screen.
     * <p>
     * By setting Failover Type to {@value SKIP_FAILOVER_SCREEN}, MiSnap will shutdown and restart in manual mode after timeout,
     * bypassing the failover screen.  Note that there will be a delay between when MiSnap shuts down and when it restarts.
     * <p>
     * By setting this MiSnap parameter to its default value of {@value USE_FAILOVER_SCREEN}, MiSnap will display the failover screen first,
     * allowing users to either retry in auto or proceed in manual.
     * <p>
     * <b>Values:</b><br>
     * Default: {@value MISNAP_FAILOVER_TYPE_DEFAULT}<br>
     */
    public static final String MiSnapFailoverType = "MiSnapFailoverType";
    public static final int USE_FAILOVER_SCREEN = 0;
    public static final int SEAMLESS_FAILOVER = 1;
    /**
     *  @deprecated SKIP_FAILOVER_SCREEN will cause the MiSnap camera to completely restart, resulting in a screen flicker when failing over.
     *  For a smoother experience, use SEAMLESS_FAILOVER instead.
     */
    @Deprecated
    public static final int SKIP_FAILOVER_SCREEN = 2;
    static final int FAILOVER_TYPE_LOWER_BOUND = 0;
    static final int FAILOVER_TYPE_UPPER_BOUND = 2;
    static final int MISNAP_FAILOVER_TYPE_DEFAULT = USE_FAILOVER_SCREEN;


    public static final String MiSnapInitialTimeout = "MiSnapInitialTimeout";
    static final int INITIAL_TIMEOUT_LOWER_BOUND = 0;
    static final int INITIAL_TIMEOUT_UPPER_BOUND = 90 * 1000;
    static final int INITIAL_TIMEOUT_DEFAULT = 20 * 1000;

    public static final String MiSnapSubsequentTimeout = "MiSnapSubsequentTimeout";
    static final int SUBSEQUENT_TIMEOUT_LOWER_BOUND = 0;
    static final int SUBSEQUENT_TIMEOUT_UPPER_BOUND = 90 * 1000;
    static final int SUBSEQUENT_TIMEOUT_DEFAULT = 30 * 1000;

    public static final String MiSnapMaxTimeouts = "MiSnapMaxTimeouts";
    static final int MAX_TIMEOUT_LOWER_BOUND = 0;
    static final int MAX_TIMEOUT_UPPER_BOUND = 9001;
    static final int MAX_TIMEOUT_DEFAULT = 0;

    public static final String MiSnapSmartHintInitialDelay = "MiSnapSmartHintInitialDelay";
    static final int HINT_INITIAL_DELAY_LOWER_BOUND = 0;
    static final int HINT_INITIAL_DELAY_UPPER_BOUND = 90 * 1000;
    static final int HINT_INITIAL_DELAY_DEFAULT = 3000;

    public static final String MiSnapSmartHintUpdatePeriod = "MiSnapSmartHintUpdatePeriod";
    static final int HINT_UPDATE_LOWER_BOUND = 0;
    static final int HINT_UPDATE_UPPER_BOUND = 90 * 1000;
    static final int HINT_UPDATE_DEFAULT = 1000;

    public static final String MiSnapAnimationRectangleColor = "MiSnapAnimationRectangleColor";
    static final int ANIMATION_RECT_COLOR_LOWER_BOUND = 0x80000000;
    static final int ANIMATION_RECT_COLOR_UPPER_BOUND = 0x7FFFFFFF;
    static final int ANIMATION_RECT_COLOR_DEFAULT = 0xFFED1C24;

    public static final String MiSnapAnimationRectangleStrokeWidth = "MiSnapAnimationRectangleStrokeWidth";
    static final int ANIMATION_RECT_STROKE_WIDTH_LOWER_BOUND = 0;
    static final int ANIMATION_RECT_STROKE_WIDTH_UPPER_BOUND = 100;
    static final int ANIMATION_RECT_STROKE_WIDTH_DEFAULT = 20;

    public static final String MiSnapAnimationRectangleCornerRadius = "MiSnapAnimationRectangleCornerRadius";
    static final int ANIMATION_RECT_CORNER_RADIUS_LOWER_BOUND = 0;
    static final int ANIMATION_RECT_CORNER_RADIUS_UPPER_BOUND = 100;
    static final int ANIMATION_RECT_CORNER_RADIUS_DEFAULT = 16;

    /**
     *  @deprecated MiSnapOverrideLocale will not work on API 25+ devices, and will be removed in future releases.
     */
    @Deprecated
    public static final String MiSnapOverrideLocale = "MiSnapOverrideLocale";
    static final String OVERRIDE_LOCALE_DEFAULT = "";

    /**
     * @deprecated Use misnapworkflow_strings.xml (misnap_check_front_text_ux2, misnap_check_back_text_ux2, misnap_default_text_ux2)
     * This value represents the text that will be displayed at the top of the image capture screen.
     * <p>
     * If the parameter is not found, a localized version of "Back Image" will be
     * displayed for check backs, or a localized version of "Front Image" will be displayed for
     * any other document type.
     * <p>
     * If the parameter is found and is "" (empty string), no prompt string will be
     * displayed.
     * <p>
     * If the value is anything else and less than 40 characters, that exact value will be displayed.
     * Otherwise, it will be truncated to show first 40 characters only.
     * <p>
     * (Therefore, if the app is designed to use its own localized value for this purpose, the
     * the call to retrieve the localized resource should occur prior to establishing the
     * parameter to be passed to MiSnapFragment.)
     * <p>
     * <b>Value:</b><br>
     * Default: Either "Front Image" or "Back Image", in the current device language, or in English if
     * the language is not supported.<br>
     */
    @Deprecated public static final String MiSnapTextOverlay = "MiSnapTextOverlay";

    /**
     * @deprecated use {@link #MiSnapTextOverlay} instead
     */
    @Deprecated public static final String MiSnapTextCheckBackPrompt = "MiSnapTextCheckBackPrompt";

    /**
     * @deprecated use {@link #MiSnapTextOverlay} instead
     */
    @Deprecated public static final String MiSnapTextCheckFrontPrompt = "MiSnapTextCheckFrontPrompt";

    /**
     * A human readable description of the document type referenced in {@link MiSnapApi#MiSnapDocumentType} .
     */
    @Deprecated public static final String MiSnapShortDescription = "MiSnapShortDescription";
    static String SHORT_DESCRIPTION_DEFAULT = "";



    private static String[] docSpecificParameters = new String[] {
            MiSnapTextOverlay
    };
    private static String[] commonParameters = new String[] {
            MiSnapTrackGlare
    };
}
