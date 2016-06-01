package com.cocoonshu.ui;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

/**
 * Camera texture view
 * @author Cocoonshu
 * @date 2016-06-01 19:39:31
 */
public class CameraTexture extends TextureView implements SurfaceTextureListener {
    
    private static final String TAG = "CameraTexture";
    
    private Camera mCamera = null;
    
    public CameraTexture(Context context) {
        this(context, null);
    }

    public CameraTexture(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public CameraTexture(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    @SuppressLint("NewApi")
    public CameraTexture(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setSurfaceTextureListener(this);
    }
    
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        try {
            mCamera = Camera.open();
            if (mCamera != null) {
                mCamera.setPreviewTexture(surfaceTexture);
                mCamera.setDisplayOrientation(0);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        // TODO do nothing
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        // TODO do nothing
    }
 
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
        return true;
    }
}
