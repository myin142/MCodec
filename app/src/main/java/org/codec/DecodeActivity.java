package org.codec;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;

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

    FrameGrab grab = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int frame = 1750;
        int frameEnd = 1900;
        int frameMiddle= frameEnd/2;

        // Create Decoder for File
        // Re-run if using new source
        grab = new FrameGrab();
        grab.setSource(SAMPLE);
        grab.init();

        // Get Single Frame
        grab.seekToTime(10);
        grab.getFrameAtTime(10);
        grab.saveBitmap(videoFolder + "time.jpg");

        // Get Frame Sequence
        grab.seekToFrame(frame);
        for(int i = frame; i <= frameEnd; i++) {
            if(grab.isEOS()){ break; }
            grab.getFrameAt(frame);
            grab.saveBitmap(videoFolder + "test/frame"+i+".jpg");
        }

        // Get Frame from the back, need to resetDecoder
        grab.flushDecoder();
        grab.seekToFrame(frameMiddle);
        grab.getFrameAt(frameMiddle);
        grab.saveBitmap(videoFolder + "test03.jpg");

        // Release if framegrab is not needed anymore or creating new FrameGrab
        grab.release();

        // Create new FrameGrab with new Source
        grab = new FrameGrab();
        grab.setSource(videoFolder + hdHighVideo);
        grab.setTargetSize(640, 360);
        grab.init();

        // Get Single Frame with another Image Size
        grab.seekToFrame(frame);
        grab.getFrameAt(frame);
        Bitmap img = grab.getBitmap();
    }

}
