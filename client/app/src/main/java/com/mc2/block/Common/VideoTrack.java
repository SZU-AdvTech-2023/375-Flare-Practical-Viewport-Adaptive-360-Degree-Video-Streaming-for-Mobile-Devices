package com.mc2.block.Common;

import static java.lang.Thread.sleep;

import android.util.Log;

import com.mc2.Utils.CheckUtils;
import com.mc2.Utils.Chunk;
import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;

import java.util.concurrent.ConcurrentLinkedQueue;

public class VideoTrack {

    private ConcurrentLinkedQueue<Chunk> mBuffer;
    public int id=-1;
    private int lastPlayedId=-1;
    private MediaSource mMediaSource;
    public Boolean preStatus=true;
    public int frameIdx=0;
    public int readViewportId=0;

    public VideoTrack(MediaSource mediaSource){
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_VIDEO_TRACK){
            CheckUtils.checkObjIsNull(mediaSource);
        }
        // init buffer
        mMediaSource=mediaSource;
        mBuffer=new ConcurrentLinkedQueue<Chunk>();
    }

    /**
     * For callback to add chunk
     * @param chunk
     */
    public void addChunkToBuffer(Chunk chunk){
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_VIDEO_TRACK){
            CheckUtils.checkObjIsNull(chunk);
        }
        if(chunk.id<=lastPlayedId){
            Log.e(Config.TAG_VIDEO_TRACK,"add illegal chunk "+lastPlayedId);
        }else {
//            Log.e(Config.TAG_VIDEO_TRACK,"add chunk "+chunk.id);
            if(!mBuffer.offer(chunk)){
                Log.e(Config.TAG_VIDEO_TRACK,"add chunk error");
            }
        }
        //TODO bug 如果分配
        if(mBuffer.size()>=Config.PRE_ALLOCATION_TIME){ //一旦有一次buffer缓存数量等于预下载量就认为预下载结束
            preStatus=false;
            if(DebugCfg.DEBUG_VIDEO_TRACK){
                Log.d(Config.TAG_VIDEO_TRACK,"end preparing...");
            }
        }

    }

    /**
     * return the next chunk of buffer for display
     *
     * @return
     */
    public Chunk displayNextChunk(){
        if(DebugCfg.CHECK_GLOBAL && DebugCfg.DEBUG_VIDEO_TRACK){
            CheckUtils.checkObjIsNull(mBuffer);
        }
        Chunk chunk=null;
        if(!preStatus){
            chunk= mBuffer.poll();
            int sleepInterval=0;
            while (chunk==null){     //阻塞循环
                chunk= mBuffer.poll();
                try {
                    sleep(sleepInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sleepInterval+=2;   //线性增长
//                Log.d(Config.TAG_VIDEO_TRACK,"displayNextChunk preparing ...");
            }
        }else {
            if(DebugCfg.DEBUG_VIDEO_TRACK){
                Log.d(Config.TAG_VIDEO_TRACK,"preparing ...");
            }
        }
        if(chunk!=null){
            lastPlayedId=id;
            id++;
        }
        return chunk;
    }



    public void release(){

    }
}
