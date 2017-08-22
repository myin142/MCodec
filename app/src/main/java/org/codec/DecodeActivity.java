package org.codec;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;

import org.codec.gl.GLHelper;
import org.codec.media.CodecOutput;
import org.codec.media.VideoDecoder;

public class DecodeActivity extends AppCompatActivity{
    private static String videoFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/videos/";
    private static String hdLowVideo = "cats_with_timecode-1920x1080-30fps-baseline-4mbps.mp4";
    private static String hdHighVideo = "cats_with_timecode-1920x1080-30fps-main-14mbps.mp4";
    private static String hdHighBase = "cats_with_timecode-1920x1080-30fps-baseline-14mbps.mp4";
    private static String lowVideo = "cats_with_timecode-640x320-30fps-baseline-4mbps.mp4";
    private static String hdHigh24 = "cats_with_timecode-1920x1080-24fps-baseline-14mpbs.mp4";

    private static final String SAMPLE = videoFolder + lowVideo;
    private static String TAG = "DecodeActivity";

    private HandlerThread mGLThread = null;
    private Handler mGLHandler = null;
    private VideoDecoder codec = null;
    private CodecOutput output = null;

    private int width = -1;
    private int height = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create Handler Thread
        Log.d(TAG, "Create Handler Thread");
        mGLThread = new HandlerThread("FrameGrabber");
        mGLThread.start();
        mGLHandler = new Handler(mGLThread.getLooper());

        output = new CodecOutput();
        setTargetSize(1920,1080);

        codec = new VideoDecoder();
        codec.setSource(SAMPLE);
        codec.init();

        if(width != -1){
            output.setWidth(width);
            output.setHeight(height);
        }else{
            output.setWidth(codec.getWidth());
            output.setHeight(codec.getHeight());
        }

        output.init();
        codec.setSurface(output.getSurface());
        codec.startDecoder();

        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                // Start decoder
                Log.d(TAG, "Initializing and Starting Decoder");

                // Get Frame at specified number
                int frameNumber = 0;
                Log.d(TAG, "Getting FrameNumber " + frameNumber);
                long startTime = System.currentTimeMillis();
                codec.seekTo(frameNumber);
                codec.getFrameAt(frameNumber); // Has to be in a different thread than framelistener
                output.saveBitmap(videoFolder + "test01.jpg");
                long endTime = System.currentTimeMillis();
                long totalTime = (endTime - startTime) / 1000;

                Log.d("Total Time", totalTime + "s");

                try {
                    Thread.sleep(100000);
                }catch(Exception e){
                    e.printStackTrace();
                }
                Log.d(TAG, "Cleaning things up");
                release();
            }
        });

    }

    public void setTargetSize(int width, int height){
        this.width = width;
        this.height = height;
    }

    public void release(){
        output.release();
        codec.release();
        mGLThread.quit();
    }




}
