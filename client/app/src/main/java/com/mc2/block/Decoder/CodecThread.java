package com.mc2.block.Decoder;

import android.graphics.SurfaceTexture;
import android.os.HandlerThread;
import android.util.Log;

import com.mc2.Utils.CheckUtils;
import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;
import com.mc2.block.Shader.CodecShader;
import com.mc2.block.Shader.EGLUtils;
import com.mc2.block.Shader.ShaderUtils;


public class CodecThread extends HandlerThread {
    public EGLUtils mEGLUtils=null;
    public CodecShader mCodecShader;
    private int mWidth;
    private int mHeight;

    public SurfaceTexture mSurfaceTexture=null;

    public CodecThread(String name, EGLUtils eglUtils, int width, int height) {
        super(name,Config.DECODER_THREAD_PRIORITY);
        if(DebugCfg.CHECK_INPUT && DebugCfg.CHECK_INPUT){
            CheckUtils.checkObjIsNull(name);
            CheckUtils.checkObjIsNull(eglUtils);
        }
        mEGLUtils=eglUtils;
        mWidth=width;
        mHeight=height;
    }

    @Override
    public void run() {
        //创建egl环境
        mEGLUtils.create_SubCtx_from_SharedCtx();
        //init FBO and surfaceTexture
        int oesTextureId = ShaderUtils.createOESTextureID(mWidth,mHeight);
        mSurfaceTexture = new SurfaceTexture(oesTextureId);
        mSurfaceTexture.setDefaultBufferSize(mWidth, mHeight);
        mCodecShader=new CodecShader(oesTextureId);

        ShaderUtils.logCurrent("codecThread");
        super.run();
    }

    @Override
    public boolean quitSafely() {
        Log.d(Config.TAG_DECODER,"CodecThread release egl ctx");
        mEGLUtils.release();
        mSurfaceTexture.release();
        return super.quitSafely();
    }
}
