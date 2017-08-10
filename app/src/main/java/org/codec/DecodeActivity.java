package org.codec;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;

public class DecodeActivity extends AppCompatActivity {
    private static String videoFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/videos/";
    private static String hdLowVideo = videoFolder + "cats_with_timecode-1920x1080-30fps-baseline-4mbps.mp4";
    private static String hdHighVideo = videoFolder + "cats_with_timecode-1920x1080-30fps-main-14mbps.mp4";
    private static String hdHighBase = videoFolder + "cats_with_timecode-1920x1080-30fps-baseline-14mbps.mp4";
    private static String lowVideo = videoFolder + "cats_with_timecode-640x320-30fps-baseline-4mbps.mp4";
    private static String hdHigh24 = videoFolder + "cats_with_timecode-1920x1080-24fps-baseline-14mpbs.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

}
