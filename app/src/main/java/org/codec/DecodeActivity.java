package org.codec;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;

import com.tam.media.FrameGrabber;

public class DecodeActivity extends AppCompatActivity{
    private static String videoFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/videos/";
    private static String hdLowVideo = "cats_with_timecode-1920x1080-30fps-baseline-4mbps.mp4";
    private static String hdHighVideo = "cats_with_timecode-1920x1080-30fps-main-14mbps.mp4";
    private static String hdHighBase = "cats_with_timecode-1920x1080-30fps-baseline-14mbps.mp4";
    private static String lowVideo = "cats_with_timecode-640x320-30fps-baseline-4mbps.mp4";
    private static String hdHigh24 = "cats_with_timecode-1920x1080-24fps-baseline-14mpbs.mp4";

    private static String SAMPLE = videoFolder + hdLowVideo;
    private int start = 0;
    private int end = 100;

    private static String TAG = "DecodeActivity";
    private static boolean VERBOSE = true;

   // Entry Point
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameGrabber grab = new FrameGrabber();
        grab.setDataSource(SAMPLE);
        grab.setTargetSize(640, 360);
        grab.init();
        ArrayList<Bitmap> frames = grab.getFrameAt(start, end);
        for(int i = 0; i < frames.toArray().length; i++){
            saveBitmap(frames.get(i), videoFolder + "test/frame"+i+".jpg");
        }
    }

    private class Decoder{
        MediaExtractor extractor;
        MediaCodec decoder;
        Surface surface;
        int timeout = 10000;
        String SAMPLE = null;

        public Decoder(String path, Surface s){
            SAMPLE = path;
            surface = s;
            initDecoder();
        }

        private void initDecoder() {
            extractor = new MediaExtractor();
            try {
                extractor.setDataSource(SAMPLE);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                extractor.selectTrack(i);
                try {
                decoder = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                e.printStackTrace();
                }
                decoder.configure(format, surface, null, 0);
                break;
                }
            }

            if (decoder == null) {
                Log.e("DecodeActivity", "Can't find video info!");
                return;
            }
        }

        private boolean getFrameAt(int t){
            // Set Variables and go to specified time in video
            BufferInfo info = new BufferInfo();
            long time = t * 33333;
            extractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

            // While render frame not reached
            boolean render = false;
            while (!render) {
                // On Input Available
                int inputId = decoder.dequeueInputBuffer(timeout);
                if (inputId >= 0) {
                    // Read Data
                    ByteBuffer buffer = decoder.getInputBuffer(inputId);
                    int sample = 0;
                    if (buffer != null) {
                        sample = extractor.readSampleData(buffer, 0);
                    }
                    long presentationTime = extractor.getSampleTime();

                    // Queue Input and continue
                    if (sample < 0) {
                        decoder.queueInputBuffer(inputId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        decoder.queueInputBuffer(inputId, 0, sample, presentationTime, 0);
                        extractor.advance();
                    }

                }

                // On Output Available
                int outputId = decoder.dequeueOutputBuffer(info, timeout);
                if (outputId >= 0) {
                    // If video time equals searched time, then render
                    if (info.presentationTimeUs >= time) render = true;
                    decoder.releaseOutputBuffer(outputId, render);
                }
            }

            return render;
        }

        // Thread Entry Point
        public void start() {
            // Create Decoder and start
            initDecoder();
            decoder.start();

            // Get Frame at specified time to Output Surface
            getFrameAt(0);

            Log.d(TAG, "Decoder Released");
            decoder.stop();
            decoder.release();
            extractor.release();
        }
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

}
