package org.codec.media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import java.nio.ByteBuffer;

/**
 * Created by min on 10/08/2017.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DecodeCall extends MediaCodec.Callback {

    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
        ByteBuffer inputBuffer = mediaCodec.getInputBuffer(i);


    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
        ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(i);
        MediaFormat bufferFormat = mediaCodec.getOutputFormat(i);

        //mediaCodec.releaseOutputBuffer(i);
    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) { }

    @Override
    public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) { }
}
