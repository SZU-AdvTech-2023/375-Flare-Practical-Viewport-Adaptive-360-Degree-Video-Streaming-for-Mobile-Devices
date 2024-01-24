package com.mc2.block.Render;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;

import java.util.Map;

public class ViewPort {

    private static final String TAG = "ViewPort";

    /** 视口采样 **/
    //视口角度，特别重要
    public float xAngle=0f;
    public float yAngle=90f;
    public float zAngle;

    private final float[] rotationMatrix = new float[16];    //旋转矩阵
    private final float bScanPoint =10; //视口内一列有多少采样的点
    private final float aScanPoint =10; //视口内一行有多少采样的点
    private float[][] viewSampleMatrix =new float[(int) (aScanPoint * bScanPoint)][4];    //旋转前的真实视口采样点矩阵
    private float[][] viewResSampleMatrix = new float[(int) (aScanPoint * bScanPoint)][4];    //旋转后的真实视口矩阵

    private float[][][] rankMatrix=new float[Config.CLASS_NUM-1][(int) (aScanPoint * bScanPoint)][4];    //k rank => k-1 numbers of table
    private float[][] rankTmpMatrix=new float [(int) (aScanPoint * bScanPoint)][4];    //k rank => k-1 numbers of table

    private float[][] macroSampleMatrix =new float[(int) (aScanPoint * bScanPoint)][4];    //旋转前的视口采样点矩阵
    private float[][] macroResSampleMatrix = new float[(int) (aScanPoint * bScanPoint)][4];    //结果矩阵

    private final float xBorderAngle=(float) 90; //视口抬头上限
    DisplayMetrics dm = new DisplayMetrics();
    private Context mContext;


    /** 默认开启重力传感器获取视口，也可以设置触控 **/
    public ViewPort(Context context){
        mContext=context;
        Log.e(Config.TAG_SPHERE,"初始化视口");
        mSensorManager = (SensorManager) mContext.getSystemService(mContext.SENSOR_SERVICE);
        if(Config.GRAVITY_SENSOR) {
            //获取系统服务中的传感器服务，传感器，并注册监听
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorManager.registerListener(mSensorEventListener, mSensor, SensorManager.SENSOR_DELAY_GAME);
            registerSensorListener();   //注册重力传感器监听视口
        }
    }

    /** 触摸监听事件 **/
    public View.OnTouchListener  mOnTouchListener=new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(Config.GRAVITY_SENSOR){
                mSensorManager.unregisterListener(mSensorEventListener);
            }
            float y = event.getY();
            float x = event.getX();

            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:   //划屏幕
                    float dy = y - mPreviousYs;
                    float dx = x - mPreviousXs;
                    yAngle += dx*0.1f;
                    xAngle += dy*0.1f;

                    if (yAngle <0) {
                        yAngle=(yAngle+360)%360;
                    }else{
                        yAngle=yAngle%360;
                    }

