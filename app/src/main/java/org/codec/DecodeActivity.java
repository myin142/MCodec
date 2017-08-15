package org.codec;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class DecodeActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static String videoFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/videos/";
    private static String hdLowVideo = "cats_with_timecode-1920x1080-30fps-baseline-4mbps.mp4";
    private static String hdHighVideo = "cats_with_timecode-1920x1080-30fps-main-14mbps.mp4";
    private static String hdHighBase = "cats_with_timecode-1920x1080-30fps-baseline-14mbps.mp4";
    private static String lowVideo = "cats_with_timecode-640x320-30fps-baseline-4mbps.mp4";
    private static String hdHigh24 = "cats_with_timecode-1920x1080-24fps-baseline-14mpbs.mp4";

    private static String TAG = "DecodeActivity";

    private static final String SAMPLE = videoFolder + hdHighBase;
    private PlayerThread mPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SurfaceView sView = (SurfaceView) findViewById(R.id.surfaceView);
        SurfaceHolder holder = sView.getHolder();
        holder.addCallback(this);
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mPlayer == null) {
            mPlayer = new PlayerThread(holder.getSurface());
            mPlayer.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mPlayer != null) {
            mPlayer.interrupt();
        }
    }

    private class PlayerThread extends Thread {
        private MediaExtractor extractor;
        private MediaCodec decoder;
        private Surface surface;

        private PlayerThread(Surface surface) {
            this.surface = surface;
        }

        private void initDecoder(){
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

            decoder.start();
        }

        @Override
        public void run() {
            initDecoder();

            BufferInfo info = new BufferInfo();
            int timeout = 10000;
            long time = 2 * 33333;
            extractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

            boolean render = false;
            while(!render){
                int inputId = decoder.dequeueInputBuffer(timeout);
                if(inputId >= 0){
                    ByteBuffer buffer = decoder.getInputBuffer(inputId);
                    int sample = 0;
                    if (buffer != null) {
                        sample = extractor.readSampleData(buffer, 0);
                    }
                    long presentationTime = extractor.getSampleTime();

                    if(sample < 0){
                        decoder.queueInputBuffer(inputId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }else{
                        decoder.queueInputBuffer(inputId, 0, sample, presentationTime, 0);
                        extractor.advance();
                    }

                }

                int outputId = decoder.dequeueOutputBuffer(info, timeout);
                if(outputId >= 0){
                    if(info.presentationTimeUs >= time) render = true;
                    decoder.releaseOutputBuffer(outputId, render);
                }
            }

            decoder.stop();
            decoder.release();
            extractor.release();
        }
    }
}
