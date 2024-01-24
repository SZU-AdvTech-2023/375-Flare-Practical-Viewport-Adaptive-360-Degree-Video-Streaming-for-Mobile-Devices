package com.mc2.block.Decoder;


import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.mc2.Utils.CheckUtils;
import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;
import com.mc2.Utils.SyncQueue;
import com.mc2.Utils.Tile;
import com.mc2.block.Common.CodecManager;
import com.mc2.block.Common.Predictor;
import com.mc2.block.Common.VideoTrack;
import com.mc2.block.Shader.CodecShader;
import com.mc2.block.Shader.ShaderUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Decoder {
    private CodecManager mCodecManager;
    private Handler mHandler=null;
    private CodecShader mCodecShader;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private MediaFormat mMediaFormat;
    private SyncQueue mUnDecodedQue;
    private MediaCodec.Callback mCallback;
    private Predictor mPredictor;

    //self
    HandlerThread mSufThread;
    Handler mSurHandler;

    // MediaFormat
    private MediaExtractor mMediaExtractor;
    private long mCurSampleTime;
    private int mCurSampleFlags;
    private MediaCodec mMediaCodec =null;
    private int mReadInputSize=1;     //抽取buffer的


    // status
    public Tile mFetchTile;
    public int frameIndex=0;
    private boolean mIsHardCodec =false;
    private VideoTrack mVideoTrack;


    // sync
    private Object mFrameSyncObject = new Object();     // guards mFrameAvailable
    private boolean mFrameAvailable=false;    // info is there new frame
    private boolean mIsEndOfStream=false;

    // record
    private long t_lastExtract;

    /**
     * Create a Texture Decoder
     * @param codecHandler
     * @param codecShader
     * @param surfaceTexture
     * @param unDecodedQue
     */
    public Decoder(CodecManager codecManager, Handler codecHandler, CodecShader codecShader,
                   SurfaceTexture surfaceTexture, SyncQueue unDecodedQue, VideoTrack videoTrack, Predictor predictor) {
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_CODEC){
            CheckUtils.checkObjIsNull(codecHandler);
            CheckUtils.checkObjIsNull(codecShader);
            CheckUtils.checkObjIsNull(surfaceTexture);
            CheckUtils.checkObjIsNull(unDecodedQue);
        }

        //init global
        mCodecManager=codecManager;
        mHandler=codecHandler;
        mCodecShader=codecShader;
        mSurfaceTexture=surfaceTexture;
        mSurface=new Surface(mSurfaceTexture);
        mUnDecodedQue=unDecodedQue;
        mVideoTrack=videoTrack;
        mPredictor=predictor;

        //unique thread for surface listening
        mSufThread=new HandlerThread("SurfaceTexture");
        mSufThread.start();
        mSurHandler=new Handler(mSufThread.getLooper());

        CodecOnFrameAvailable codecOnFrameAvailable=new CodecOnFrameAvailable();
        mSurfaceTexture.setOnFrameAvailableListener(codecOnFrameAvailable,mSurHandler);

        mCallback=new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {

                ByteBuffer inputBuffer= mMediaCodec.getInputBuffer(index);
                mReadInputSize = readBuffer(inputBuffer);
                if (mReadInputSize >= 0) {
                    codec.queueInputBuffer(index, 0, mReadInputSize, mCurSampleTime, mCurSampleFlags);
                }else{
                    codec.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    );
                }
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
//                if(info.flags!=MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                if(!mIsEndOfStream) {
                    codec.releaseOutputBuffer(index, true);
                    awaitNewImage();
                    try {
                        drawFrame();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else {
                    boolean isTheSame=deQueToExtractor();
//                    Log.e(Config.TAG_DECODER,"isTheSame "+isTheSame+" mFetchTile.height "+mFetchTile.height+" mIsHardCodec "+mIsHardCodec);
                    if(!Config.ENABLE_HARDWARE_CODEC ||
                            ((mFetchTile.height>=Config.HARDWARE_MIN_REQUIREMENT
                                    && mFetchTile.width>=Config.HARDWARE_MIN_REQUIREMENT)
                                    && mIsHardCodec) ||
                            ((mFetchTile.height<Config.HARDWARE_MIN_REQUIREMENT
                                    || mFetchTile.width<Config.HARDWARE_MIN_REQUIREMENT)
                                    && !mIsHardCodec)){
                        if(isTheSame){
                            // do not need to switch
                            mMediaCodec.flush();
                        }else{
                            // need to re config
                            mMediaCodec.stop();
                            mMediaCodec.setCallback(mCallback,mHandler);
                            mMediaCodec.configure(mMediaFormat,mSurface,null,0);
                        }

                    }else{
                        // need to release and re create
                        mMediaCodec.release();
//                        mMediaCodec.getName();
                        if(mIsHardCodec){
                            try {
                                mMediaCodec=MediaCodec.createByCodecName("OMX.qcom.video.decoder.avc");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            mIsHardCodec=false;
                        }else{
                            try {
                                mMediaCodec=MediaCodec.createByCodecName("OMX.google.h264.decoder");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            mIsHardCodec=true;
                        }
                        mMediaCodec.setCallback(mCallback,mHandler);
                        mMediaCodec.configure(mMediaFormat,mSurface,null,0);
                        if(DebugCfg.DEBUG_CODEC){
                            Log.d(Config.TAG_DECODER,Thread.currentThread().getName()+" reConfig mIsHardCodec "+mIsHardCodec);
                        }
                    }
                    mMediaCodec.start();
                    mIsEndOfStream=false;
                }
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                Log.e(Config.TAG_DECODER,"onError "+ e);
                codec.stop();
            }
            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                Log.d(Config.TAG_DECODER,"onOutputFormatChanged "+format.toString());

            }
        };


    }

    //自定义监听器
    private class CodecOnFrameAvailable implements SurfaceTexture.OnFrameAvailableListener {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//            surfaceTexture.updateTexImage();
//            drawFrame();
            synchronized (mFrameSyncObject) {
                if (mFrameAvailable) {
                    throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
                }
                mFrameAvailable = true;
                mFrameSyncObject.notifyAll();
            }

        }
    }

    /**
     * setup mediacodec and listen to the unDecoded queue
     */
    public void setup(){    //在自己的线程里阻塞
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                if(mMediaCodec ==null){
                    try {
//                        if(Config.ENABLE_HARDWARE_CODEC){
//                            mMediaCodec = MediaCodec.createByCodecName("OMX.qcom.video.decoder.vp9");
//                            mIsHardCodec =true;
//                        }else {
//                            mMediaCodec = MediaCodec.createByCodecName("OMX.google.vp9.decoder");
//                            mIsHardCodec=false;
//                        }
//                        mMediaCodec = MediaCodec.createByCodecName("OMX.google.vp9.decoder");
                        mIsHardCodec=false;
//                        mMediaCodec = MediaCodec.createByCodecName("OMX.qcom.video.decoder.avc");
                        mMediaCodec = MediaCodec.createByCodecName("OMX.google.h264.decoder");
                        //OMX.qcom.video.decoder.avc 是硬件解码器，其余的OMX.google c2.android 是软件解码器

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                deQueToExtractor();     // stuck here and wait for input
//                getCodecListForMime(mMediaFormat.getString(MediaFormat.KEY_MIME));
                mMediaCodec.setCallback(mCallback,mHandler);
                mMediaCodec.configure(mMediaFormat,mSurface,null,0);
                mMediaCodec.start();
            }
        });
 
    }


    /**
     * get stuck until fetch tile from the queue
     */
    private boolean deQueToExtractor(){
        boolean isFormatTheSame=false;
        if(DebugCfg.DEBUG_CODEC){
            Log.e(Config.TAG_DECODER,Thread.currentThread().getName()+" deQueToExtractor start request ...");
        }
        int sleepInterval=0;
        Tile tile=null;
        while (tile==null){     //阻塞循环
            tile=(Tile) mUnDecodedQue.poll();
            try {
                sleep(sleepInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sleepInterval+=2;   //线性增长
        }

        if(tile.videoPath==null){
            Log.e(Config.TAG_DECODER,"mInputCallback getVideoPath null");
        }
        mFetchTile =tile;
        mFetchTile.isToDc=true;   //送去解码了
        if(mMediaFormat!=null
                &&mMediaFormat.getInteger("width")==mFetchTile.width
                &&mMediaFormat.getInteger("height")==mFetchTile.height){ //format change,first time escape
            isFormatTheSame=true;
        }

        initExtractor(mFetchTile.videoPath);
        //create tex, warning here is not checking
        mFetchTile.width=mMediaFormat.getInteger("width");
        mFetchTile.height=mMediaFormat.getInteger("height");
        mFetchTile.tex= ShaderUtils.createTileTexturesSet(mMediaFormat.getInteger("width"),mMediaFormat.getInteger("height"));
        t_lastExtract =System.currentTimeMillis();

        if(DebugCfg.DEBUG_CODEC){
            Log.e(Config.TAG_DECODER,"create tex width height "+tile.width+" "+tile.height);
            Log.e(Config.TAG_DECODER,Thread.currentThread().getName()+" deQueToExtractor end request ...");
        }

        return isFormatTheSame;
    }

    /**
     * Call by draw and wont check
     */
    private void updateDecodingState(){
        mFetchTile.parent.decodedState.updateReadyCount();
    }


    private void awaitNewImage() {
        final int TIMEOUT_MS = 50000;
        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    mFrameSyncObject.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                        throw new RuntimeException("frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }

            }
            mFrameAvailable = false;
        }
        // Latch the data.
        mSurfaceTexture.updateTexImage();
    }


    /**
     * Draw a frame and not completely check, no safe
     */
    private void drawFrame() throws InterruptedException {
        if(Config.ENABLE_DRAW){
            mCodecShader.draw(mFetchTile.width,mFetchTile.height,mFetchTile.tex[frameIndex]);
        }
        frameIndex+=1;
        if(frameIndex>=Config.FRAME_NUM){  //the end of the every tile
            frameIndex=0;
            long nowt=System.currentTimeMillis();
            mFetchTile.decodingT=nowt-t_lastExtract;  //解码时间
            mFetchTile.parent.addDecodedList(mFetchTile);
            updateDecodingState();  //更新计数器
            mIsEndOfStream=true;
            boolean overTime=false;

            if(Config.ENABLE_DC_ABORTION){
                if(!mVideoTrack.preStatus){ //不是准备阶段
                    overTime=(nowt-mFetchTile.parent.dcDecisionT)>=Config.DC_ABORTION_TIME;
                    if(overTime){   //超时则抛弃队列
                        if(mFetchTile.parent.decodedState.unReadyCount>0){
                            Log.d(Config.TAG_DECODER,"TDecoder "+currentThread().getId()+" chunk "+mFetchTile.parent.id
                                    +" discard dc number "+mFetchTile.parent.decodedState.unReadyCount);
                        }
                        mFetchTile.parent.decodedState.reset(0);
                    }
                }
            }

            if((!mVideoTrack.preStatus&&overTime) ||
                    mFetchTile.parent.isAllReady()){ //解码超分都完成了才会切换
                mCodecManager.maybeSwitch(mFetchTile.parent);
                float totalCost=nowt- mFetchTile.parent.dcDecisionT;
                mFetchTile.parent.dcT=totalCost;
                Log.i(Config.TAG_DECODER, currentThread().getName()+" chunk "+mFetchTile.parent.id+" total consume "+(totalCost));
            }

        }
        if(DebugCfg.DEBUG_CODEC){
            if(frameIndex==1){  //first draw done
                Log.w(Config.TAG_DECODER, Thread.currentThread().getName()+" init cost "+(System.currentTimeMillis()-t_lastExtract));
            }
        }
    }


    /**
     * 从视频文件初始化媒体提取器
     *
     * @param path
     */
    private void initExtractor(String path) {
        if(DebugCfg.DEBUG_CODEC){
            Log.d(Config.TAG_DECODER, Thread.currentThread().getName() + " initExtractor path: " + path);
        }
        if (mMediaExtractor != null) {
            mMediaExtractor.release();
        }
        try {
            mMediaExtractor = new MediaExtractor();
            // 设置数据源
            mMediaExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaFormat = mMediaExtractor.getTrackFormat(0);
//        Log.d(Config.TAG_DECODER,"mMediaFmt "+ mMediaFormat.toString());
        standardiseFmt("height");
        standardiseFmt("width");
        if(DebugCfg.DEBUG_CODEC){
            Log.d(Config.TAG_DECODER, "实际解码器中的宽高 " + mMediaFormat.getInteger("width") + " " + mMediaFormat.getInteger("height"));
        }
//        mediaFormat.setInteger("frame-rate", 900);
//        ,Log.d(TAG, "解码格式 " + mediaFormat);
    }


    /**
     * the key number should be the mod of 16 because of hardware
     * android.media.MediaCodec$CodecException: Error 0xfffffc0e
     * @param key
     */
    private void standardiseFmt(String key) {
        if (mMediaFormat.containsKey(key)) {
            int keyInt = mMediaFormat.getInteger(key);
            if (keyInt % 16 != 0) {
                keyInt = (keyInt / 16 + 1) * 16;
                mMediaFormat.setInteger(key, keyInt);
                Log.w(Config.TAG_DECODER, "standardiseFmt set key " + keyInt);
            }
        } else {
            Log.w(Config.TAG_DECODER, "standardiseFmt get wrong key " + key);
        }
    }


    private int readBuffer(ByteBuffer buffer) {
        //先清空数据
        buffer.clear();
        //选择要解析的轨道
        mMediaExtractor.selectTrack(0);
        //读取当前帧的数据
        int bufferCount = mMediaExtractor.readSampleData(buffer, 0);
        if (bufferCount < 0) {
            return -1;
        }
        //记录当前时间戳
        mCurSampleTime = mMediaExtractor.getSampleTime();
        //记录当前帧的标志位
        mCurSampleFlags = mMediaExtractor.getSampleFlags();
        //进入下一帧
        mMediaExtractor.advance();
        return bufferCount;
    }

    /**
     * 获取支持的解码器列表
     * @param mime
     *
     * mime video/avc supported by OMX.qcom.video.decoder.avc
     * mime video/avc supported by OMX.qcom.video.decoder.avc
     * mime video/avc supported by OMX.qcom.video.decoder.avc
     * mime video/avc supported by c2.android.avc.decoder
     * mime video/avc supported by OMX.google.h264.decoder
     * mime video/avc supported by c2.qti.avc.decoder
     * mime video/avc supported by c2.android.avc.decoder
     * mime video/avc supported by OMX.google.h264.decoder
     * mime video/avc supported by c2.qti.avc.decoder
     * mime video/avc supported by c2.android.avc.decoder
     * mime video/avc supported by OMX.google.h264.decoder
     * mime video/avc supported by c2.qti.avc.decoder
     * @param mime
     */
    private void getCodecListForMime(String mime){
        MediaCodecList mediaCodecList=new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        MediaCodecInfo[] codecInfos=mediaCodecList.getCodecInfos();
        for(MediaCodecInfo codecInfo:codecInfos){
            if (codecInfo.isEncoder()) continue;
            String[] types=codecInfo.getSupportedTypes();
            for(String type:types){
                if(type.equals(mime)){
                    Log.e(Config.TAG_DECODER,"mime "+mime+" supported by "+codecInfo.getName());
                }
            }
        }
    }

    public void release(){
        try {
            mMediaCodec.stop();
            mMediaCodec.release();
            //释放 MediaExtractor
            mMediaExtractor.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
