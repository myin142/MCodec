package org.codec.media;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

import org.codec.gl.GLHelper;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by min on 22/08/2017.
 */

public class CodecOutput implements SurfaceTexture.OnFrameAvailableListener{
    String TAG = "CodecOutput";

    GLHelper mGLHelper = null;

    int mDefaultTextureID = 10001;
    int mWidth = 640;
    int mHeight = 360;

    static final Object mWaitFrame = new Object();
    static boolean mFrameAvailable = false;

    SurfaceTexture sTexture = null;
    Surface surface = null;
    int textureID;

    Bitmap frame = null;

    // Create GLHelper and Surface
    public void init(){
        mGLHelper = new GLHelper();
        SurfaceTexture st = new SurfaceTexture(mDefaultTextureID);
        st.setDefaultBufferSize(mWidth, mHeight);
        mGLHelper.init(st);

        textureID = mGLHelper.createOESTexture();
        sTexture = new SurfaceTexture(textureID);
        sTexture.setOnFrameAvailableListener(this);
        surface = new Surface(sTexture);
    }

    public void setWidth(int width){
        mWidth = width;
    }
    public void setHeight(int height){
        mHeight = height;
    }

    public Surface getSurface(){
        return surface;
    }

    // Get Bitmap, only once
    public Bitmap getBitmap(){
        Bitmap bm = frame;
        frame = null;
        return bm;
    }

    // Wait for FrameProcessed()
    public static void awaitFrame(){
        synchronized (mWaitFrame) {
            try {
                mWaitFrame.wait();
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
            mFrameAvailable = false;
        }
    }

    // On Codec Frame Available, save Frame as Bitmap
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "Frame available");

        mGLHelper.drawFrame(sTexture, textureID);
        frame = mGLHelper.readPixels(mWidth, mHeight);
    }

    // Notify awaitFrame() to continue
    public void frameProcessed(){
        synchronized (mWaitFrame) {
            if (mFrameAvailable) {
                throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
            }
            mFrameAvailable = true;
            mWaitFrame.notifyAll();
        }
    }

    public void release(){
        sTexture.release();
        surface.release();
        mGLHelper.release();
    }

}
