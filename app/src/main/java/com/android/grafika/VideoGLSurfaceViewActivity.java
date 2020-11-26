package com.android.grafika;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Surface;

import com.android.grafika.gles.ShaderUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

public class VideoGLSurfaceViewActivity extends Activity {
    private GLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_glsurfaceview);
        mGLSurfaceView = findViewById(R.id.gl_surface_view);
        mGLSurfaceView.setEGLContextClientVersion(3);
        mGLSurfaceView.setRenderer(new GLRenderer(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    private class GLRenderer implements GLSurfaceView.Renderer {

        private Context mContext;

        private SurfaceTexture mSurfaceTexture;
        private MediaPlayer mMediaPlayer;
        private String[] mMovieFiles;

        //渲染程序
        private int mProgram = -1;
        private FloatBuffer vertexBuffer;
        private FloatBuffer textBuffer;
        private FloatBuffer colorBuffer;

        //3个定点，等腰直角
        float triangleCoords[] ={
                0.5f,  0.5f, 0.0f, // top
                0.5f, -0.5f, 0.0f, // bottom left
                -0.5f, -0.5f, 0.0f  // bottom right
        };

        float textCoords[] ={
                1.0f,  1.0f, // top
                1.0f, 0.0f, // bottom left
                0.0f, 0.0f  // bottom right
        };

        private float color[] = {
                0.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f
        };

        // 纹理
        private int textureId;
        private boolean mUpdateTexture = false;

        public GLRenderer(Context context) {
            mContext = context;
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(triangleCoords.length*4);
            byteBuffer.order(ByteOrder.nativeOrder());
            vertexBuffer = byteBuffer.asFloatBuffer();
            vertexBuffer.put(triangleCoords);
            vertexBuffer.position(0);

            byteBuffer = ByteBuffer.allocateDirect(textCoords.length*4);
            byteBuffer.order(ByteOrder.nativeOrder());
            textBuffer = byteBuffer.asFloatBuffer();
            textBuffer.put(textCoords);
            textBuffer.position(0);

            colorBuffer = ByteBuffer.allocateDirect(color.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            colorBuffer.put(color);
            colorBuffer.position(0);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES30.glClearColor(0.0f,0.0f,0.0f,1.0f);
            initProgram();
            initSurfaceTexture();
            initMediaPlayer();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES30.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            updateTexture();
            draw();
        }

        private void initProgram() {
            //编译顶点着色程序
            String vertexShaderStr = ShaderUtil.loadAssets(mContext, "vertex_texture_2d.glsl");
            int vertexShaderId = ShaderUtil.compileVertexShader(vertexShaderStr);
            //编译片段着色程序
            String fragmentShaderStr = ShaderUtil.loadAssets(mContext, "fragment_texture_2d.glsl");
            int fragmentShaderId = ShaderUtil.compileFragmentShader(fragmentShaderStr);
            //连接程序
            mProgram = ShaderUtil.linkProgram(vertexShaderId, fragmentShaderId);
            //在OpenGLES环境中使用程序
            GLES30.glUseProgram(mProgram);
        }

        private void initSurfaceTexture() {
            int[] textures = new int[1];
            GLES30.glGenTextures(1, textures, 0);
            textureId = textures[0];
            GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
            GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

            mSurfaceTexture = new SurfaceTexture(textureId);
            mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    synchronized (this) {
                        mUpdateTexture = true;
                    }
                }
            });
        }

        private void initMediaPlayer() {
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
                    mMediaPlayer.setSurface(new Surface(mSurfaceTexture));
                    mMediaPlayer.start();
                }
            });
            mMovieFiles = MiscUtils.getFiles(getFilesDir(), "*.mp4");
            if (mMovieFiles != null && mMovieFiles.length > 0) {
                File videoFile = new File(getFilesDir(), mMovieFiles[0]);
                try {
                    mMediaPlayer.setDataSource(videoFile.getAbsolutePath());
                    mMediaPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void updateTexture() {
            synchronized (this) {
                if (mUpdateTexture) {
                    mSurfaceTexture.updateTexImage();
                    mUpdateTexture = false;
                }
            }
        }

        private void draw() {
            GLES30.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            GLES30.glClear(GL10.GL_COLOR_BUFFER_BIT
                    | GL10.GL_DEPTH_BUFFER_BIT);

            GLES30.glVertexAttribPointer(0,3, GLES30.GL_FLOAT,false,0, vertexBuffer);
            GLES30.glEnableVertexAttribArray(0);

            GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, 0, colorBuffer);
            GLES30.glEnableVertexAttribArray(1);

            GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, 0, textBuffer);
            GLES30.glEnableVertexAttribArray(2);

            // 设置当前活动的纹理单元为纹理单元0
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            // 将纹理ID绑定到当前活动的纹理单元上
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            // 将纹理单元传递片段着色器的u_TextureUnit
            int uTextureLocation = GLES30.glGetUniformLocation(mProgram,"uTexture");
            GLES30.glUniform1i(uTextureLocation, 0);

            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3);

            //禁止顶点数组的句柄
            GLES30.glDisableVertexAttribArray(0);
            GLES30.glDisableVertexAttribArray(1);
            GLES30.glDisableVertexAttribArray(2);
        }
    }
}
