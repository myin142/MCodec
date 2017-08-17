package com.tam.media;

import com.tam.gl.GLHelper;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.util.ArrayList;

public class FrameGrabber {
	final static String TAG = "FrameGrabber";
	
	private HandlerThread mGLThread = null;
	private Handler mGLHandler = null;
	private GLHelper mGLHelper = null;

	private Surface surface = null;
	private SurfaceTexture sTexture = null;
	private int textureID;
	private int mDefaultTextureID = 10001;
	
	private int mWidth = 1920;
	private int mHeight = 1080;
	
	private String mPath = null;
	
	public FrameGrabber() {
		mGLHelper = new GLHelper();
		mGLThread = new HandlerThread("FrameGrabber");
		mGLThread.start();
		mGLHandler = new Handler(mGLThread.getLooper());
	}
	
	public void setDataSource(String path) {
		mPath = path;
	}
	
	public void setTargetSize(int width, int height) {
		mWidth = width;
		mHeight = height;
	}
	
	public void init() {
		mGLHandler.post(new Runnable() {
			@Override
			public void run() {				
				SurfaceTexture st = new SurfaceTexture(mDefaultTextureID);
				st.setDefaultBufferSize(mWidth, mHeight);
				mGLHelper.init(st);
			}
		});
	}

	public void createSurface(){
		textureID = mGLHelper.createOESTexture();
		sTexture = new SurfaceTexture(textureID);
		surface = new Surface(sTexture);
	}
	
	public void release() {
		mGLHandler.post(new Runnable() {
			@Override
			public void run() {
				mGLHelper.release();
				mGLThread.quit();
			}			
		});		
	}
	
	private Object mWaitBitmap = new Object();
	private Bitmap mBitmap = null;
	private ArrayList<Bitmap> mBitmapArr = null;

	public Bitmap getFrameAt(final int frameNumber) {
		if (mPath == null || mPath.isEmpty()) {
			throw new RuntimeException("Illegal State");
		}
		
		mGLHandler.post(new Runnable() {
			@Override
			public void run() {
				getFrameAtImpl(frameNumber);
			}			
		});		
		
		synchronized (mWaitBitmap) {
			try {
				mWaitBitmap.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return mBitmap;
	}

	public void getFrameAtImpl(int frameNumber) {
        createSurface();
		final VideoDecoder vd = new VideoDecoder(mPath, surface);
		sTexture.setOnFrameAvailableListener(new OnFrameAvailableListener() {
			@Override
			public void onFrameAvailable(SurfaceTexture surfaceTexture) {
				//Log.i(TAG, "onFrameAvailable");
				mGLHelper.drawFrame(sTexture, textureID);
				mBitmap = mGLHelper.readPixels(mWidth, mHeight);
				synchronized (mWaitBitmap) {						
					mWaitBitmap.notify();						
				}
				
				vd.release();
				sTexture.release();
				surface.release();
			}			
		});
		vd.prepare(frameNumber);
	}

	public ArrayList<Bitmap> getFrameAt(final int start, final int end) {
		if (mPath == null || mPath.isEmpty()) {
			throw new RuntimeException("Illegal State");
		}

		mGLHandler.post(new Runnable() {
			@Override
			public void run() {
				getFrameAtImpl(start, end);
			}
		});

		synchronized (mWaitBitmap) {
			try {
				mWaitBitmap.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return mBitmapArr;
	}

	public void getFrameAtImpl(int start, int end) {
		createSurface();
		final VideoDecoder vd = new VideoDecoder(mPath, surface);
        final int size = end - start;
		mBitmapArr = new ArrayList<>();
		sTexture.setOnFrameAvailableListener(new OnFrameAvailableListener() {
			@Override
			public void onFrameAvailable(SurfaceTexture surfaceTexture) {
				mGLHelper.drawFrame(sTexture, textureID);
				Bitmap bm = mGLHelper.readPixels(mWidth, mHeight);
				mBitmapArr.add(bm);
				Log.d(TAG, "Save To Array");

				if(mBitmapArr.toArray().length >= size) {
					synchronized (mWaitBitmap) {
						mWaitBitmap.notify();
					}

					vd.release();
					sTexture.release();
					surface.release();
				}
			}
		});
		vd.prepare(start, end);
	}
}
