package org.codec;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.tam.media.FrameGrabber;

import org.jcodec.api.JCodecException;
import org.jcodec.api.android.AndroidFrameGrab;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class MainActivity extends AppCompatActivity{

	private static String videoFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/videos/";
	private static String hdLowVideo = videoFolder + "cats_with_timecode-1920x1080-30fps-baseline-4mbps.mp4";
	private static String hdHighVideo = videoFolder + "cats_with_timecode-1920x1080-30fps-main-14mbps.mp4";
	private static String hdHighBase = videoFolder + "cats_with_timecode-1920x1080-30fps-baseline-14mbps.mp4";
	private static String lowVideo = videoFolder + "cats_with_timecode-640x320-30fps-baseline-4mbps.mp4";
	private static String hdHigh24 = videoFolder + "cats_with_timecode-1920x1080-24fps-baseline-14mpbs.mp4";

	private static int startFrame = 0;
	private static int endFrame = 200;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        // Create Layout
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		long startTime = System.currentTimeMillis();

		decodeVideoSequence(lowVideo, startFrame, endFrame, false);
		/*try {
			decodeVideoSequence(hdLowVideo, startFrame, endFrame, false);
		} catch (IOException | JCodecException e) {
			e.printStackTrace();
		}*/
		long endTime = System.currentTimeMillis();
		long totalTime = (endTime - startTime) / 1000;

		Log.d("Total Time", totalTime + "s");
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

	private FrameGrabber grab = new FrameGrabber();
	private Bitmap getFrameAtTime(String path, long time){
		MediaMetadataRetriever mRetriever = new MediaMetadataRetriever();
		mRetriever.setDataSource(path);
		int height = Integer.parseInt(mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
		int width = Integer.parseInt(mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));

		grab.setDataSource(path);
		grab.setTargetSize(width, height);
		grab.init();
		Bitmap bm = grab.getFrameAtTime(time);
		//grab.release();
		return bm;
	}

	// Get Frame with FrameGrabber
	void decodeVideoSequence(String filePath, int start, int end, boolean ffmpeg){
		for (int i = start; i <= end; i++) {
			int frame = i;
			//Bitmap bmp = getFrameAtTime(filePath, frame * 41666);
			Bitmap bmp;

			if(!ffmpeg){
				bmp = getFrameAtTime(filePath, frame * 33333);
			}else{
                FFmpegMediaMetadataRetriever mRetriever = new FFmpegMediaMetadataRetriever();
				mRetriever.setDataSource(filePath);
				bmp = mRetriever.getFrameAtTime(frame * 33333, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
			}

			String location = videoFolder + "frames/frame_"+i+".jpg";
			saveBitmap(bmp, location);
		}
		grab.release();
	}

	void decodeVideoSequence(String filePath, int start, int end, int width, int height, boolean ffmpeg){
		for (int i = start; i <= end; i++) {
			int frame = i;
			Bitmap bmp = null;

			if(!ffmpeg){
				bmp = getFrameAtTime(filePath, frame * 33333);
			}else{
				FFmpegMediaMetadataRetriever mRetriever = new FFmpegMediaMetadataRetriever();
				mRetriever.setDataSource(filePath);
				bmp = mRetriever.getFrameAtTime(frame * 33333, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
			}

			if(bmp != null)
                bmp = resizeBitmap(bmp, width, height);

			String location = videoFolder + "frames/frame_"+i+".jpg";
			saveBitmap(bmp, location);
		}
	}

	Bitmap resizeBitmap(Bitmap bm, int width, int height){
		return Bitmap.createScaledBitmap(bm, width, height, false);
	}

	void decodeVideoSequence(String filePath, int start, int end, int width, int height) throws IOException, JCodecException{
		FileChannelWrapper ch = NIOUtils.readableChannel(new File(filePath));
		AndroidFrameGrab frameGrab = AndroidFrameGrab.createAndroidFrameGrab(ch);

		for (int i = 0; i < end; i++) {
			Bitmap frame = resizeBitmap(frameGrab.getFrame(), width, height);
			String location = videoFolder + "sequence/frame_"+i+".jpg";
			saveBitmap(frame, location);
		}
	}

	void decodeVideoSequence(String filePath, int start, int end) throws IOException, JCodecException{
		FileChannelWrapper ch = NIOUtils.readableChannel(new File(filePath));
		AndroidFrameGrab frameGrab = AndroidFrameGrab.createAndroidFrameGrab(ch);

		for (int i = 0; i < end; i++) {
			Bitmap frame = frameGrab.getFrame();
			String location = videoFolder + "sequence/frame_"+i+".jpg";
			saveBitmap(frame, location);
		}
	}

}
