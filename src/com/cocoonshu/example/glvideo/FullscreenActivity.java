
package com.cocoonshu.example.glvideo;

import java.io.IOException;

import com.cocoonshu.ui.VideoTexture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Entry activity
 * @author Cocoonshu
 * @date 2016-06-01 18:59:36
 */
public class FullscreenActivity extends Activity {

    private static final int KEY_PICK_LEFT_VIDEO  = 0x0001;
    private static final int KEY_PICK_RIGHT_VIDEO = 0x0002;
    
    private Button       mButtonOpenVideoLeft  = null;
    private Button       mButtonOpenVideoRight = null;
    private VideoTexture mVideoTextureLeft     = null;
    private VideoTexture mVideoTextureRight    = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        
        setupViews();
        setupListeners();
    }


    private void setupViews() {
        mButtonOpenVideoLeft  = (Button) findViewById(R.id.Button_OpenVideoLeft);
        mButtonOpenVideoRight = (Button) findViewById(R.id.Button_OpenVideoRight);
        mVideoTextureLeft     = (VideoTexture) findViewById(R.id.VideoTextureLeft);
        mVideoTextureRight    = (VideoTexture) findViewById(R.id.VideoTextureRight);
    }
    
    private void setupListeners() {
        mButtonOpenVideoLeft.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                startImagePicker(KEY_PICK_LEFT_VIDEO);
            }
            
        });
        
        mButtonOpenVideoRight.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                startImagePicker(KEY_PICK_RIGHT_VIDEO);
            }
            
        });
    }
    
    private void startImagePicker(int action) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);  
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE); 
        startActivityForResult(
                Intent.createChooser(
                        intent,
                        getResources().getString(R.string.open_file_dialog_title)
                ),
                action);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && data.getData() != null) {
            String filePath = getPath(FullscreenActivity.this, data.getData());
            if (requestCode == KEY_PICK_LEFT_VIDEO) {
                mButtonOpenVideoLeft.setTag(filePath);
                mButtonOpenVideoLeft.setVisibility(View.GONE);
            } else if (requestCode == KEY_PICK_RIGHT_VIDEO) {
                mButtonOpenVideoRight.setTag(filePath);
                mButtonOpenVideoRight.setVisibility(View.GONE);
            }
            
            // Setup data set to video textures after both left side and right side video files are ready
            String  leftVideoFilePath  = (String) mButtonOpenVideoLeft.getTag();
            String  rightVideoFilePath = (String) mButtonOpenVideoRight.getTag();
            boolean canPlayback        = leftVideoFilePath != null && rightVideoFilePath != null;
            if (canPlayback) {
                try {
                    mVideoTextureLeft.setDataSet(leftVideoFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                    // Do something to handle error
                }
                try {
                    mVideoTextureRight.setDataSet(rightVideoFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                    // Do something to handle error
                }
            }
        }
    }
    
    public static String getPath(Context context, Uri uri) { 
        if ("content".equalsIgnoreCase(uri.getScheme())) { 
            String[] projection = { MediaStore.Images.Media.DATA }; 
            Cursor cursor = null; 
  
            try { 
                cursor = context.getContentResolver().query(uri, projection,null, null, null); 
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA); 
                if (cursor.moveToFirst()) { 
                    return cursor.getString(column_index); 
                } 
            } catch (Exception e) { 
                // Eat it 
            } 
        } else if ("file".equalsIgnoreCase(uri.getScheme())) { 
            return uri.getPath(); 
        } 
  
        return null; 
    } 
}
