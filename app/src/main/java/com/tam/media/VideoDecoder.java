package com.tam.media;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

public class VideoDecoder {
	final static String TAG = "VideoDecoder";
	final static String VIDEO_MIME_PREFIX = "video/"; 
	
	private MediaExtractor mMediaExtractor = null;
	private MediaCodec mMediaCodec = null;
	
	private Surface mSurface = null;
	private String mPath = null;	
	private int mVideoTrackIndex = -1;
	private int timeout = 10000;
	
	public VideoDecoder(String path, Surface surface) {
		mPath = path;
		mSurface = surface;
		
		initCodec();
	}

	public int getFrameRate(){
		int frameRate = 24; //may be default
        int numTracks = mMediaExtractor.getTrackCount();
        for (int i = 0; i < numTracks; ++i) {
            MediaFormat format = mMediaExtractor.getTrackFormat(i);
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

	public void release() {
		if (null != mMediaCodec) {
			mMediaCodec.stop();
			mMediaCodec.release();
		}
		
		if (null != mMediaExtractor) {
			mMediaExtractor.release();
		}
	}

	public void prepare(int frame){
		// Set Variables and go to specified time in video
		BufferInfo info = new BufferInfo();
		long time = frame * getFrameRate();
		mMediaExtractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

		decodeFrameAt(time, info);
	}

	public void prepare(int startFrame, int lastFrame){
		// Set Variables and go to specified time in video
		BufferInfo info = new BufferInfo();
		int fps = getFrameRate();
		long startTime = startFrame * fps;
		long endTime = lastFrame * fps;
		mMediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

		for(long i = startTime; i <= endTime; i+=fps){
			decodeFrameAt(i, info);
		}
	}
	
	private boolean initCodec() {
		//Log.i(TAG, "initCodec");
		mMediaExtractor = new MediaExtractor();
		try {
			mMediaExtractor.setDataSource(mPath);
		} catch (IOException e) {			
			e.printStackTrace();
			return false;
		}
		
		int trackCount = mMediaExtractor.getTrackCount();
		for (int i = 0; i < trackCount; ++i) {
			MediaFormat mf = mMediaExtractor.getTrackFormat(i);
			String mime = mf.getString(MediaFormat.KEY_MIME);
			if (mime.startsWith(VIDEO_MIME_PREFIX)) {
				mVideoTrackIndex = i;
				break;
			}
		}
		if (mVideoTrackIndex < 0) 
			return false;
		
		mMediaExtractor.selectTrack(mVideoTrackIndex);
		MediaFormat mf = mMediaExtractor.getTrackFormat(mVideoTrackIndex);
		String mime = mf.getString(MediaFormat.KEY_MIME);
		try {
			mMediaCodec = MediaCodec.createDecoderByType(mime);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mMediaCodec.configure(mf, mSurface, null, 0);
		mMediaCodec.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
		mMediaCodec.start();
		//Log.i(TAG, "initCodec end");
		
		return true;
	}

	private void decodeFrameAt(long time, BufferInfo info){

		// While render frame not reached
		boolean render = false;
		while (!render) {
			// On Input Available
			int inputId = mMediaCodec.dequeueInputBuffer(timeout);
			if (inputId >= 0) {
				// Read Data
				ByteBuffer buffer = mMediaCodec.getInputBuffer(inputId);
				int sample = 0;
				if (buffer != null) {
					sample = mMediaExtractor.readSampleData(buffer, 0);
				}
				long presentationTime = mMediaExtractor.getSampleTime();

				// Queue Input and continue
				if (sample < 0) {
					mMediaCodec.queueInputBuffer(inputId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
				} else {
					mMediaCodec.queueInputBuffer(inputId, 0, sample, presentationTime, 0);
					mMediaExtractor.advance();
				}

			}

			// On Output Available
			int outputId = mMediaCodec.dequeueOutputBuffer(info, timeout);
			if (outputId >= 0) {
				// If video time equals searched time, then render
				if (info.presentationTimeUs >= time) render = true;
				mMediaCodec.releaseOutputBuffer(outputId, render);
				Log.d(TAG, "Release Output, InfoTime = "+info.presentationTimeUs+",render = " + render);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/*private boolean mIsInputEOS = false;
	private boolean decodeFrameAt(long timeUs) {
		//Log.i(TAG, "decodeFrameAt " + timeUs);
		mMediaExtractor.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
		//Log.i(TAG, "sampleTime " + mMediaExtractor.getSampleTime());

		mIsInputEOS = false;
		CodecState inputState = new CodecState();
		CodecState outState = new CodecState();
		boolean reachTarget = false;
		for (;;) {			
			if (!inputState.EOS)
				handleCodecInput(inputState);
						
			if (inputState.outIndex < 0) {
				handleCodecOutput(outState);
				reachTarget = processOutputState(outState, timeUs);
			} else {
				reachTarget = processOutputState(inputState, timeUs);
			}
			
			if (true == reachTarget || outState.EOS) {
				//Log.i(TAG, "decodeFrameAt " + timeUs + " reach target or EOS");
				break;
			}
			
			inputState.outIndex = -1;
			outState.outIndex = -1;
		}
		
		return reachTarget;
	}
	
	private boolean processOutputState(CodecState state, long timeUs) {
		if (state.outIndex < 0) 
			return false;
		
		if (state.outIndex >= 0 && state.info.presentationTimeUs < timeUs) {
			//Log.i(TAG, "processOutputState presentationTimeUs " + state.info.presentationTimeUs);
			mMediaCodec.releaseOutputBuffer(state.outIndex, false);
			return false;
		}
		
		if (state.outIndex >= 0) {
			//Log.i(TAG, "processOutputState presentationTimeUs " + state.info.presentationTimeUs);
			mMediaCodec.releaseOutputBuffer(state.outIndex, true);
			return true;
		}
		
		return false;
	}
	
	private class CodecState {
		int outIndex = -1;
		BufferInfo info = new BufferInfo();
		boolean EOS = false;
	}
	
	private void handleCodecInput(CodecState state) {
		ByteBuffer [] inputBuffer = null;
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            inputBuffer = mMediaCodec.getInputBuffers();
        }

		while(!mIsInputEOS) {
			int inputBufferIndex = mMediaCodec.dequeueInputBuffer(10000);
			if (inputBufferIndex < 0) 
				continue;

			ByteBuffer in;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				in = mMediaCodec.getInputBuffer(inputBufferIndex);
			}else{
				in = inputBuffer[inputBufferIndex];
			}
			int readSize = mMediaExtractor.readSampleData(in, 0);
			long presentationTimeUs = mMediaExtractor.getSampleTime();
			int flags = mMediaExtractor.getSampleFlags();

			//Log.i(TAG, "sampleTime before " + mMediaExtractor.getSampleTime());
			boolean EOS = !mMediaExtractor.advance();
			//Log.i(TAG, "sampleTime after " + mMediaExtractor.getSampleTime());
			EOS |= (readSize <= 0);
			EOS |= ((flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) > 0);
			
			//Log.i(TAG, "input presentationTimeUs " + presentationTimeUs + " isEOS " + EOS);
			
			if (EOS && readSize < 0) 
				readSize = 0;
			
			if (readSize > 0 || EOS) 
				mMediaCodec.queueInputBuffer(inputBufferIndex, 0, readSize, presentationTimeUs, flags | (EOS? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0));
			
			if (EOS) {
				state.EOS = true;
				mIsInputEOS = true;
				break;
			}
						
			state.outIndex = mMediaCodec.dequeueOutputBuffer(state.info, 10000);
			if (state.outIndex >= 0) 
				break;
		}
	}
	
	private void handleCodecOutput(CodecState state) {
		state.outIndex = mMediaCodec.dequeueOutputBuffer(state.info, 10000);
		if (state.outIndex < 0) {
			return;
		}
		
		if ((state.info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {	
			state.EOS = true;
			//Log.i(TAG, "reach output EOS " + state.info.presentationTimeUs);
		}
	}*/
}
