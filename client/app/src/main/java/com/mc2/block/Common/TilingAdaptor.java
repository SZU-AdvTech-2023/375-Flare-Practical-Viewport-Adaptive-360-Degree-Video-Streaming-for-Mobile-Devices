package com.mc2.block.Common;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.mc2.Utils.CheckUtils;
import com.mc2.Utils.Chunk;
import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;
import com.mc2.Utils.PathCfg;
import com.mc2.Utils.Tile;
import com.mc2.Utils.TypeUtils;
import java.util.List;

public class TilingAdaptor {

    private MediaSource mMediaSource;
    private VideoTrack mVideoTrack;
    private Predictor mPredictor;
    private Handler mHandler;
    private HandlerThread mHandlerThread;

    /**
     * Tiling scheme param
     */
    private int mTRowNum;
    private int mTColNum;
    private int mTSIdx;


    public TilingAdaptor(MediaSource mediaSource,VideoTrack videoTrack,Predictor predictor){

        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_TILING_ADAPTOR){
            CheckUtils.checkObjIsNull(mediaSource);
            CheckUtils.checkObjIsNull(videoTrack);
            CheckUtils.checkObjIsNull(predictor);
        }
        mHandlerThread=new HandlerThread("TilingAdaptorThread");
        mHandlerThread.start();
        mHandler= new Handler(mHandlerThread.getLooper());
        mMediaSource=mediaSource;
        mVideoTrack=videoTrack;
        mPredictor=predictor;
        initTileScheme();

    }

    /**
     * 【包含参数】
     *
     * 1. 不可随意调用，且其余函数仅能读取不可写入变量！
     * 2. 对于这个组件的tiling方案只能在这组函数中切换，其余切换会导致未知错误，
     * 3. 并且组件内tiling方案仅用于决策缓存，其余组件不可读取
     * 4. 需保证关于tiling的函数都由一个线程串行执行才不会出错
     */
    private void initTileScheme(){
        mTRowNum=Config.TILE_ROW_NUM_SET[0];
        mTColNum=Config.TILE_COLUMN_NUM_SET[0];
        mTSIdx=0;
    }
    public void higherTileScheme(){
        mTRowNum=Config.TILE_ROW_NUM_SET[0];
        mTColNum=Config.TILE_COLUMN_NUM_SET[0];
        mTSIdx=0;
    }
    public void lowerTileScheme(){
        mTRowNum=Config.TILE_ROW_NUM_SET[0];
        mTColNum=Config.TILE_COLUMN_NUM_SET[0];
        mTSIdx=0;
    }


    public void preAdaption() {
        if (DebugCfg.CHECK_GLOBAL && DebugCfg.DEBUG_TILING_ADAPTOR) {
            CheckUtils.checkObjIsNull(mPredictor);
            CheckUtils.checkObjIsNull(mVideoTrack);
        }
        for(int t=0;t<Config.PRE_ALLOCATION_TIME;t++){
            fixVAllocation(t,t%Config.VIDEO_LENGTH);
        }
//        fixAdaption(0);
    }

    /**
     *  adapt chunk t and insert to queue
     * @param pst
     */
    public void fixVAllocation(int id,int pst) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (DebugCfg.CHECK_GLOBAL && DebugCfg.DEBUG_TILING_ADAPTOR) {
                    CheckUtils.checkObjIsNull(mPredictor);
                    CheckUtils.checkObjIsNull(mVideoTrack);
                }

                //micro list
                float Q=0;
                int[] microLayer = mPredictor.getInitSet();
                int[] miResIdxSet = new int[microLayer.length];
                for (int i=0;i<microLayer.length;i++) {
                    miResIdxSet[i] =Config.FIX_ALLOCATION_RATE;
                    Q+=Config.BITRATE_SET[miResIdxSet[i]];
                }
                Tile[] microTiles= createMiTileSet(pst,microLayer,miResIdxSet);

                //TODO back layer mechanism
                Chunk chunk=new Chunk(id,pst,mTRowNum,mTColNum,mTSIdx,microTiles);
                if(DebugCfg.DEBUG_TILING_ADAPTOR){
                    Log.e(Config.TAG_TILING_ADAPTOR,"fixVAllocation chunk"+chunk.id+" Q "+Q);
                }
                mMediaSource.prepareChunk(chunk);
