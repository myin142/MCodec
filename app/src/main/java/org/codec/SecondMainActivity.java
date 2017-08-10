package org.codec;

/**
 * Created by min on 04/08/2017.
 */

import android.graphics.Bitmap;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.tam.media.FrameGrabber;

import org.jcodec.api.android.AndroidFrameGrab;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import wseemann.media.FFmpegMediaMetadataRetriever;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class SecondMainActivity extends AppCompatActivity {
    private static String videoFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/videos/";
    private static String hdLowVideo = videoFolder + "cats_with_timecode-1920x1080-30fps-baseline-4mbps.mp4";
    private static String hdHighVideo = videoFolder + "cats_with_timecode-1920x1080-30fps-main-14mbps.mp4";
    private static String hdHighBase = videoFolder + "cats_with_timecode-1920x1080-30fps-baseline-14mbps.mp4";
    private static String lowVideo = videoFolder + "cats_with_timecode-640x320-30fps-baseline-4mbps.mp4";
    private static String hdHigh24 = videoFolder + "cats_with_timecode-1920x1080-24fps-baseline-14mpbs.mp4";

    private static final File bunnyVid = new File(hdHigh24);
    private static final String LOG_TAG_DECODE_TASK = "DecodeTask";

    private int frameRate = getFrameRate(bunnyVid);
    private int duration = getVideoDuration(bunnyVid);
    private int framesInVideo = frameRate * duration;

    private FrameGrabber mFrameGrabber = null;

    private Bitmap getFrameAtTimeByFrameGrabber(String path, long time){
        mFrameGrabber = new FrameGrabber();
        mFrameGrabber.setDataSource(path);
        mFrameGrabber.setTargetSize(640, 360);
        mFrameGrabber.init();
        return mFrameGrabber.getFrameAtTime(time);
    }

    void saveBitmap(Bitmap bm, String location){
        try {
            FileOutputStream out = new FileOutputStream(location);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Show First Image
        startDecodeTask(bunnyVid);

        // Add Swipe Listener
        ImageView iView = (ImageView) findViewById(R.id.imageView);
        iView.setOnTouchListener(new SwipeListener(this){
            public void onSwipeLeft(){
                if(taskFinished())
                    currFrame += nextFrame;

                startDecodeTask(bunnyVid);
            }

            public void onSwipeRight(){
                if(taskFinished())
                    currFrame -= nextFrame;

                startDecodeTask(bunnyVid);
            }
        });
    }

    private AndroidFrameGrab frameGrab;
    private int currFrame = 0;
    private int nextFrame = 1;
    private FFmpegMediaMetadataRetriever mdRetriever;

    private DecodeTask currTask = new DecodeTask();

    class DecodeTask extends AsyncTask<File, Integer, Bitmap> {

        long startTime;
        long endTime;
        long totalTime;

        protected void onPreExecute(){
            startTime = System.currentTimeMillis();
            Log.d(LOG_TAG_DECODE_TASK, "Return Frame " + currFrame + " as Bitmap. " + frameRate + " FPS");
        }

        // Start in Background
        protected Bitmap doInBackground(File ...file) {
            return mdRetriever.getFrameAtTime(currFrame * 33333, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            //return mdRetriever.getFrameAtTime(currFrame * 41666, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            //return getFrameAtTimeByFrameGrabber(bunnyVid.getPath(), currFrame * 33333);

            /*try {
                if(frameGrab == null){
                    FileChannelWrapper ch = NIOUtils.readableChannel(file[0]);
                    frameGrab = AndroidFrameGrab.createAndroidFrameGrab(ch);
                }
                Log.d(LOG_TAG_DECODE_TASK, "Return Frame " + currFrame + " as Bitmap. " + frameRate + " FPS");
                return AndroidFrameGrab.getFrame(frameGrab.getVideoTrack(), frameGrab.getDecoder(), currFrame);
            } catch (Exception e) {
                Log.e(LOG_TAG_DECODE_TASK, "Could not decode one frame.", e);
                e.printStackTrace();
                return null;
            }*/
        }

        // After Background Task
        protected void onPostExecute(Bitmap bitmap) {
            long endTime   = System.currentTimeMillis();
            long totalTime = (endTime - startTime) / 1000;

            if (bitmap != null) {
                Log.d(LOG_TAG_DECODE_TASK, "Decoded image size: [" + bitmap.getWidth() + ", " + bitmap.getHeight() + "]. Time " + totalTime + "s.");
                display(bitmap);
            } else {
                Log.d(LOG_TAG_DECODE_TASK, "Bitmap is NULL");
                if(currFrame < 0){
                    currFrame = framesInVideo;
                }else if(currFrame > framesInVideo){
                    currFrame = 0;
                }
                currTask = (DecodeTask) new DecodeTask().execute(bunnyVid);
            }
        }


    }

    // Start Task when finished
    void startDecodeTask(File file){
        Log.d(LOG_TAG_DECODE_TASK, String.valueOf(currTask.getStatus()));
        if(taskFinished()){
            currTask = (DecodeTask) new DecodeTask().execute(file);
        }else if(currTask.getStatus() == AsyncTask.Status.PENDING){
            currTask.execute(file);
        }
    }

    boolean taskFinished(){
        return (currTask.getStatus() == AsyncTask.Status.FINISHED);
    }

    void display(Bitmap bitmap) {
        ImageView iView = (ImageView) findViewById(R.id.imageView);
        iView.setImageBitmap(bitmap);
    }

    // Get Frame Rate of Video
    int getFrameRate(File file){
        MediaExtractor extractor = new MediaExtractor();
        int frameRate = 24; //may be default
        try {
            extractor.setDataSource(file.getPath());
            int numTracks = extractor.getTrackCount();
            for (int i = 0; i < numTracks; ++i) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            extractor.release();
        }
        return frameRate;
    }

    // Get Duration of Video in Seconds
    int getVideoDuration(File file){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(file.getPath());
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Integer.parseInt(time)/1000;
    }

}