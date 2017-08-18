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
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.tam.gl.GLHelper;

public class DecodeActivity extends AppCompatActivity implements SurfaceTexture.OnFrameAvailableListener{
    private static String videoFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/videos/";
    private static String hdLowVideo = "cats_with_timecode-1920x1080-30fps-baseline-4mbps.mp4";
    private static String hdHighVideo = "cats_with_timecode-1920x1080-30fps-main-14mbps.mp4";
    private static String hdHighBase = "cats_with_timecode-1920x1080-30fps-baseline-14mbps.mp4";
    private static String lowVideo = "cats_with_timecode-640x320-30fps-baseline-4mbps.mp4";
    private static String hdHigh24 = "cats_with_timecode-1920x1080-24fps-baseline-14mpbs.mp4";

    private static final String SAMPLE = videoFolder + hdHighBase;
    private static String TAG = "DecodeActivity";

    private HandlerThread mGLThread = null;
    private Handler mGLHandler = null;
    private GLHelper mGLHelper = null;

    private int mDefaultTextureID = 10001;
    private int mWidth = 1920;
    private int mHeight = 1080;

    private final Object mWaitCodec = new Object();
    private final Object mWaitFrame = new Object();
    private boolean mFrameAvailable = false;

    private SurfaceTexture sTexture = null;
    private int textureID;

    private Decoder codec = null;
    private Bitmap frame = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create Handler Thread
        Log.d(TAG, "Create Handler Thread");
        mGLThread = new HandlerThread("FrameGrabber");
        mGLThread.start();
        mGLHandler = new Handler(mGLThread.getLooper());

        // Create GLHelper
        Log.d(TAG, "Create GLHelper");
        mGLHelper = new GLHelper();
        SurfaceTexture st = new SurfaceTexture(mDefaultTextureID);
        st.setDefaultBufferSize(mWidth, mHeight);
        mGLHelper.init(st);

        // Create Surface for Codec
        Log.d(TAG, "Create Surface for Decoder");
        textureID = mGLHelper.createOESTexture();
        sTexture = new SurfaceTexture(textureID);
        sTexture.setOnFrameAvailableListener(this);
        final Surface surface = new Surface(sTexture);

        // Start decoder
        Log.d(TAG, "Initializing and Starting Decoder");
        codec = new Decoder(surface);
        codec.init();

        // Get Frame at specified number
        int frameNumber = 0;
        Log.d(TAG, "Getting FrameNumber " + frameNumber);
        frame = codec.getFrameAt(frameNumber);

        // Wait for Codec to finish
        synchronized (mWaitCodec){
            try {
                mWaitCodec.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "Saving Bitmap");
        saveBitmap(frame, videoFolder + "test/frame.jpg");

        // Release All
        Log.d(TAG, "Cleaning Things up");
        sTexture.release();
        surface.release();
        codec.release();
        mGLHelper.release();
        mGLThread.quit();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (mWaitFrame){
            if(mFrameAvailable) throw new RuntimeException("Frame already available");
            mFrameAvailable = true;
            Log.d(TAG, "OnFrameAvailable");
            mWaitFrame.notifyAll();
        }
    }

    private class Decoder{
        private MediaExtractor extractor;
        private MediaCodec decoder;
        private Surface surface;

        private int timeout = 10000;

        public Decoder(Surface surface) {
            this.surface = surface;
        }

        public void init(){
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
        public void release(){
            decoder.stop();
            decoder.release();
            extractor.release();
        }

        public Bitmap getFrameAt(int frame){
            BufferInfo info = new BufferInfo();
            long time = frame * getFrameRate();
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
                    if(render){
                        Log.d(TAG, "Released Render Output, InfoTime: " + info.presentationTimeUs);
                        synchronized (mWaitFrame) {
                            Log.d(TAG, "Wait for FrameAvailable");
                            try {
                                mWaitFrame.wait();
                            } catch (InterruptedException ie) {
                                throw new RuntimeException(ie);
                            }
                            mFrameAvailable = false;
                            Log.d(TAG, "Frame is Available");
                        }

                        Log.d(TAG, "After FrameAvailable - Returning Bitmap");
                        mGLHelper.drawFrame(sTexture, textureID);
                        synchronized (mWaitCodec){
                            Log.d(TAG, "Codec Process Done");
                            mWaitCodec.notifyAll();
                        }
                        return mGLHelper.readPixels(mWidth, mHeight);
                    }
                }
            }

            return null;
        }

        public int getFrameRate(){
            int frameRate = 24; //may be default
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

            int result = (int)((1f / frameRate) * 1000 * 1000);

            return result;
        }

    }

    private class CodecSurface implements SurfaceTexture.OnFrameAvailableListener{

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            synchronized (mWaitFrame) {
                if (mFrameAvailable) {
                    throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
                }
                mFrameAvailable = true;
                mWaitFrame.notifyAll();
            }
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