//                MacroSplit.test();
            }
        });

    }

    /**
     * 通过预测获取对应macro area码率决策
     * @param curChk
     */
    public void viewportAllocation(Chunk curChk){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (DebugCfg.CHECK_GLOBAL && DebugCfg.DEBUG_TILING_ADAPTOR) {
                    CheckUtils.checkObjIsNull(mPredictor);
                    CheckUtils.checkObjIsNull(mVideoTrack);
                }
                int chunkNum=Config.PREDICTION_CHUNK_NUM-Config.ALLOCATE_DURATION; //最终要决策的chunk数量
                for(int vId=0;vId<chunkNum;vId++){

                    int pst = curChk.pst+Config.ALLOCATE_DURATION+ vId  + 1;
                    int idTmp = curChk.id +Config.ALLOCATE_DURATION + vId  + 1;

                    if(Config.CIRCULATION_MOD){
                        pst=(pst)%Config.VIDEO_LENGTH;
                    }else if(pst>Config.VIDEO_LENGTH-1) {
                        break;
                    }

                    List<Integer> vpTiles =mPredictor.getPredictedList(vId+Config.ALLOCATE_DURATION,mTRowNum,mTColNum);
                    int totalTNum=mTRowNum*mTColNum;
                    int[] microLayer=new int[totalTNum];
                    int[] miResIdxSet = new int[totalTNum];
                    for(int i=0;i<totalTNum;i++){
                        microLayer[i]=i;
                        miResIdxSet[i]=1;
                    }

                    //TODO rate adaption
                    float Q=0;
                    for (int i=0;i<vpTiles.size();i++) {
                        miResIdxSet[vpTiles.get(i)]=2;
//                        miResIdxSet[i] =2;  //todo new
                        Q+=0.5*Config.BITRATE_SET[miResIdxSet[i]];
                    }
//                    int[] microLayer= TypeUtils.convertList(vpTiles);
                    Tile[] microTiles= createMiTileSet(pst,microLayer,miResIdxSet);
                    Chunk chunk=new Chunk(idTmp,pst,mTRowNum,mTColNum,mTSIdx,microTiles);

                    if(DebugCfg.DEBUG_TILING_ADAPTOR){
                        Log.d(Config.TAG_TILING_ADAPTOR, "viewportAllocation prepare chunk "+chunk.id);
                    }
                    mMediaSource.prepareChunk(chunk);
                }
            }
        });
    }

    /**
     * Rate adaptation
     *
     */
    private int bandwidthIdx=0;
    public void rateAdaptation(Chunk chunk){
        mHandler.post(new Runnable() {
            @Override
            public void run(){
                //TODO
            }
        });
    }


    public static float stdDev(List<Float> bList){
        // 计算数组的平均值
        float sum = 0.0f;
        for (float num : bList) {
            sum += num;
        }
        int length = bList.size();
        float mean = sum / length;

        // 计算数组的标准差
        float standardDeviation = 0.0f;
        for (float num : bList) {
            standardDeviation += Math.pow(num - mean, 2);
        }
        standardDeviation = (float) Math.sqrt(standardDeviation / length);
        return  standardDeviation;
    }



    /**`
     * Create a tile set from combined tileIndexSet and resolutionSet
     * @param pst
     * @param tileIndexSet
     * @param resIdxSet
     * @return
     */
    private Tile[] createMiTileSet(int pst, int[] tileIndexSet, int[] resIdxSet){
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_TILING_ADAPTOR){
            CheckUtils.checkObjIsNull(tileIndexSet);
            CheckUtils.checkObjIsNull(resIdxSet);
        }
        if(tileIndexSet.length != resIdxSet.length){
            Log.e(Config.TAG_TILING_ADAPTOR,"createFrontTileSet input IllegalArgument");
            return null;
        }

        Tile[] tiles=new Tile[tileIndexSet.length];
        for(int i=0;i<tileIndexSet.length;i++){
            int resolution=Config.RESOLUTION_SET[resIdxSet[i]];

            // for decoding
            String vPath=PathCfg.getPathApp()+"/video/"+resolution+"p_s"+pst+"_d"+Config.CHUNK_DURATION
                    +"_x"+mTRowNum+"_y"+mTColNum+"_t"+tileIndexSet[i]+".mp4";
            float[] pos=convertTileIdx2Position(tileIndexSet[i]);
            if(pos==null){
                Log.e(Config.TAG_TILING_ADAPTOR,"createFrontTileSet get null pos");
                return null;
            }
            int width=resolution*Config.WIDTH_HEIGHT_RATIO/mTColNum;
            int height=resolution/mTRowNum;

            tiles[i]=new Tile(vPath,pos,tileIndexSet[i],resolution,width,height,mTRowNum,mTColNum);
        }
        return tiles;
    }


    /**
     * 从原点在左上角的坐标系转换成笛卡尔坐标系
     * 返回顶点坐标系
     * 坐标原点在正方形中央，笛卡尔坐标系
     * 从左上角顺时针依次记录四个坐标
     * @param tileIdx
     * @return
     */
    private float[] convertTileIdx2Position(int tileIdx){
        float rowStep=2/(float)mTRowNum;
        float columnStep=2/(float)mTColNum;
        float[] vexData=new float[8];
        int rowIdx=tileIdx/mTColNum;
        int columnIdx=tileIdx%mTColNum;
        vexData[0]=(columnIdx*columnStep)-1f;
        vexData[1]=-(rowIdx*rowStep)+1f;
        vexData[2]=(columnIdx*columnStep+columnStep)-1f;
        vexData[3]=-(rowIdx*rowStep)+1f;
        vexData[4]=columnIdx*columnStep-1f;
        vexData[5]=-(rowIdx*rowStep+rowStep)+1f;
        vexData[6]=(columnIdx*columnStep+columnStep)-1f;
        vexData[7]=-(rowIdx*rowStep+rowStep)+1f;
        return  vexData;
    }



}

