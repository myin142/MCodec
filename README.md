# MCodec
MCodec is a pure java library that uses Androids MediaCodec to decode video frames and return it as bitmap. It uses the OpenGL converter from this [repository](https://github.com/kswlee/FrameGrabber) to convert the SurfaceTexture to a Bitmap.

# Usage
There is an example Activity in app/src/main/java/org/codec/DecodeActivity.

Get Single Frame
```java
// Create FrameGrab and Decoder
FrameGrab grab = new FrameGrab();
grab.setSource(filepath);
grab.init();

// Get Frame
grab.seekToFrame(frameNumber);
grab.getFrameAt(frameNumber);

// Only one option can be used, after usage Bitmap of FrameGrab will be null again
Bitmap frame = grab.getBitmap();  // Option 1: return Frame as Bitmap
grab.saveBitmap(location);        // Option 2: save Frame to Location
```

Get Frame Sequence
```java
// Create FrameGrab and Decoder
...

// Get Frame Sequence
grab.seekToFrame(frameStart);
for(int i = frameStart; i <= frameEnd; i++){
  grab.getFrameAt(i);
  Bitmap frame = grab.getBitmap();  // Option 1
}
```

Get Single Frame with Time
```java
// Create FrameGrab and Decoder
...

// Get Frame
grab.seekToTime(timeInSec);
grab.getFrameAtTime(timeInSec);
```

If you want to decode a frame that has already been decoded use resetDecoder(). E.g: Decoded 0-100 Frame, now want Frame 1 again.
```java
// Create FrameGrab and Decoder
...

//Get Frame Sequence
...

grab.resetDecoder();
grab.seekToFrame(FrameOne);
grab.getFrameAt(FrameOne);
```

If you want to change the target size of the frame, use setTargetSize() before init().
```java
// Create FrameGrab and Decoder
FrameGrab grab = new FrameGrab();
grab.setSource(filePath);
grab.setTargetSize(1920, 1080);
grab.init();
```

And always release() if you do not need FrameGrab anymore or want to create a new FrameGrab with a new Source.
```java
grab.release();

// Create new FrameGrab with new Source
grab = new FrameGrab();
grab.setSource(newFilePath);
...
```

To check if End-Of-Sream is reached use isEOS().
```java
grab.isEOS();
```

To get Informations about the video. You can easily add your own function to get information. MediaFormat or MediaExtractor can be used for that.
```java
grab.getFrameRate();
grab.getDuration();
```
