package com.android.grafika;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;

import java.io.File;
import java.io.IOException;

public class VideoTextureViewActivity extends Activity implements TextureView.SurfaceTextureListener {
    private TextureView mTextureView;
    private MediaPlayer mMediaPlayer;
    private String[] mMovieFiles;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_textureview);
        mTextureView = findViewById(R.id.texture_view);
        mTextureView.setSurfaceTextureListener(this);
        mMovieFiles = MiscUtils.getFiles(getFilesDir(), "*.mp4");
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mMediaPlayer.start();
            }
        });
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMediaPlayer.start();
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mMovieFiles != null && mMovieFiles.length > 0) {
            File videoFile = new File(getFilesDir(), mMovieFiles[0]);
            try {
                mMediaPlayer.setDataSource(videoFile.getAbsolutePath());
                mMediaPlayer.setSurface(new Surface(surface));
                mMediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
