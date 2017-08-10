package org.codec.media;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by min on 09/08/2017.
 */

public class VideoDecoder{
    private static final String TAG = "VideoDecoder";
    private static final boolean DEBUG = false;

    private int MAX_FRAMES = 10;
    private final int timeout = 10000;

    private MediaCodec decoder;
    private MediaExtractor extractor;
    private OutputSurface outputSurface;
    private String file;
    private String outputFolder;

    // Video Decoder Constructor
    public VideoDecoder(){ }
    public VideoDecoder(String path){ this.file = path; }
    // CONSTRUCTOR END

    // SETTER
    public void setSource(String file){
        this.file = file;
    }
    public void setOutputFolder(String folder){ this.outputFolder = folder; }
    // SETTER END

    // Initialize MediaExtractor and MediaCodec with current file
    private void init(){
        decoder = null;
        try {
            File f = new File(this.file);
            if(f.exists() && !f.isDirectory()) {
                Log.e(TAG, "File does not exists");
            }

            extractor = new MediaExtractor();
            extractor.setDataSource(this.file);

            for(int i = 0; i < extractor.getTrackCount(); i++){
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if(mime.startsWith("video/")){
                    int width = format.getInteger(MediaFormat.KEY_WIDTH);
                    int height = format.getInteger(MediaFormat.KEY_HEIGHT);

                    extractor.selectTrack(i);
                    decoder = MediaCodec.createDecoderByType(mime);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        //decoder.setCallback(new DecodeCall());
                    }
                    decoder.configure(format, outputSurface, null, 0);
                    break;
                }
            }

            if(decoder == null){
                Log.e(TAG, "Can't find video info!");
            }

            decoder.start();
            if(DEBUG) Log.d(TAG, "Initialization done");

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void decodeFrameAt(long time){
        init();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ByteBuffer[] inputBuffer = decoder.getInputBuffers();
            ByteBuffer[] outputBuffer = decoder.getOutputBuffers();
            BufferInfo info = new BufferInfo();
            boolean isEOS = false;

            for(;;){
                if(!isEOS){
                    int inputBufferId = decoder.dequeueInputBuffer(timeout);
                    if(inputBufferId >= 0){
                        ByteBuffer buffer = inputBuffer[inputBufferId];
                        int sampleSize = extractor.readSampleData(buffer, 0);
                        if(sampleSize < 0){
                            Log.d(TAG, "InputBuffer END OF STREAM");
                            decoder.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        }else{
                            decoder.queueInputBuffer(inputBufferId, 0, sampleSize, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }
            }
        }
    }

    // Releasing MediaExtractor and MediaCodec
    public void release(){
        if(extractor != null) extractor.release();

        if(decoder != null){
            decoder.stop();
            decoder.release();
        }
    }

}
