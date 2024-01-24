package com.mc2.Utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Chunk {
    public int id;  //分配的唯一id
    public int pst;    //对应第几秒的视频
    public int tSIdx;
    public int tRowNum; //这个chunk所使用的tiling方案
    public int tColNum;
    public int tSum;
    public Tile[] microList;
    public SyncState decodedState;
    public SyncPredict dsState; //解码决策状态
    public HashMap<Integer,String> modelPathMap;
    public CopyOnWriteArrayList<Tile> doneTiles;

    public float bandwidthEst;
    public long downloadT;  //下载耗时
    public float bandwidthUsage; //带宽消耗
    public long dcDecisionT;    //dc决策结束时间
    public float dcT;    //总的解码时间
    public boolean isSendToDownload=false; //是否被送去下载了
    public boolean isDownloadDone=false; //下载是否完成了
    public boolean isPre=false;     //vmap是否被填充过数值
    public HashMap<Integer,Boolean> vMap;

    /**
     *
     * @param i_id
     * @param i_pst
     * @param tileRowNum
     * @param tileColNum
     * @param tileSchemeIdx
     * @param microTiles

     */
    public Chunk(int i_id, int i_pst,
                 int tileRowNum, int tileColNum, int tileSchemeIdx,
                 Tile[] microTiles){
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_MEDIA_SOURCE){
            CheckUtils.checkObjIsNull(microTiles);
        }
        id=i_id;
        pst=i_pst;
        tRowNum=tileRowNum;
        tColNum=tileColNum;
        tSum=tileRowNum*tileColNum;
        tSIdx=tileSchemeIdx;
        microList =microTiles;
        modelPathMap=new HashMap<Integer,String>();
        doneTiles =new CopyOnWriteArrayList<>();
        //set tile parent
        for(Tile tile:microTiles){
            tile.parent=this;
        }
        decodedState =new SyncState(0);
        dsState =new SyncPredict();
    }



    /**
     * add tile to list after decoding
     * @param tile
     */
    public void addDecodedList(Tile tile){
        doneTiles.add(tile);
    }


    public class SyncState{
        public int unReadyCount=0;    //未解码块计数器,其他线程只读

        public SyncState(int unReadyNum){
            unReadyCount=unReadyNum;
        }

        /**
         * 更新计数器
         */
        public synchronized boolean updateReadyCount(){
            if(unReadyCount==0){
//                Log.d(Config.TAG_MEDIA_SOURCE,"unReadyCount clear");
                return  false;
            }
            unReadyCount-=1;
//            ExpUtils.LogToFile(Thread.currentThread().getName()+" "+unReadyCount);
            return  true;
        }

        public synchronized void reset(int len){
            unReadyCount=len;
            return;
        }
        public void add(int number){
            unReadyCount+=number;
            if(unReadyCount<0){
                Log.d(Config.TAG_MEDIA_SOURCE,"unReadyCount err");
                unReadyCount=0;
            }
            return;
        }

    }

    public class SyncPredict{
        public int predictId=0;    //用于决策的id是多少,视为key

        public SyncPredict(){}
        /**
         * 是否有新的预测结果
         */
        public synchronized boolean hasNewPreRes(int curPreId){
            if(curPreId>predictId){
                predictId=curPreId; //更新解码的id
                return true;
            }
            return  false;
        }
    }


    /**
     * check the chunk decoded state
     * @return
     */
    public boolean isAllReady(){
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_MEDIA_SOURCE ){
            CheckUtils.checkObjIsNull(decodedState);
        }
        if(decodedState.unReadyCount==0){
            return true;
        }

        if(DebugCfg.DEBUG_UTILS){
            Log.d(Config.TAG_UTILS,"decodedState.unReadyCount "+ decodedState.unReadyCount);
        }
        return false;
    }

}
