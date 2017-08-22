package org.codec.media;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by min on 22/08/2017.
 */

public class VideoDecoder{
    String TAG = "VideoDecoder";

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

    // Callable after init()
    public int getWidth(){
        return format.getInteger(MediaFormat.KEY_WIDTH);
    }
    public int getHeight(){ return format.getInteger(MediaFormat.KEY_HEIGHT); }
    // END CALLABLE AFTER

    public void release(){
        decoder.stop();
        decoder.release();
        extractor.release();
    }

    // Source has to be set
    // Create Extractor and Format
    public void init(){
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
        String mime = format.getString(MediaFormat.KEY_MIME);
        try {
            decoder = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        decoder.configure(format, surface, null, 0);

        Log.d(TAG, "Decoder Starting");
        decoder.start();
    }

    // Go to last intra frame of frameNumber
    public void seekTo(int frame){
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
                if(render){
                    Log.d(TAG, "Waiting Frame");
                    CodecOutput.awaitFrame();
                }
            }
        }
    }

}

