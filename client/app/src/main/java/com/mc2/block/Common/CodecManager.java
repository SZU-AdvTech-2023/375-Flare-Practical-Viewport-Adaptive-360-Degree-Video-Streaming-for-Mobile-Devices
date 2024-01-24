package com.mc2.block.Common;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.mc2.Utils.CheckUtils;
import com.mc2.Utils.Chunk;
import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;
import com.mc2.Utils.SyncQueue;
import com.mc2.Utils.Tile;
import com.mc2.block.Decoder.CodecThread;
import com.mc2.block.Decoder.Decoder;
import com.mc2.block.Shader.EGLUtils;

import java.util.LinkedList;



public class CodecManager {

    private EGLUtils mEglUtils;
    private VideoTrack mVideoTrack;

    private Handler[] mCodecHandlers;
    private CodecThread[] mCodecThreads;   //解码线程
    private Decoder[] mDecoders;

    public SyncQueue<Tile> mMQueue;

    //当前解码chunk
    public Chunk mCurChunk=null;
    public boolean needToSwitch=true;
    public LinkedList<Chunk> mCQueue;


    public CodecManager(EGLUtils eglUtils, MediaSource mediaSource, VideoTrack videoTrack, Predictor predictor){
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_CODEC){
            CheckUtils.checkObjIsNull(eglUtils);
            CheckUtils.checkObjIsNull(mediaSource);
            CheckUtils.checkObjIsNull(videoTrack);
            CheckUtils.checkObjIsNull(predictor);
        }

        MediaSource.CallBack callBack=new MediaSource.CallBack(){
            @Override
            public void addChunkToCodec(Chunk chunk) {
                if(DebugCfg.CHECK_INPUT){
                    CheckUtils.checkObjIsNull(chunk);
                }
                try {
                    insertChunk(chunk);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        mediaSource.setCallBack(callBack);

        mEglUtils=eglUtils;
        mVideoTrack=videoTrack;

        mMQueue = new SyncQueue<Tile>();
        mCodecThreads=new CodecThread[Config.T_DECODER_SUM];
        mCodecHandlers=new Handler[Config.T_DECODER_SUM];
        mDecoders=new Decoder[Config.T_DECODER_SUM];

        for(int i = 0; i<Config.T_DECODER_SUM; i++){
            mCodecThreads[i]=new CodecThread("CodecThread"+i,mEglUtils,Config.SURFACE_TEXTURE_WIDTH,Config.SURFACE_TEXTURE_HEIGHT);
            mCodecThreads[i].start();
            mCodecHandlers[i]=new Handler(mCodecThreads[i].getLooper());
            mDecoders[i]=new Decoder(this,mCodecHandlers[i],mCodecThreads[i].mCodecShader,mCodecThreads[i].mSurfaceTexture,
                    mMQueue,mVideoTrack,predictor);
        }
        mCQueue =new LinkedList<Chunk>();
    }


    /**
     * according to status to decide whether need to switch
     */
    public synchronized void maybeSwitch(Chunk chunk) throws InterruptedException {
        if(mCurChunk==chunk){
            mCurChunk=null;
            mMQueue.clear();
//            mNQueue.clear();
            mVideoTrack.addChunkToBuffer(chunk);
//            Log.e(Config.TAG_DECODER,"add chunk "+chunk.id+" to view");

            //next chunk
            mCurChunk=mCQueue.poll();
            if(mCurChunk!=null){
                decodeDecision(mCurChunk);
                needToSwitch=false;
            }else {
                needToSwitch=true;
            }
        }
    }


    /**
     *  add chunk to list
     */
    public synchronized void insertChunk(Chunk chunk) throws InterruptedException {
        mCQueue.offer(chunk);
        if(needToSwitch){
            mCurChunk=mCQueue.poll();
            decodeDecision(mCurChunk);
            needToSwitch=false;
        }
    }

    public synchronized void decodeDecision(Chunk chunk) throws InterruptedException {
        if(mVideoTrack.preStatus){
            chunk.isPre=true;
            chunk.decodedState.reset(chunk.microList.length);
            for(int i=0;i<chunk.microList.length;i++){
                mMQueue.offer(chunk.microList[i]);
            }
        }else {
            chunk.decodedState.reset(chunk.microList.length);
            for(int i=0;i<chunk.microList.length;i++){
                mMQueue.offer(chunk.microList[i]);
            }
        }

        chunk.dcDecisionT =System.currentTimeMillis();
    }

    /**
     * 启动解码器
     */
    public void start(){
        if(DebugCfg.CHECK_GLOBAL && DebugCfg.DEBUG_CODEC){
            CheckUtils.checkObjIsNull(mDecoders);
        }
        for(int i = 0; i<Config.T_DECODER_SUM; i++) {
            mDecoders[i].setup();
        }
    }


}
