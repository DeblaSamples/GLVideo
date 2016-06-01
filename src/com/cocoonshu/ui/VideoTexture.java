package com.cocoonshu.ui;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodec.Callback;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodec.CodecException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

/**
 * Video texture view
 * @author Cocoonshu
 * @date 2016-05-31 15:43:32
 */
public class VideoTexture extends TextureView implements SurfaceTextureListener {
    
    private static final String TAG = "VideoTexture";
    
    private MediaExtractor   mExtractor        = null;
    private MediaFormat      mFormat           = null;
    private MediaCodec       mCodec            = null;
    private MediaInfo        mInformation      = null;
    private FrameSyncHandler mFrameSyncHandler = new FrameSyncHandler();
    private Surface          mSurface          = null;
    
    public static class MediaInfo {
        public String filePath    = null;
        public String mimeType    = null;
        public int    width       = 0;
        public int    height      = 0;
        public long   duration    = 0;
        public int    sampleRate  = 0;
        public int    bitRate     = 0;
        public int    channelSize = 0;
        public int    trackSize   = 0;
    }
    
    public static class FrameSyncHandler implements Handler.Callback {
        private HandlerThread mHanlderThread = null;
        private Handler       mInnerHandler  = null;
        private long          mStartTimeUS   = -1;
        private boolean       mIsRunning     = true;
        
        public class Frame {
            BufferInfo info;
            int        bufferID;
            MediaCodec codec;
        }
        
        public FrameSyncHandler() {
            mHanlderThread = new HandlerThread("FrameSyncHandlerThread");
            mHanlderThread.start();
            mInnerHandler = new Handler(mHanlderThread.getLooper(), FrameSyncHandler.this);
        }

        @Override
        public boolean handleMessage(Message message) {
            Frame frame      = (Frame) message.obj;
            long  timestamps = frame.info.presentationTimeUs;
            if (mIsRunning) {
                frame.codec.releaseOutputBuffer(frame.bufferID, true);
            }
            message.recycle();
            
            Log.e(TAG, String.format("[handleMessage] timestamps = %02d:%02d:%02d:%04d",
                    (timestamps / 3600000000L),      // Hour
                    ((timestamps / 60000000L) % 60), // Minute
                    ((timestamps / 1000000L) % 60),  // Second
                    ((timestamps / 1000L) % 1000L)   // Millisecond
                ));
            return true;
        }

        public void resetTimer() {
            mStartTimeUS = -1;
        }
        
        public void syncTimestamp() {
            if (mStartTimeUS == -1) {
                mStartTimeUS = SystemClock.uptimeMillis();
            }
        }
        
        public void terminate() {
            mIsRunning = false;
        }
        
        public void syncFrame(BufferInfo bufferInfo, MediaCodec codec, int bufferID) {
            Message msg    = mInnerHandler.obtainMessage();
            Frame   frame  = new Frame();
            frame.bufferID = bufferID;
            frame.codec    = codec;
            frame.info     = bufferInfo;
            msg.obj        = frame;
            if (!mInnerHandler.sendMessageAtTime(msg, mStartTimeUS + bufferInfo.presentationTimeUs / 1000)) {
                mInnerHandler.sendMessage(msg);
            }
        }
    }
    
    public VideoTexture(Context context) {
        this(context, null);
    }

    public VideoTexture(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public VideoTexture(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSurfaceTextureListener(this);
    }

    @SuppressLint("NewApi")
    public VideoTexture(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setSurfaceTextureListener(this);
    }

    public boolean setDataSet(String path) throws IOException {
        if (path != null) {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(path);
            if (mExtractor.getTrackCount() > 0) {
                mInformation             = new MediaInfo();
                mFormat                  = mExtractor.getTrackFormat(0);
                mInformation.filePath    = path;
                mInformation.mimeType    = mFormat.getString(MediaFormat.KEY_MIME);
                mInformation.duration    = mFormat.getLong(MediaFormat.KEY_DURATION);
                mInformation.width       = mFormat.getInteger(MediaFormat.KEY_WIDTH);
                mInformation.height      = mFormat.getInteger(MediaFormat.KEY_HEIGHT);
                mCodec = MediaCodec.createDecoderByType(mInformation.mimeType);
                
                if (mExtractor.getTrackCount() > 0) {
                    mExtractor.selectTrack(0);
                }
                
                if (mCodec != null) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }
    
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        mSurface = new Surface(surfaceTexture);
        if (mInformation != null && mFormat != null && mCodec != null) {
            mCodec.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            mCodec.configure(mFormat, mSurface, null, 0);
            mCodec.setCallback(new Callback() {

                @Override
                public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                    // do nothing
                }

                @Override
                public void onInputBufferAvailable(MediaCodec codec, final int bufferID) {
                    ByteBuffer inputBuffer = mCodec.getInputBuffer(bufferID);
                    long       timestamps  = mExtractor.getSampleTime();
                    int        sampleSize  = mExtractor.readSampleData(inputBuffer, 0);
                    
                    if (sampleSize < 0) {
                        mCodec.queueInputBuffer(bufferID, 0, 0, timestamps, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        mFrameSyncHandler.syncTimestamp();
                        mCodec.queueInputBuffer(bufferID, 0, sampleSize, timestamps, 0);
                        mExtractor.advance();
                    }
                }

                @Override
                public void onOutputBufferAvailable(MediaCodec codec, final int bufferID, BufferInfo bufferInfo) {
                    if (bufferID > 0) {
                        VideoTexture.this.mFrameSyncHandler.syncFrame(bufferInfo, codec, bufferID);
                    }
                }
                
                @Override
                public void onError(MediaCodec codec, CodecException exp) {
                    Log.e(TAG, "[onError]", exp);
                }
                
            });
            mCodec.start();
            mFrameSyncHandler.resetTimer();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        // TODO Auto-generated method stub
    }
 
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (mCodec != null) {
            mFrameSyncHandler.terminate();
            mCodec.stop();
            mCodec.release();
            mExtractor.release();
        }
        return false;
    }
    
}
