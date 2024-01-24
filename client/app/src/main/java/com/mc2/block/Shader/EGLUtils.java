package com.mc2.block.Shader;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES30;
import android.util.Log;

import com.mc2.Utils.Config;

import java.nio.IntBuffer;

//用来给当前线程创建一个EGL环境
public class EGLUtils {

    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
    private EGLContext mEGLCtx = EGL14.EGL_NO_CONTEXT;
    private EGLDisplay mEGLDis = EGL14.EGL_NO_DISPLAY;


    //egl
    private EGLContext mSharedCtx;
    private EGLConfig mSharedCfg;

    //init, must run on parent EGL thread
    public  EGLUtils(){
        // 获取Unity线程的EGLContext,EGLDisplay
        mSharedCtx=EGL14.eglGetCurrentContext();
        if(mSharedCtx!= EGL14.EGL_NO_CONTEXT){
            Log.d(Config.TAG_SHADER,"get share context");
        }else {
            Log.d(Config.TAG_SHADER,"Error: get EGL_NO_CONTEXT");
            return;
        }
        EGLDisplay sharedDis = EGL14.eglGetCurrentDisplay();
        if(sharedDis!= EGL14.EGL_NO_DISPLAY){
            Log.d(Config.TAG_SHADER,"get share EGLDisplay");
        }else {
            Log.d(Config.TAG_SHADER,"Error: get EGL_NO_DISPLAY");
            return;
        }

        // 获取绘制线程的EGLConfig
        int[] numEglConfigs = new int[1];
        EGLConfig[] eglConfigs = new EGLConfig[1];
        if (!EGL14.eglGetConfigs(sharedDis, eglConfigs, 0, eglConfigs.length,numEglConfigs, 0)){
            Log.w(Config.TAG_SHADER, "unable to get suitable EGLConfig");
        }
        mSharedCfg = eglConfigs[0];
        Log.d(Config.TAG_SHADER,"init EGLUtils "+Thread.currentThread().getName()+" id "+Thread.currentThread().getId());
    }

    //create sub context in sub EGL thread
    public void create_SubCtx_from_SharedCtx(){
        //获取默认显示设备
        mEGLDis = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if(mEGLDis == EGL14.EGL_NO_DISPLAY){
            throw new RuntimeException("eglGetDisplay failed");
        }

        //初始化默认显示设备
        int[] version = {1,4};  //EGL 1.4
        if(!EGL14.eglInitialize(mEGLDis, version, 0, version, 1)){
            throw new RuntimeException("eglInitialize failed");
        }

        //指定使用的opengl版本
        int[] ctxAttr = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, // 版本需要一致
                EGL14.EGL_NONE
        };
        mEGLCtx = EGL14.eglCreateContext(mEGLDis, mSharedCfg, mSharedCtx, ctxAttr, 0);
        if(mEGLCtx!= EGL14.EGL_NO_CONTEXT){
            Log.d(Config.TAG_SHADER,"get thread context!!!!");
        }else {
            Log.d(Config.TAG_SHADER,"get EGL_NO_CONTEXT, return");
            return;
        }

        //创建离屏缓存,不用于渲染，所以给的很小
        int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, 64,
                EGL14.EGL_HEIGHT, 64,
                EGL14.EGL_NONE
        };
        mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDis, mSharedCfg, surfaceAttribs, 0);

        //绑定当前线程
        if(!EGL14.eglMakeCurrent(mEGLDis, mEGLSurface, mEGLSurface, mEGLCtx)){
            Log.e(Config.TAG_SHADER,"MakeCurrent false");
        }
        GLES30.glFlush();
        Log.d(Config.TAG_SHADER,"create sub EGL ctx on "+Thread.currentThread().getName()+" id "+Thread.currentThread().getId());
    }


    //渲染
    public void eglSwapBuffers(){
        EGL14.eglSwapBuffers(mEGLDis, mEGLSurface);
    }

    //获取当前context
    public EGLContext getCurContext() {
        return mEGLCtx;
    }

    public static void hasDepth(){

        IntBuffer depthFormat = IntBuffer.allocate(1);
        // 获取深度附件的内部格式
        GLES30.glGetFramebufferAttachmentParameteriv(GLES30.GL_FRAMEBUFFER,
                GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, depthFormat);
        if (depthFormat.get(0) == GLES30.GL_RENDERBUFFER) {
            // 深度附件是一个渲染缓冲区
            Log.e(Config.TAG_SHADER,"renderBuffer");

        } else if (depthFormat.get(0) == GLES30.GL_TEXTURE) {
            // 深度附件是一个纹理
            Log.e(Config.TAG_SHADER,"texture");
        } else {
            // 没有深度附件
            Log.e(Config.TAG_SHADER,"none"+ depthFormat.get(0));
        }

//        int size = mWidth * mHeight * 4; // assuming DEPTH16 format
//        ByteBuffer buffer = ByteBuffer.allocate(size);
//        GLES30.glReadPixels((int)0, (int)0, mWidth, mHeight, GL_DEPTH_COMPONENT, GL_FLOAT,buffer);

    }

    //销毁
    public void release(){
        if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(mEGLDis, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(mEGLDis, mEGLSurface);
            mEGLSurface = EGL14.EGL_NO_SURFACE;
        }
        if (mEGLCtx != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(mEGLDis, mEGLCtx);
            mEGLCtx = EGL14.EGL_NO_CONTEXT;
        }
        if (mEGLDis != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(mEGLDis);
            mEGLDis = EGL14.EGL_NO_DISPLAY;
        }
    }



}
