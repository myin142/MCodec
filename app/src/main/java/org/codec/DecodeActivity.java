package org.codec;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.codec.media.FrameGrab;

public class DecodeActivity extends AppCompatActivity{
    // Video Files
    private static String videoFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/videos/";
    private static String hdLowVideo = "cats_with_timecode-1920x1080-30fps-baseline-4mbps.mp4";
    private static String hdHighVideo = "cats_with_timecode-1920x1080-30fps-main-14mbps.mp4";
    private static String hdHighBase = "cats_with_timecode-1920x1080-30fps-baseline-14mbps.mp4";
    private static String lowVideo = "cats_with_timecode-640x320-30fps-baseline-4mbps.mp4";
    private static String hdHigh24 = "cats_with_timecode-1920x1080-24fps-baseline-14mpbs.mp4";

    private static String SAMPLE = videoFolder + lowVideo;
    private static String TAG = "DecodeActivity";

    FrameGrab grab = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int frame = 0;
        int frameEnd = 30;
        int frameMiddle= frameEnd/2;

        grab = new FrameGrab();

        // Create Decoder for File
        // Re-Run everytime new file is used
        grab.setSource(SAMPLE);
        grab.init();

        // Get Range of Frames
        grab.seekToFrame(frame);
        for(int i = frame; i <= frameEnd; i++) {
            grab.getFrameAt(frame);
            grab.saveBitmap(videoFolder + "test/frame"+i+".jpg");
        }

        // Get Frame from the back, need to resetDecoder
        grab.resetDecoder();
        grab.seekToFrame(frameMiddle);
        grab.getFrameAt(frameMiddle);
        grab.saveBitmap(videoFolder + "test03.jpg");

        for(int i = 0; i < 10000; i++) {
            grab.init();
        }

        grab.release();

    }

}
