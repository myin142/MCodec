package org.codec;

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

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class DecodeActivity extends AppCompatActivity {
    private static String videoFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/videos/";
    private static String hdLowVideo = videoFolder + "cats_with_timecode-1920x1080-30fps-baseline-4mbps.mp4";
    private static String hdHighVideo = videoFolder + "cats_with_timecode-1920x1080-30fps-main-14mbps.mp4";
    private static String hdHighBase = videoFolder + "cats_with_timecode-1920x1080-30fps-baseline-14mbps.mp4";
    private static String lowVideo = videoFolder + "cats_with_timecode-640x320-30fps-baseline-4mbps.mp4";
    private static String hdHigh24 = videoFolder + "cats_with_timecode-1920x1080-24fps-baseline-14mpbs.mp4";

    private final String TAG = "DecodeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // CODE START
        try {
            // Variables
            int track = 0;
            int timeout = 10000;
            boolean render = true;
            String filePath = lowVideo;
            BufferInfo info = new BufferInfo();

            // Get Surface from SurfaceView
            SurfaceView sView = (SurfaceView) findViewById(R.id.surfaceView);
            SurfaceHolder sHolder = sView.getHolder();
            Surface surface = sHolder.getSurface();

            // Extractor, Set File Path
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(filePath);

            // Get MediaFormat
            MediaFormat format = extractor.getTrackFormat(track);
            String mime = format.getString(MediaFormat.KEY_MIME);

            // Create Decoder and start
            extractor.selectTrack(track);
            MediaCodec decoder = MediaCodec.createDecoderByType(mime); // Uninitialized
            decoder.configure(format, surface, null, 0); // Configuered
            decoder.start(); // Executing - flushed

            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();

            // Process Input
            int inputBufferId = decoder.dequeueInputBuffer(timeout); // Executing - running
            if(inputBufferId >= 0){
                ByteBuffer input = inputBuffers[inputBufferId];
                int readSample = extractor.readSampleData(input, 0);
                long sampleTime = extractor.getSampleTime();

                if(readSample < 0){
                    decoder.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }

                decoder.queueInputBuffer(inputBufferId, 0, readSample, sampleTime, 0); // Executing - next
            }

            // Process Output
            int outputBufferId = decoder.dequeueOutputBuffer(info, timeout);
            if(outputBufferId >= 0){
                ByteBuffer output = outputBuffers[outputBufferId];

                decoder.releaseOutputBuffer(outputBufferId, render);
            }

        }catch(IOException e){
            e.printStackTrace();
        }
        // CODE END
    }

}
