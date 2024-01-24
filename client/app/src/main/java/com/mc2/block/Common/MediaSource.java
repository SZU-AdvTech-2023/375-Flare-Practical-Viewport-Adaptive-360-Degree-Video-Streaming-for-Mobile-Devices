package com.mc2.block.Common;

import static java.lang.Thread.sleep;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.mc2.Utils.CheckUtils;
import com.mc2.Utils.Chunk;
import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;
import com.mc2.Utils.PathCfg;
import com.mc2.Utils.Tile;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;


public class MediaSource {
    private CallBack mCallBack;

    private HttpClient mHttpClient;
    private Handler mHandler;
    public final byte[] lock = new byte[0];  // 队列枷锁
    public LinkedList<Chunk> unDownloadQue;

    public float bitrate=3000;

    public HashMap<Integer,Chunk> chunkMap;

    public int lastDownloadedChkId=0;

    public MediaSource( HttpClient httpClient){
        if(!Config.LOCAL_MODE){
            CheckUtils.checkObjIsNull(httpClient);

            mHttpClient=httpClient;
        }
        HandlerThread thread=new HandlerThread("shareHandler");
        thread.start();
        mHandler= new Handler(thread.getLooper());
        unDownloadQue=new LinkedList<>();
//        initTexBuffer();
        chunkMap=new HashMap<>();
    }


    /**
     * Add a chunk to the video track
     */
    public interface CallBack {
        public void addChunkToCodec(Chunk chunk);
    }

    public void setCallBack(CallBack callBack){
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_MEDIA_SOURCE){
            CheckUtils.checkObjIsNull(callBack);
        }
        mCallBack=callBack;
    }

    public void start(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                while (true){
                    if(Config.LOCAL_MODE){ //todo 离线验证文件是否存在
                        mCallBack.addChunkToCodec(fetchChunk());
                    }else {
                        try {
                            handleChunk(fetchChunk());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private Chunk fetchChunk(){
        int sleepInterval=0;
        Chunk chunk=null;
        while (chunk==null){     //阻塞循环
            synchronized (lock){
                chunk= unDownloadQue.poll();
            }
            try {
                sleep(sleepInterval);   //TODO thread stuck
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sleepInterval+=1;   //线性增长
        }
        chunk.isSendToDownload=true;
        if(DebugCfg.DEBUG_MEDIA_SOURCE){
            Log.d(Config.TAG_BLOCK,"calculateAcc "+"fetchChunk "+chunk.id+" "+chunk);
        }
        return chunk;
    }

    /**
     * return the chunk with the same Id
     * @param cId
     */
    public Chunk findTheChunk(int cId){
        synchronized (lock) {
            return chunkMap.getOrDefault(cId, null);
        }
    }

    //TODO no download
    /**
     * prepare a chunk
     * req_string: OK|chunkId,tx,ty|macroIdx1,macroIdx2,maResolutionId|tileIdx,miResolutonId,,,,,|isDownloadSR|modelResolution,,,
     * @param chunk
     */
    public void prepareChunk(Chunk chunk){
        synchronized (lock) {
            if (DebugCfg.CHECK_INPUT) {
                CheckUtils.checkObjIsNull(chunk);
            }

            if(chunkMap.containsKey(chunk.id)){ //曾经入队过这个id
//                //搜索队列中还有没有这个chunk，有就替换
                Chunk laterChk=chunkMap.get(chunk.id);
                if(laterChk.isSendToDownload!=true){    //曾今加载进入map而且还没被送去下载
                    boolean hasTheSame=false;
                    int bIdx=0;
                    for(Chunk c: unDownloadQue){
                        if(chunk.id==c.id){
                            hasTheSame=true;
                            break;
                        }
                        bIdx++;
                    }
                    if(hasTheSame){
                        unDownloadQue.set(bIdx,chunk);
                        chunkMap.put(chunk.id,chunk);   //update the chunk map
                    }else {
                        Log.e(Config.TAG_MEDIA_SOURCE,"prepareChunk 找不到chk来更新决策 chkId "+chunk.id);
                    }
                }

            }else { //曾经没有入队过这个id，直接后入
                unDownloadQue.offer(chunk);
                chunkMap.put(chunk.id,chunk);   //update the chunk map
            }
        }
    }

    private void handleChunk(Chunk chunk) throws InterruptedException {
        chunk.downloadT=System.currentTimeMillis();
        int size =mHttpClient.reqChunk(chunk);
        long timeTaken = System.currentTimeMillis()-chunk.downloadT;

        // Calculate the bandwidth in b/s
        bitrate = size / (float)(timeTaken);
        if(DebugCfg.DEBUG_MEDIA_SOURCE){
            Log.d(Config.TAG_MEDIA_SOURCE,"download chunk"+chunk.id+" actual bitrate "+ bitrate+" cost "+(timeTaken));
        }
        chunk.isDownloadDone=true;
        mCallBack.addChunkToCodec(chunk);
    }

    public int downloadedBufLen(int chkId){
        return Math.max(lastDownloadedChkId-chkId,0);
    }

    public void release(){

    }

}
