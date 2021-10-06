package com.miteksystems.facialcapture.workflow.accessibility;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;

/**
 * Created by awood on 4/26/16.
 */
public class MiSound {

    private static final Uri CAMERA_CLICK_SOUND
            = Uri.parse("file:///system/media/audio/ui/camera_click.ogg");

//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void playCameraClickSound(final Context context) {
        Context appContext = context.getApplicationContext();

        // Check system volume. Only play sound if the sound is enabled
        final int volume = getMediaVolume(appContext);
        if (volume > 0) {
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
//                MediaActionSound sound = new MediaActionSound();
//                sound.play(MediaActionSound.SHUTTER_CLICK);
//                //sound.release();
//            } else {
                final MediaPlayer mediaPlayer = MediaPlayer.create(appContext, CAMERA_CLICK_SOUND);
                if (mediaPlayer != null) {
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.setVolume(volume, volume);
                            mp.start();
                        }
                    });
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mp.release();
                        }
                    });
                }
//            }
        }
    }

    public static int getMediaVolume(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
    }

    public static void vibrate(Context context) {
        // Get instance of Vibrator from current Context
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        // The following numbers represent millisecond lengths
        int dot = 100;       // Length of a Morse Code "dot" in milliseconds
        int dash = 200;      // Length of a Morse Code "dash" in milliseconds
        int short_gap = 100; // Length of Gap Between dots/dashes
        long[] pattern = { 0, dot, short_gap, dash };

        // Only perform this pattern one time (-1 means "do not repeat")
        v.vibrate(pattern, -1);
    }
}