                    if (xAngle < -xBorderAngle) {
                        xAngle = -xBorderAngle;
                    } else if (xAngle > xBorderAngle) {
                        xAngle = xBorderAngle;
                    }
//                    rotate();
                    break;
                case MotionEvent.ACTION_UP: //抬手
                    if(Config.GRAVITY_SENSOR){
                        mSensorManager.registerListener(mSensorEventListener,mSensor,SensorManager.SENSOR_DELAY_GAME);
                    }
                    break;
            }
            mPreviousYs = y;
            mPreviousXs = x;
            return true;
        }
    };

    /** 初始化视口采样 **/
    public void onSurfaceChanged(int screenWidth,int screenHeight){
        //初始化视口采样
        //初始化世界坐标系朝向的采样矩阵
//        aDegree= (float) (Math.atan((float)screenWidth/screenHeight)*180f/Math.PI);
//        aDegree= (float) calculateHorizontalFov(90,screenWidth,screenHeight)/2;

        Log.e(Config.TAG_SPHERE," height width "+String.valueOf(screenHeight)+" "+String.valueOf(screenWidth));
        initSampleMatrix(Config.VIEW_A_DEGREE,Config.VIEW_B_DEGREE,viewSampleMatrix);
        initSampleMatrix(Config.SR_AREA_A_DEGREE,Config.SR_AREA_B_DEGREE,macroSampleMatrix);

        for(int i=0;i<Config.CLASS_NUM-1;i++){
            initSampleMatrix(Config.VIEW_A_DEGREE+(Config.VIEWPORT_STEP*i),
                    Config.VIEW_B_DEGREE+(Config.VIEWPORT_STEP*i),rankMatrix[i]);
        }

//        Log.e(TAG,"original ***************************************************** ");
    }


    private void initSampleMatrix(float aDegree,float bDegree, float[][] sampleMatrix){
        //采样点的下标
        int pointIndex=0;
        //视口左上角的第一个点kaishi
        float bStep=(2*bDegree)/(bScanPoint -1);
        float aStep=(2* aDegree)/(aScanPoint -1);
        for(int rowIndex = 0; rowIndex< aScanPoint; rowIndex++){
            for(int colIndex = 0; colIndex< bScanPoint; colIndex++){

                float alpha=(aDegree)-colIndex*aStep;
                float beta=(bDegree)-rowIndex*bStep;


                sampleMatrix[pointIndex][0]= -(float) (Math.sin(Math.toRadians(90f-beta))*Math.sin(Math.toRadians(alpha)));

                //求y点坐标
                sampleMatrix[pointIndex][1]= (float) Math.cos(Math.toRadians(90f-beta));

                //求z坐标
                sampleMatrix[pointIndex][2]= (float) (Math.sin(Math.toRadians(90f-beta))*Math.cos(Math.toRadians(alpha)));
                //求x点坐标

                //齐次坐标
                sampleMatrix[pointIndex][3]=1f;
                if(DebugCfg.DEBUG_VIEWPORT_PROJECTION){
                    Log.e(TAG,"original x,y,z: "+String.valueOf(sampleMatrix[pointIndex][0])+" "+String.valueOf(sampleMatrix[pointIndex][1])+" "+ sampleMatrix[pointIndex][2]+" "+beta);
                }
                pointIndex=pointIndex+1;
            }
        }
    }



    // 定义一个函数，接受垂直fov，宽度和高度作为参数，返回水平fov
    public static double calculateHorizontalFov(double verticalFov, double width, double height) {
        // 计算宽高比
        double aspect = width / height;
        // 将垂直fov转换为弧度
        double verticalFovInRadians = Math.toRadians(verticalFov);
        // 根据公式计算水平fov的一半
        double halfHorizontalFovInRadians = Math.atan(aspect * Math.tan(verticalFovInRadians / 2));
        // 将水平fov的一半乘以2，得到水平fov
        double horizontalFovInRadians = halfHorizontalFovInRadians * 2;
        // 将水平fov转换为角度
        double horizontalFov = Math.toDegrees(horizontalFovInRadians);
        // 返回水平fov
        return horizontalFov;
    }


    private synchronized void transformMacroResSampleMatrix (float xAg, float yAg){

//定义变量
        float xr = (float) Math.toRadians(-xAg);
        float yr = (float) Math.toRadians(yAg);

//循环遍历所有采样点
        for(int rowIndex = 0; rowIndex < aScanPoint; rowIndex++){
            for(int colIndex = 0; colIndex < bScanPoint; colIndex++){

                //计算当前采样点在原始矩阵中的下标
                int pointIndex = (int) (rowIndex * bScanPoint + colIndex);

                //获取原始采样点的坐标
                float x = macroSampleMatrix[pointIndex][0];
                float y = macroSampleMatrix[pointIndex][1];
                float z = macroSampleMatrix[pointIndex][2];

                //绕x轴旋转
                float rotatedY = (float) (y * Math.cos(xr) - z * Math.sin(xr));
                float rotatedZ = (float) (y * Math.sin(xr) + z * Math.cos(xr));

                //绕y轴旋转
                float rotatedX = (float) (x * Math.cos(yr) + rotatedZ * Math.sin(yr));
                rotatedZ = (float) (-x * Math.sin(yr) + rotatedZ * Math.cos(yr));

                //将旋转后的点保存在resultSampleMatrix中
                macroResSampleMatrix[pointIndex][0] = rotatedX;
                macroResSampleMatrix[pointIndex][1] = rotatedY;
                macroResSampleMatrix[pointIndex][2] = rotatedZ;
                macroResSampleMatrix[pointIndex][3] = 1f;
            }
        }
        if(DebugCfg.DEBUG_VIEWPORT_PROJECTION){
            Log.e(Config.TAG_SPHERE,"x,y,z angle: "+String.valueOf(xAg)+" "+String.valueOf(yAg)+" "+String.valueOf(zAngle));
        }
    }


    /** macro area中的tile **/
    public void calculateMacroViewTile(int tRowNum, int tColNum,float xAg, float yAg, Map<Integer, Boolean> tileMap){
        transformMacroResSampleMatrix(xAg,yAg);

        for(int pointIndex = 0, pointNum = (int) (aScanPoint * bScanPoint); pointIndex<pointNum; pointIndex++){
            float x= macroResSampleMatrix[pointIndex][0];
            float y= macroResSampleMatrix[pointIndex][1];
            float z= macroResSampleMatrix[pointIndex][2];

//            float r= (float) Math.sqrt(x*x+y*y+z*z);
            float beta= (float) (Math.acos(y)*180/Math.PI);
            float alpha=(float) (Math.atan2(x,z)*180/Math.PI);
            int res=getTileIndex(tRowNum,tColNum,beta, alpha);
            tileMap.put(res,true);

            if(DebugCfg.DEBUG_VIEWPORT_PROJECTION){
                if(pointIndex==0){
                    Log.e(TAG,"x,y,z: "+String.valueOf(x)+" "+String.valueOf(y)+" "+String.valueOf(z)+" PointIdx "+pointIndex+" idx "+res+" beta "+beta+" alpha "+alpha);
                }
            }

        }

//        Set tiles=tileMap.keySet();
//        Log.e(Config.TAG_SPHERE,"tileset: "+tiles.toString());
    }


    private synchronized void transformViewResSampleMatrix (float xAg, float yAg){

        //定义变量
        float xr = (float) Math.toRadians(-xAg);
        float yr = (float) Math.toRadians(yAg);

        //循环遍历所有采样点
        for(int rowIndex = 0; rowIndex < aScanPoint; rowIndex++){
            for(int colIndex = 0; colIndex < bScanPoint; colIndex++){

                //计算当前采样点在原始矩阵中的下标
                int pointIndex = (int) (rowIndex * bScanPoint + colIndex);

                //获取原始采样点的坐标
                float x = viewSampleMatrix[pointIndex][0];
                float y = viewSampleMatrix[pointIndex][1];
                float z = viewSampleMatrix[pointIndex][2];

                //绕x轴旋转
                float rotatedY = (float) (y * Math.cos(xr) - z * Math.sin(xr));
                float rotatedZ = (float) (y * Math.sin(xr) + z * Math.cos(xr));

                //绕y轴旋转
                float rotatedX = (float) (x * Math.cos(yr) + rotatedZ * Math.sin(yr));
                rotatedZ = (float) (-x * Math.sin(yr) + rotatedZ * Math.cos(yr));

                //将旋转后的点保存在resultSampleMatrix中
                viewResSampleMatrix[pointIndex][0] = rotatedX;
                viewResSampleMatrix[pointIndex][1] = rotatedY;
                viewResSampleMatrix[pointIndex][2] = rotatedZ;
                viewResSampleMatrix[pointIndex][3] = 1f;
            }
        }
        if(DebugCfg.DEBUG_VIEWPORT_PROJECTION){
            Log.e(Config.TAG_SPHERE,"x,y,z angle: "+String.valueOf(xAg)+" "+String.valueOf(yAg)+" "+String.valueOf(zAngle));
        }

    }

    /** 用户看到tile **/
    public void calculateUserViewTile(int tRowNum,int tColNum,float xAg, float yAg, Map<Integer, Boolean> tileMap){
        transformViewResSampleMatrix(xAg,yAg);

        for(int pointIndex = 0, pointNum = (int) (aScanPoint * bScanPoint); pointIndex<pointNum; pointIndex++){
            float x= viewResSampleMatrix[pointIndex][0];
            float y= viewResSampleMatrix[pointIndex][1];
            float z= viewResSampleMatrix[pointIndex][2];

//            float r= (float) Math.sqrt(x*x+y*y+z*z);
            float beta= (float) (Math.acos(y)*180/Math.PI);
            float alpha=(float) (Math.atan2(x,z)*180/Math.PI);
            int res=getTileIndex(tRowNum,tColNum,beta, alpha);
            if(tileMap!=null){
                tileMap.put(res,true);
            }

            if(DebugCfg.DEBUG_VIEWPORT_PROJECTION){
                if(pointIndex==0){
                    Log.e(TAG,"x,y,z: "+String.valueOf(x)+" "+String.valueOf(y)+" "+String.valueOf(z)+" PointIdx "+pointIndex+" idx "+res+" beta "+beta+" alpha "+alpha);
                }
            }

        }

//        Set tiles=tileMap.keySet();
//        Log.e(Config.TAG_SPHERE,"tileset: "+tiles.toString());
    }

    /**
     * @param cMap
     */
    public void calculateUserViewTile(int tRowNum,int tColNum,float xAg, float yAg, Map<Integer, Boolean> eMap,Map<Integer, Boolean> cMap){
        transformViewResSampleMatrix(xAg,yAg);

        for(int pointIndex = 0, pointNum = (int) (aScanPoint * bScanPoint); pointIndex<pointNum; pointIndex++){
            float x= viewResSampleMatrix[pointIndex][0];
            float y= viewResSampleMatrix[pointIndex][1];
            float z= viewResSampleMatrix[pointIndex][2];

//            float r= (float) Math.sqrt(x*x+y*y+z*z);
            float beta= (float) (Math.acos(y)*180/Math.PI);
            float alpha=(float) (Math.atan2(x,z)*180/Math.PI);
            int res=getTileIndex(tRowNum,tColNum,beta, alpha);
            eMap.put(res,true);
            if(cMap!=null){
                cMap.put(res,true);
            }

            if(DebugCfg.DEBUG_VIEWPORT_PROJECTION){
                if(pointIndex==0){
                    Log.e(TAG,"x,y,z: "+String.valueOf(x)+" "+String.valueOf(y)+" "+String.valueOf(z)+" PointIdx "+pointIndex+" idx "+res+" beta "+beta+" alpha "+alpha);
                }
            }
        }

//        Set tiles=tileMap.keySet();
//        Log.e(Config.TAG_SPHERE,"tileset: "+tiles.toString());
    }

    /**
     *
     * @param xAg
     * @param yAg
     * @param tileMap
     */
    public void calculateWeight(int tRowNum,int tColNum,float xAg, float yAg, Map<Integer, Integer> tileMap){

        //定义变量
        float xr = (float) Math.toRadians(-xAg);
        float yr = (float) Math.toRadians(yAg);

        for(int vIdx=0;vIdx<Config.CLASS_NUM-1;vIdx++){
            //循环遍历所有采样点
            for(int rowIndex = 0; rowIndex < aScanPoint; rowIndex++){
                for(int colIndex = 0; colIndex < bScanPoint; colIndex++){

                    //计算当前采样点在原始矩阵中的下标
                    int pointIndex = (int) (rowIndex * bScanPoint + colIndex);

                    //获取原始采样点的坐标
                    float x = rankMatrix[vIdx][pointIndex][0];
                    float y = rankMatrix[vIdx][pointIndex][1];
                    float z = rankMatrix[vIdx][pointIndex][2];

                    //绕x轴旋转
                    float rotatedY = (float) (y * Math.cos(xr) - z * Math.sin(xr));
                    float rotatedZ = (float) (y * Math.sin(xr) + z * Math.cos(xr));

                    //绕y轴旋转
                    float rotatedX = (float) (x * Math.cos(yr) + rotatedZ * Math.sin(yr));
                    rotatedZ = (float) (-x * Math.sin(yr) + rotatedZ * Math.cos(yr));

                    //将旋转后的点保存在resultSampleMatrix中
                    rankTmpMatrix[pointIndex][0] = rotatedX;
                    rankTmpMatrix[pointIndex][1] = rotatedY;
                    rankTmpMatrix[pointIndex][2] = rotatedZ;
                    rankTmpMatrix[pointIndex][3] = 1f;
                }
            }

            if(DebugCfg.DEBUG_VIEWPORT_PROJECTION){
                Log.e(Config.TAG_SPHERE,"x,y,z angle: "+String.valueOf(xAg)+" "+String.valueOf(yAg)+" "+String.valueOf(zAngle));
            }

            for(int pointIndex = 0, pointNum = (int) (aScanPoint * bScanPoint); pointIndex<pointNum; pointIndex++){
                float x= rankTmpMatrix[pointIndex][0];
                float y= rankTmpMatrix[pointIndex][1];
                float z= rankTmpMatrix[pointIndex][2];

//                float r= (float) Math.sqrt(x*x+y*y+z*z);
                float beta= (float) (Math.acos(y)*180/Math.PI);
                float alpha=(float) (Math.atan2(x,z)*180/Math.PI);
                int res=getTileIndex(tRowNum,tColNum,beta, alpha);
                if(!tileMap.containsKey(res)){
                    tileMap.put(res,vIdx);
                }
                if(DebugCfg.DEBUG_VIEWPORT_PROJECTION){
                    if(pointIndex==0){
                        Log.e(TAG,"x,y,z: "+String.valueOf(x)+" "+String.valueOf(y)+" "+String.valueOf(z)+" PointIdx "+pointIndex+" idx "+res+" beta "+beta+" alpha "+alpha);
                    }
                }

            }

        }

//        Set tiles=tileMap.keySet();
//        Log.e(Config.TAG_SPHERE,"tileset: "+tiles.toString());
    }


    public int calculateCenterBlock(int tRowNum,int tColNum,float xAg,float yAg){
//定义变量
        float xr = (float) Math.toRadians(-xAg);
        float yr = (float) Math.toRadians(yAg);

        int rowIndex=(int) aScanPoint/2;
        int colIndex=(int) bScanPoint/2;

        //计算当前采样点在原始矩阵中的下标
        int pointIndex = (int) (rowIndex * bScanPoint + colIndex);

        //获取原始采样点的坐标
        float x = macroSampleMatrix[pointIndex][0];
        float y = macroSampleMatrix[pointIndex][1];
        float z = macroSampleMatrix[pointIndex][2];

        //绕x轴旋转
        float rotatedY = (float) (y * Math.cos(xr) - z * Math.sin(xr));
        float rotatedZ = (float) (y * Math.sin(xr) + z * Math.cos(xr));

        //绕y轴旋转
        float rotatedX = (float) (x * Math.cos(yr) + rotatedZ * Math.sin(yr));
        rotatedZ = (float) (-x * Math.sin(yr) + rotatedZ * Math.cos(yr));


        if(DebugCfg.DEBUG_VIEWPORT_PROJECTION){
            Log.e(Config.TAG_SPHERE,"x,y,z angle: "+String.valueOf(xAg)+" "+String.valueOf(yAg)+" "+String.valueOf(zAngle));
        }

        x=rotatedX;
        y=rotatedY;
        z=rotatedZ;

        float beta= (float) (Math.acos(y)*180/Math.PI);
        float alpha=(float) (Math.atan2(x,z)*180/Math.PI);
        int res=getTileIndex(tRowNum,tColNum,beta, alpha);

        if(DebugCfg.DEBUG_VIEWPORT_PROJECTION){
            if(pointIndex==0){
                Log.e(TAG,"x,y,z: "+String.valueOf(x)+" "+String.valueOf(y)+" "+String.valueOf(z)+" PointIdx "+pointIndex+" idx "+res+" beta "+beta+" alpha "+alpha);
            }
        }
        return res;
    }


    private int  getTileIndex(int tRowNum,int tColNum,float xTemp,float yTemp){

        //过滤点的范围
        if(yTemp%360>270f){
            yTemp=yTemp%360-360;
        }else if(yTemp%360<-90f){
            yTemp=yTemp%360+360;
        }else{
            yTemp=yTemp%360;
        }

        //找到对应的tile下标
        int ySurfaceIndex= (int) Math.floor((float)((-yTemp+270f)/(360f/(float) (tColNum))));
        int xSurfaceIndex= (int) Math.floor((float)((xTemp)/(180f/(float)(tRowNum))));
        return xSurfaceIndex*tColNum+ySurfaceIndex;
    }


    /** 重力传感器相关 **/
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private float timestamp;
    private float angle[] = new float[3];
    private float mPreviousY, mPreviousYs;
    private float mPreviousX, mPreviousXs;

    /** 注销监听器 **/
    public void unRegisterSensorListener(){
        mSensorManager.unregisterListener(mSensorEventListener);
    }

    /** 注册监听器 **/
    public void registerSensorListener(){
        mSensorManager.registerListener(mSensorEventListener,mSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    /**  监听事件具体实现 **/
    private SensorEventListener mSensorEventListener =  new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.accuracy != 0) {
                int type = sensorEvent.sensor.getType();
                switch (type) {
                    case Sensor.TYPE_GYROSCOPE:
                        if (timestamp != 0) {

                            angle[0] += sensorEvent.values[0];
                            angle[1] += sensorEvent.values[1];
//                            angle[2] += sensorEvent.values[2];
                            float anglex = (float) Math.toDegrees(angle[0]);
                            float angley = (float) Math.toDegrees(angle[1]);
//                            float anglez = (float) Math.toDegrees(angle[2]);

                            float dy = angley - mPreviousY;// 计算触控笔Y位移

                            float dx = anglex - mPreviousX;

                            yAngle += dx * 0.025f;
                            xAngle -= dy * 0.025f;

                            if (yAngle % 360 > 270) {
                                yAngle = yAngle % 360 - 360;
                            } else if (yAngle % 360 < -90) {
                                yAngle = yAngle % 360 + 360;
                            } else {
                                yAngle = yAngle % 360;
                            }

                            if (xAngle < -xBorderAngle) {
                                xAngle = -xBorderAngle;
                            } else if (xAngle > xBorderAngle) {
                                xAngle = xBorderAngle;
                            }
                            mPreviousY = angley;
                            mPreviousX = anglex;
//                            rotate();
                        }

                        timestamp = sensorEvent.timestamp;
                        break;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

}
