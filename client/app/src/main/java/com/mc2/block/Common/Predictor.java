package com.mc2.block.Common;

import android.util.Log;
import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;
import com.mc2.block.Render.ViewPort;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Predictor {

    private int n; // 数据点的数量
    private float sumX; // 时间的总和
    private float sumY; // 角度值的总和

    private float meanX; // x均值
    private float meanY; // y均值
    private float cov;  // 协方差
    private float var;  // 方差

    /**
     * 为了复用斜率和截距，不抽象为通用的斜率和截距
     */
    private float slopeX ; //斜率
    private float interceptX ; //截距
    private float slopeY ; //斜率
    private float interceptY ; //截距


    public int pointCount=0;
    private LinkedList<Float> mXAngleRec;
    public float[] mXPreRec;

    private LinkedList<Float> mYAngleRec;
    public float[] mYPreRec;

    private ViewPort mViewport;
    public int predictId=0;





    public Predictor(ViewPort viewPort){
        mXAngleRec=new LinkedList<>();
        mXPreRec=new float[Config.PREDICTION_POINT];
        mYAngleRec=new LinkedList<>();
        mYPreRec=new float[Config.PREDICTION_POINT];
        mViewport=viewPort;
//        Iterator it = myList.iterator (); while (it.hasNext ()) { System.out.println (it.next ()); }
    }

    /**
     * add sample tileIdx to the sample list
     * @param xAg
     * @param yAg
     * @return
     */
    public void addSample(float xAg,float yAg){
        if(pointCount >=Config.HISTORY_WINDOW) {
            mXAngleRec.poll();
            mYAngleRec.poll();
        } else {
            pointCount++;
        }
        mXAngleRec.offer(xAg);
        mYAngleRec.offer(yAg);

        // 每次添加新的样本后，更新 pointCount 的值
        pointCount = mXAngleRec.size();

        if(DebugCfg.DEBUG_PREDICTOR){
            Log.e(Config.TAG_PREDICTOR,"addX "+xAg+" addY "+yAg);
        }
    }

    //TODO remove and  add num of predict point
    public void predict(){
        predictId++;    // 记录唯一的预测次数
        if(pointCount !=Config.HISTORY_WINDOW){
            Log.e(Config.TAG_PREDICTOR,"predict is lacking data!!! pointCount "+pointCount);
        }
        float t;
        init();
        t=0;
        for(int i=0;i<Config.HISTORY_WINDOW;i++){
            t=(i+1)*Config.SAMPLING_PERIOD;
            addDataPoint(t,mXAngleRec.get(i));
        }
        meanX = sumX/n;
        meanY = sumY/n;
        for(int i=0;i<Config.HISTORY_WINDOW;i++){
            t=(i+1)*Config.SAMPLING_PERIOD;
            calcCovVar(t, mXAngleRec.get(i));
        }
        calSlopeInterceptX();
        for(int i=0;i<Config.PREDICTION_POINT;i++){
            t = Config.PREDICTION_DURATION+(Config.HISTORY_WINDOW + i + 1) * Config.SAMPLING_PERIOD;
            mXPreRec[i]= (float) predictPointX(t);
            if(DebugCfg.DEBUG_PREDICTOR){
                Log.d(Config.TAG_PREDICTOR,"preX "+mXPreRec[i]);
            }
        }

        init();
        t=0;
        for(int i=0;i<Config.HISTORY_WINDOW;i++){
            t=(i+1)*Config.SAMPLING_PERIOD;
            addDataPoint(t,mYAngleRec.get(i));
        }
        meanX = sumX/n;
        meanY = sumY/n;
        for(int i=0;i<Config.HISTORY_WINDOW;i++){
            t=(i+1)*Config.SAMPLING_PERIOD;
            calcCovVar(t, mYAngleRec.get(i));
        }
        calSlopeInterceptY();
        for(int i=0;i<Config.PREDICTION_POINT;i++){
            t = Config.PREDICTION_DURATION+(Config.HISTORY_WINDOW + i + 1) * Config.SAMPLING_PERIOD;
//            t=Config.PREDICTION_DURATION+(i+1)*Config.SAMPLING_PERIOD;
            mYPreRec[i]= (float) predictPointY(t);
            if(DebugCfg.DEBUG_PREDICTOR){
                Log.d(Config.TAG_PREDICTOR,"preY "+mYPreRec[i]);
            }
        }
    }


    private void init() {
        n = 0;
        sumX = 0f;
        sumY = 0f;
        cov = 0f;
        var = 0f;
        meanX = 0f;
        meanY = 0f;
    }

    // 添加一个数据点
    private void addDataPoint(double x, double y) {
        n++;
        sumX += x;
        sumY += y;
    }

    private void calcCovVar(double x, double y) {
        cov += (x - meanX) * (y - meanY);
        var += (x - meanX) * (x - meanX);
    }

    // 计算回归方程的斜率 and intercept
    private void calSlopeInterceptX() {
        cov /= n;
        var /= n;
        slopeX = cov/var;
        interceptX= meanY - slopeX * meanX;
    }
    private void calSlopeInterceptY() {
        cov /= n;
        var /= n;
        slopeY = cov/var;
        interceptY= meanY - slopeY * meanX;
    }


    /**
     * 根据时间进行预测，k的定义是从0开始到目标时刻的时间段长度，单位是ms
     * @param k
     * @return
     */
    private float predictPointX(float k) {
        return slopeX * k + interceptX;
    }
    private float predictPointY(float k) {
        return slopeY * k + interceptY;
    }



    //TODO transform the predicted coordinates to chunk indexes
    public int[] getInitSet(){

//        return new int[]{0,1,2,3,4,5};
//        return new int[]{0,1,2,3,4,5,6,7,8,9,10};
//        return new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
//        return new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};

        return new int[]{0,1,2,3,4,5,6,7,8};
//        return new int[]{13,16,19,22,8,9,26,27};
//        return new int[]{13,14,15,16};
//        return new int[]{};
    }


    /**
     * 获取预测结果对应macro area的tile list
     * @param chunkIdx
     * @return
     */
    public List<Integer> getPredictedList(int chunkIdx,int tRowNum,int tColNum){

        Map<Integer, Boolean> tileMap=new HashMap<>();
        for (int i=0;i<Config.SAMPLING_FREQUENCY;i++){
            mViewport.calculateUserViewTile(
                    tRowNum,tColNum,
                    mXPreRec[Config.SAMPLING_FREQUENCY*chunkIdx+i],
                    mYPreRec[Config.SAMPLING_FREQUENCY*chunkIdx+i],
                    tileMap
            );
        }
        Set<Integer> mapKeySet=tileMap.keySet();

        List<Integer> tileSet=new LinkedList();
        for (Integer t : mapKeySet) {
            tileSet.add(t);
        }
        return tileSet;
    }





}


