package com.mc2.block;

import android.content.Context;
import android.telecom.Call;
import android.util.Log;
import android.view.View;

import com.mc2.block.Common.Player;
import com.mc2.Utils.CheckUtils;
import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;
import com.mc2.block.Render.GLSurfaceViewEGL14;

/**
 * The Main Entry of TBSR Back end
 */
public class BLOCK implements GLSurfaceViewEGL14.Renderer {
    private GLSurfaceViewEGL14 mGLSurFaceView;
    public BLOCK(String fileDirPath, Context context,GLSurfaceViewEGL14 glSurfaceViewEGL14) {
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_TBSR){
            CheckUtils.checkObjIsNull(fileDirPath);
            CheckUtils.checkObjIsNull(glSurfaceViewEGL14);
        }
        mPlayer=new Player(fileDirPath,context);
        mGLSurFaceView=glSurfaceViewEGL14;
    }

    //解码
    private Player mPlayer;
    @Override
    public void onSurfaceCreated(android.opengl.EGLConfig config) {
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_TBSR){
            CheckUtils.checkObjIsNull(mGLSurFaceView);
        }
        mPlayer.prepare();
        mPlayer.start();
        mGLSurFaceView.setOnTouchListener(mPlayer.mCoreLooper.mViewPort.mOnTouchListener);
        //TODO toast info the init complete
        Log.w(Config.TAG_BLOCK,"onSurfaceCreated !!");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        mPlayer.mCoreLooper.mViewPort.onSurfaceChanged(width,height);
        mPlayer.mCoreLooper.mShader.onSurfaceChanged(width,height);
        Log.w(Config.TAG_BLOCK,"onSurfaceChanged width "+width+" height "+height);
    }

    @Override
    public boolean onDrawFrame() {
        return mPlayer.loop();
    }

    public void release(){
        if(mPlayer!=null){
            mPlayer.release();
        }
    }
}