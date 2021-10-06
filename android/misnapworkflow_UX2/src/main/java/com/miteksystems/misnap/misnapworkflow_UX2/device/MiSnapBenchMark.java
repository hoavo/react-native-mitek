package com.miteksystems.misnap.misnapworkflow_UX2.device;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;

import java.util.List;

/**
 * Created by asingal on 11/10/2015.
 */
public class MiSnapBenchMark {

    public static boolean isCameraSufficientForAutoCapture(final Context context) {
        if (context == null) {
            return false;
        }

        boolean testsPass = true;

        try {
            //create a Camera Object
            Camera mCamera = getCameraInstance(context);

            //Check if the device supports the required focus modes
            if (!supportsRequiredResolutions(context, mCamera)) {//check if the device supports 108op or 720 resolution
                testsPass = false;
            } else if(!supportsAutoFocusMode(context, mCamera)) {
                testsPass = false;
            }

            //release the Camera obj
            release(mCamera);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return testsPass;
    }


    /** Check if this device has a camera */
    private static boolean cameraHardwareExists(Context context) {
        return context
                .getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /** A safe way to get an instance of the Camera object.
     * @throws Exception */
    private static Camera getCameraInstance(Context context) throws Exception {
        if (!cameraHardwareExists(context)) {
            throw new Exception("MiSnap: Camera Hardware does not exits");
        }

        Camera cameraObj;
        try {
            cameraObj = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("MiSnap: Trouble starting native Camera");
        }
        return cameraObj;
    }

    private static boolean supportsAutoFocusMode(Context context,Camera cameraObj){
        Camera.Parameters camParams = cameraObj.getParameters();
        if (camParams == null) {
            return false;
        }

        boolean supportsAutoFocus=false,supportsContVideoFocus=false,supportsContPictureFocus=false;
        List<String> focusModes = camParams.getSupportedFocusModes();
        if (focusModes != null) {
            //auto-focus
            supportsAutoFocus = focusModes
                    .contains(Camera.Parameters.FOCUS_MODE_AUTO);
            if (!supportsAutoFocus) {
                supportsAutoFocus
                        = context
                        .getPackageManager()
                        .hasSystemFeature("android.hardware.camera.autofocus");
            }
            //continuous video mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                supportsContVideoFocus = focusModes
                        .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            //continuous picture mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                supportsContPictureFocus = focusModes
                        .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
        }
        return supportsAutoFocus || supportsContVideoFocus || supportsContPictureFocus;
    }

    private static boolean supportsRequiredResolutions(Context context,Camera cameraObj) {
        //check if the calculations have already been done
        Camera.Parameters camParams = cameraObj.getParameters();
        if (camParams == null) {
            return false;
        }

        boolean supports1080p=false,supports720p=false;
        List<Camera.Size> previewSizes = camParams.getSupportedPreviewSizes();
        if (null != previewSizes && 0 != previewSizes.size()) {
            final Camera.Size tenEightyP = cameraObj.new Size(1920, 1080);
            final Camera.Size sevenTwentyP = cameraObj.new Size(1280, 720);

            supports1080p = previewSizes.contains(tenEightyP);
            supports720p = previewSizes.contains(sevenTwentyP);
        }

        return supports720p || supports1080p;
    }

     private static void release(Camera cameraObj) {
        try{

            if (null != cameraObj) {
                cameraObj.release();
                cameraObj = null;
            }
        } catch(Exception e) {
            //something wrong happened
        }
    }

}
