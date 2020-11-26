package com.android.grafika;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.VideoView;

import java.io.File;

public class VideoViewActivity extends Activity {
    private String[] mMovieFiles;
    private VideoView mVideoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_view);
        mVideoView = findViewById(R.id.video_view);
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mVideoView.start();
            }
        });
        mMovieFiles = MiscUtils.getFiles(getFilesDir(), "*.mp4");
        if (mMovieFiles != null && mMovieFiles.length > 0) {
            File videoFile = new File(getFilesDir(), mMovieFiles[0]);
            mVideoView.setVideoPath(videoFile.getAbsolutePath());
            mVideoView.start();
        }
    }
}
