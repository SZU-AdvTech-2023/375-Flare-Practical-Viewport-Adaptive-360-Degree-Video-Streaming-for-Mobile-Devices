package com.mc2.block.Common;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.mc2.Utils.CheckUtils;
import com.mc2.Utils.Chunk;
import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;
import com.mc2.Utils.Tile;
import com.mc2.Utils.TypeUtils;
import com.mc2.block.Render.ViewPort;
import com.mc2.block.Shader.Shader;
import com.mc2.block.Shader.SphereShader;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class CoreLooper {

    private VideoTrack mVideoTrack;
    private Predictor mPredictor;
    private TilingAdaptor mTilingAdaptor;
    private MediaSource mMediaSource;

    private boolean mIsLooperEnd;
    public ViewPort mViewPort;
    public Shader mShader;
    private CodecManager mCodecManager;
    public Handler mHandler;
    private HandlerThread mHandlerThread;

    private int sortCount=0;
    private Chunk mCurChunk;
    private int mLoopStep =0; // 线程的按照一定步长进行响应，但是不等同与实际渲染视频内容的帧数

    //record
    private long lastProcessCpuT=0;
    private int displayChunkCount =0;
    private int lastAdaptationNum=1;
    private long lastEndDisplayT=0;
    private float lastQ=0;
    private HashMap<Integer, Boolean> mViewMap =new HashMap<>();

    public CoreLooper(ViewPort viewPort, VideoTrack videoTrack, Predictor predictor, TilingAdaptor tilingAdaptor, CodecManager codecManager,MediaSource mediaSource){
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_LOOPER){
            CheckUtils.checkObjIsNull(viewPort);
            CheckUtils.checkObjIsNull(videoTrack);
            CheckUtils.checkObjIsNull(predictor);
            CheckUtils.checkObjIsNull(tilingAdaptor);
            CheckUtils.checkObjIsNull(codecManager);
            CheckUtils.checkObjIsNull(mediaSource);
        }
        mViewPort =viewPort;
        mVideoTrack=videoTrack;
        mPredictor=predictor;
        mTilingAdaptor= tilingAdaptor;
        mCodecManager=codecManager;
        mMediaSource=mediaSource;
        mHandlerThread=new HandlerThread("EvaluatorThread");
        mHandlerThread.start();
        mHandler= new Handler(mHandlerThread.getLooper());
    }

    public void start(){
        mIsLooperEnd=false;
        mShader=new SphereShader(mViewPort);
        mTilingAdaptor.preAdaption();
    }

    private boolean isNeedSwap=true;

    private long lastSwitchT=0;

    public boolean loop(){
        //init share context and fbo
        /**
         * FBO can not be shared, so need to create in this thread
         */
        if (!mIsLooperEnd){
            if(mLoopStep %Config.PREDICTION_RESPONSE_INTERVAL==0){
                //TODO 检查step和帧数的关系
                if(Config.ENABLE_PREDICTION){
//                    mViewPort.yAngle=ExpUtils.yArray[mVideoTrack.readViewportId];
//                    mViewPort.xAngle=ExpUtils.xArray[mVideoTrack.readViewportId];
                    mPredictor.addSample(mViewPort.xAngle,mViewPort.yAngle);
                    if(mPredictor.pointCount==Config.HISTORY_WINDOW){
                        //Predict
                        mPredictor.predict();
                    }
                }

                //统计用互看到哪些tile
                if(mCurChunk!=null){
                    mViewPort.calculateUserViewTile(mCurChunk.tRowNum,mCurChunk.tColNum,
                            mViewPort.xAngle,mViewPort.yAngle,
                            mViewMap,mCurChunk.vMap);
                }
//                mVideoTrack.readViewportId=(mVideoTrack.readViewportId+1)%Config.READ_VIEW_PORT_NUM;
            }

            if(mCurChunk==null){
                mCurChunk=mVideoTrack.displayNextChunk();
            }

            if(mCurChunk!=null){
                isNeedSwap=true;

                //TODO Draw FBO without check
                mShader.prepareDraw();
                for(Tile tile:mCurChunk.doneTiles){
                    mShader.draw(mCurChunk.tSIdx,tile,mVideoTrack.frameIdx);
                }
                mShader.endDraw(mVideoTrack.frameIdx);


                if(DebugCfg.DEBUG_LOOPER&&DebugCfg.DEBUG_LOOPER_DRAW){
                    Log.d(Config.TAG_LOOPER,"Chunk "+mCurChunk.id + " draw frame "+mVideoTrack.frameIdx);
                }
                mVideoTrack.frameIdx++;
                if(mVideoTrack.frameIdx>=Config.FRAME_NUM){
                    mVideoTrack.frameIdx=0;
                    displayChunkCount++;

                    if(displayChunkCount >= Config.INIT_PREDICTION_TIME){ //准备好预测数据再开始分配
                        if(displayChunkCount-lastAdaptationNum>=Config.ALLOCATE_DURATION) { //距离上一次分配的时间已经到分配间隔
                            if(!Config.ENABLE_PREDICTION){ //固定分配mIsLooperEnd
                                for(int i = 1; i<=Config.FIX_ALLOCATE_TIME; i++){
                                    if(Config.CIRCULATION_MOD){ //是否是循环模式
//                                      Log.e(Config.TAG_RENDER,"mchunk pst "+mCurChunk.pst+" next chunk pst "+String.valueOf((mCurChunk.pst)%Config.VIDEO_LENGTH+1));
                                        mTilingAdaptor.fixVAllocation((mVideoTrack.id+Config.ALLOCATE_DURATION+i),(mCurChunk.pst+Config.ALLOCATE_DURATION+i)%Config.VIDEO_LENGTH);

                                    }else if(mCurChunk.pst+Config.ALLOCATE_DURATION+i<=Config.VIDEO_LENGTH-1){
                                        mTilingAdaptor.fixVAllocation((mVideoTrack.id+Config.ALLOCATE_DURATION+i),mCurChunk.pst+Config.ALLOCATE_DURATION+i);

                                    }
                                }

                            }else { //用预测分配
                                if(Config.ENABLE_RATE_ADAPTATION){
                                    mTilingAdaptor.rateAdaptation(mCurChunk);
                                }else {
//                                    mTilingAdaptor.rateAdaptation(mCurChunk.pst);
                                    mTilingAdaptor.viewportAllocation(mCurChunk);
                                }
                            }
                            lastAdaptationNum=displayChunkCount;
                        }

                    }

                    if(!mVideoTrack.preStatus){
                        //todo
//                        evaluate();
                    }
                    sortCount=0;
                    recycle();
                    mCurChunk=null;
                    System.gc();
                }
            }else {     // not ready
                isNeedSwap=false;
                //TODO draw buffering pic
            }

            // Response step control
            mLoopStep++;
            if(mLoopStep >=Config.RESPONSE_INTERVAL_SUM){
                mLoopStep =0;
            }

        }
        return  isNeedSwap;
    }

    public void release(){
        mIsLooperEnd=true;
    }

    private void recycle(){
        for(Tile t:mCurChunk.doneTiles){
            mShader.recycle(t);
        }
    }
}
