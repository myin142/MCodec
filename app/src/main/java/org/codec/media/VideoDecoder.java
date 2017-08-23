package org.codec.media;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import org.jcodec.codecs.h264.io.model.Frame;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoder{
    String TAG = "VideoDecoder";
    boolean DEBUG = FrameGrab.DEBUG;

    MediaExtractor extractor = null;
    MediaFormat format = null;
    MediaCodec decoder = null;
    Surface surface = null;
    BufferInfo info = null;

    String source = "";
    int timeout = 10000;

    public void setSource(String path){
        source = path;
    }
    public void setSurface(Surface surface){
        this.surface = surface;
    }

    public String getSource(){
        return source;
    }
    public Surface getSurface(){
        return surface;
    }
    public int getFPS(){
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
        return frameRate;
    }
    public int getFrameRate(){
        int fps = getFPS();
        int result = (int)((1f / fps) * 1000 * 1000);
        return result;
    }

    // Callable after init()
    public int getWidth(){
        return format.getInteger(MediaFormat.KEY_WIDTH);
    }
    public int getHeight(){
        return format.getInteger(MediaFormat.KEY_HEIGHT);
    }
    // END CALLABLE AFTER

    public void release(){
        if(decoder != null) {
            decoder.stop();
            decoder.release();
        }

        if(extractor != null)
            extractor.release();

        decoder = null;
        extractor = null;
        format = null;
    }

    // Source has to be set
    // Create Extractor and Format
    public void init(){
        if(DEBUG) Log.d(TAG, "Initializing Extractor");
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(source);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < extractor.getTrackCount(); i++) {
            format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                extractor.selectTrack(i);
                break;
            }
        }
    }

    // Surface has to be set, and after init()
    // Create Decoder and Start
    public void startDecoder(){
        if(DEBUG) Log.d(TAG, "Initializing Decoder");
        String mime = format.getString(MediaFormat.KEY_MIME);
        try {
            decoder = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        decoder.configure(format, surface, null, 0);

        if(DEBUG) Log.d(TAG, "Starting Decoder");
        decoder.start();
    }

    // Reset Decoder
    public void resetDecoder(){
        if(decoder != null) {
            decoder.stop();
            decoder.configure(format, surface, null, 0);
            decoder.start();
        }
    }

    // Go to last intra frame of frameNumber
    public void seekTo(int frame){
        if(DEBUG) Log.d(TAG, "Seek To Frame " + frame);
        info = new BufferInfo();
        long time = frame * getFrameRate();
        extractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
    }

    // Get one frame at frameNumber
    public void getFrameAt(int frame){
        long time = frame * getFrameRate();
        boolean render = false;
        while (!render) {
            int inputId = decoder.dequeueInputBuffer(timeout);
            if (inputId >= 0) {
                ByteBuffer buffer = decoder.getInputBuffer(inputId);
                int sample = extractor.readSampleData(buffer, 0);
                long presentationTime = extractor.getSampleTime();

                if (sample < 0) {
                    decoder.queueInputBuffer(inputId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    decoder.queueInputBuffer(inputId, 0, sample, presentationTime, 0);
                    extractor.advance();
                }

            }

            int outputId = decoder.dequeueOutputBuffer(info, timeout);
            if (outputId >= 0) {
                if (info.presentationTimeUs >= time) render = true;
                decoder.releaseOutputBuffer(outputId, render);
                if(DEBUG && render) Log.d(TAG, "Rendering Output Time " + info.presentationTimeUs);
            }
        }
    }

}

