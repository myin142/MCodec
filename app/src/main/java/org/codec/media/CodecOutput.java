package org.codec.media;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

import org.codec.gl.GLHelper;

public class CodecOutput implements SurfaceTexture.OnFrameAvailableListener{
    String TAG = "CodecOutput";
    boolean DEBUG = FrameGrab.DEBUG;

    GLHelper mGLHelper = null;

    int mDefaultTextureID = 10001;
    int mWidth = 640;
    int mHeight = 360;

    final Object mWaitFrame = new Object();

    SurfaceTexture sTexture = null;
    Surface surface = null;
    int textureID;

    Bitmap frame = null;

    // Create GLHelper and Surface
    public void init(){
        if(DEBUG) Log.d(TAG, "Creating GlHelper and Surface");
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
    public void awaitFrame(){
        if(DEBUG) Log.d(TAG, "Waiting for FrameAvailable");
        synchronized (mWaitFrame) {
            try {
                mWaitFrame.wait();
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }
    }

    // On Codec Frame Available, save Frame as Bitmap
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if(DEBUG) Log.d(TAG, "Frame available");

        mGLHelper.drawFrame(sTexture, textureID);
        frame = mGLHelper.readPixels(mWidth, mHeight);
        frameProcessed();
    }

    // Notify awaitFrame() to continue
    public void frameProcessed(){
        if(DEBUG) Log.d(TAG, "Frame Processed");
        synchronized (mWaitFrame) {
            mWaitFrame.notifyAll();
        }
    }

    public void release(){
        if(sTexture != null)
            sTexture.release();

        if(surface != null)
            surface.release();

        if(mGLHelper != null)
            mGLHelper.release();

        sTexture = null;
        surface = null;
        mGLHelper = null;
    }

}

