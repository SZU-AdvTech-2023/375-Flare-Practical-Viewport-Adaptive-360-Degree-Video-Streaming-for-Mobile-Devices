package com.mc2.block.Common;

import android.content.Context;
import android.util.Log;

import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;
import com.mc2.Utils.PathCfg;
import com.mc2.block.Render.ViewPort;
import com.mc2.block.Shader.EGLUtils;


/**
 * center control when to do decision, frame rendering
 */
public class Player {

    private HttpClient mHttpClient;

    private VideoTrack mVideoTrack;

    private EGLUtils mEGLCtx;

    public CoreLooper mCoreLooper;

    private MediaSource mMediaSource;

    private TilingAdaptor mTilingAdaptor;

    private CodecManager mCodecManager;

    private Predictor mPredictor;

    private Context mContext;

    private ViewPort mViewPort;



    public Player(String fileDirPath, Context context){
        mContext=context;
        PathCfg.setPath(fileDirPath);
        Config.checkConfig();
    }

    //Should be run in UI thread
    public void prepare(){
        // init EGL context
        mEGLCtx =new EGLUtils();

        if(!Config.LOCAL_MODE){
            mHttpClient=new HttpClient();
            mHttpClient.reqMpd();
        }

        // init MediaSource, using shared thread
        mMediaSource= new MediaSource(mHttpClient);

        // init VideoTrack
        mVideoTrack=new VideoTrack(mMediaSource);

        mViewPort=new ViewPort(mContext);

        // start prediction, using shared thread
        mPredictor=new Predictor(mViewPort);

        // init TilingAdaptor, using share thread
        mTilingAdaptor=new TilingAdaptor(mMediaSource,mVideoTrack,mPredictor);

        // init Codec, using unique thread
        mCodecManager=new CodecManager(mEGLCtx,mMediaSource,mVideoTrack,mPredictor);

        // init ViewPort

        // init Shader, using unique thread
        mCoreLooper =new CoreLooper(mViewPort,mVideoTrack,mPredictor,mTilingAdaptor,mCodecManager,mMediaSource);
    }

    public void start(){
        //ui control
        mCodecManager.start();
        mMediaSource.start();
        mCoreLooper.start();
    }

    /**
     * response require faster, so no check
     * control the pace
     */
    long mStartMs;
    long mNeedT = Config.RESPONSE_INTERVAL;
//    long mNeedT = 100;
    long mEndMs;
    boolean isNeedSwap=true;
    public boolean loop(){
        mStartMs = System.currentTimeMillis();
        isNeedSwap= mCoreLooper.loop();
        mEndMs = System.currentTimeMillis();
        long usedTime = mEndMs - mStartMs;
        if(DebugCfg.DEBUG_LOOPER){
            Log.d(Config.TAG_LOOPER,"loop mStartMs "+mStartMs+ " mEndMs "+mEndMs+ " sleepT "+(mNeedT - usedTime));
        }
        if (usedTime < mNeedT) {
            try {
                Thread.sleep(mNeedT - usedTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return  isNeedSwap;
    }

    public void stop() {
        //ui control
        //render stop consuming buffer

    }


    public void replay(){
        //ui control

    }

    public void release(){
//        //回收所有线程
//        try {
//            mSocketClient.release();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        mShareHandlerThread.quit();
//        mCodecManager.release();
    }


}
