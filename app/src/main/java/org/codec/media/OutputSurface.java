package org.codec.media;

import android.graphics.SurfaceTexture;
import android.view.Surface;

/**
 * Created by min on 10/08/2017.
 */

public class OutputSurface {

    private SurfaceTexture sTexture;
    private Surface outputSurface;
    private int textureId = -12345;

    private int width;
    private int height;

    public OutputSurface(int width, int height){
        this.width = width;
        this.height = height;
    }
}
