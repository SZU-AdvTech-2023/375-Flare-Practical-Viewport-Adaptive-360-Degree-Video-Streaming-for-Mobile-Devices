package com.mc2.Utils;

import android.util.Log;

public class Config {
    //按照我们的播放分辨率为3840x1920

    /**
     * Package Tag
     */
    public static final String TAG_BLOCK="BLOCK";
    public static final String TAG_LOOPER="Looper";
    public static final String TAG_DECODER="Decoder";
    public static final String TAG_SHADER="Shader";
    public static final String TAG_UTILS="Utils";
    public static final String TAG_MEDIA_SOURCE="MediaSource";
    public static final String TAG_VIDEO_TRACK="VideoTrack";
    public static final String TAG_TILING_ADAPTOR="TilingAdapter";

    public static final String TAG_RENDER="Render";
    public static final String TAG_HTTP="Http";
    public static final String TAG_SPHERE = "Sphere";
    public static final String TAG_VIEWPORT="ViewPort";
    public static final String TAG_PREDICTOR ="Predictor";
    public static final String TAG_DC_SR_SCHEME="DCSRScheme";

    //TODO macro tile supported
    public static final boolean LOCAL_MODE=false; //是否使用本地模式
    public static final boolean CIRCULATION_MOD=true; //是否循环播放，默认循环10s
    public static final boolean ENABLE_DRAW=true; //是否渲染画面
    public static final boolean ENABLE_HARDWARE_CODEC=true; //是否开启硬件解码器
    public static final boolean ENABLE_PREDICTION=true; //是否用预测分配视口
    public static final boolean ENABLE_RATE_ADAPTATION=false; //是否开启码率自适应
    public static final boolean ENABLE_MACRO=true; //是否启用macroTile
    public static final int VIDEO_LENGTH=30; //视频长度 路径为1-10
    public static final int EXP_SAVE_NUMBER=45; //保存多少个chunk的实验结果

    public static final boolean GRAVITY_SENSOR=false;   //是否使用重力传感器

    public static final String SERVER_ADDRESS="192.168.88.120";
    public static final int SERVER_PORT=8888;


    /**
     * default video id
     */
    public static final int VIDEO_ID=0;
    public static final String VIDEO_NAME="video0";

    /**
     * bandwidth data set
     */
    public static final boolean ENABLE_NETWORK_TRACE=true;
    public static final int FIX_BANDWIDTH=1000000; //固定带宽，单位Mbps
    public static final String BANDWIDTH_DATA_HIGH="high";
    public static final String BANDWIDTH_DATA_MEDIUM="medium";
    public static final String BANDWIDTH_DATA=BANDWIDTH_DATA_MEDIUM;


    /**
     * Decision settings
     */
    //Flare->0 PARSEC->1 HAND->2 TBSR->3 NEMO->4
    public static final int MODE_FLARE=0;
    public static final int MODE_PARSEC=1;
    public static final int MODE_HAND=2;
    public static final int MODE_TBSR=3;
    public static final int MODE_NEMO=4;
    public static final int DECISION_MODE=MODE_TBSR;

    public static final boolean ENABLE_DC_ABORTION =false;
    public static final int DC_ABORTION_TIME =960;
    /**
     * Nemo
     */
    public static final int T_DECODER_SUM=4;   //解码器个数

    /**
     * Decoder Config
     */
    public static final int DECODER_THREAD_PRIORITY=-1; //解码线程优先级

    public static final int RESOLUTION_SUM=4; //有几种分辨率
    public static final int[] RESOLUTION_SET={240,480,960,1920}; //视频分辨率质量等级

    private static final float D_T_E =3;  //thread efficient
//    public static final int[] MACRO_BITRATE_SET_5_10={10118,24037,108871,248025}; //5x10 macro area 每个tile平均大小，单位bit
    public static final int[] MACRO_BITRATE_SET_5_10={10118,24037,128871,268025}; //5x10 macro area 每个tile平均大小，单位bit
//    public static final int[] BITRATE_SET_5_10={15475,27998,140597,291254}; //5x10 每个tile的平均大小，单位bit
    public static final int[] BITRATE_SET_5_10={13267,25482,139668,266604}; //5x10 每个tile的平均大小，单位bit
    public static final float[] DECODING_TIME_5_10={101,89,114, 140}; //5x10 每个tile的大致解码时间

//    public static final int[] BITRATE_SET_4_8={20023,41693,226809,469309}; //4x8 每个tile的大致bit大小
//    public static final float[] DECODING_TIME_4_8={23.2f,26.0f,38.7f,39.5f}; //4x8 每个tile的大致解码时间

    public static final int[] BITRATE_SET_4_6={24385,53686,305540,645543}; //4x6 每个tile的大致bit大小 PARSEC
    public static final float[] DECODING_TIME_4_6={121/D_T_E,153/D_T_E,100/D_T_E, 108/D_T_E}; //4x6 flare
//    public static final float[] DECODING_TIME_4_6={121/D_T_E,159/D_T_E,111/D_T_E, 128/D_T_E}; //4x6 parsec

    public static final int[] BITRATE_SET_3_3={76366,189898,1315130,2613020}; //3x3 每个tile的大致bit大小
    public static final float[] DECODING_TIME_3_3={25.2f,30.0f,35.7f,39.5f}; //3x3 每个tile的大致解码时间

    public static final int[] BITRATE_SET=BITRATE_SET_5_10;
    public static final float[] DECODING_TIME=DECODING_TIME_5_10;
    public static final int[] MACRO_BITRATE_SET=MACRO_BITRATE_SET_5_10;

    public static final int[] SCALE_SET={4,4,4,2};
    public static final String[] MODEL_SET={"NEMO_S_B2_F4_S4_deconv","NEMO_S_B2_F4_S4_deconv","NEMO_S_B2_F4_S4_deconv","NEMO_S_B2_F4_S2_deconv"};
    public static final String PROFILE_NAME="nemo_0.5_16.profile";

    public static final int HARDWARE_MIN_REQUIREMENT=128;

    //不同的tiling方案
    public static final int TILE_SCHEME_NUM=1;  //有多少种tiling方案

    public static final int[] TILE_ROW_NUM_SET={5};
    public static final int[] TILE_COLUMN_NUM_SET={5};
    public static final int TILE_SCHEME_INDEX=0;   //   目前是哪个tiling方案

    public static final int WIDTH_HEIGHT_RATIO =2;    //宽高比
    public static final int SURFACE_TEXTURE_WIDTH=3840;    //单个tile纹理宽度
    public static final int SURFACE_TEXTURE_HEIGHT=1920;    //单个tile纹理高度
    public static final int FRAME_NUM=30;   //帧数
    public static final  int CHUNK_DURATION=1; //chunk间隔大小 s

    /**
     * Tiling Adaptor
     */

//    public static final int FIX_BANDWIDTH=20000000;
    public static final int PRE_ALLOCATION_TIME=3;  //提前分配的buffer长度 default->3 8k->1
    public static final int INIT_PREDICTION_TIME=2; //播放到第几秒启动预测算法 default->2 8k->1
    public static final int FIX_ALLOCATION_RATE=1; //固定分配模式下的码率
    public static final int ALLOCATE_DURATION=1; // 分配间隔，对于chunk0，下载时跳过n个chunk
    public static final int FIX_ALLOCATE_TIME =ALLOCATE_DURATION;    //固定分配下分配的chunk数
    public static final int MIN_MACRO_AREA_SIZE=6;

    public static final float ALPHA =1;
//    public static final float BETA =0.5f; //tbsr high
    public static final float BETA =0.2f;   //flare
//    public static final float BETA =0.1f; //tbsr
//    public static final float BETA =0.1f; //nemo mid
    public static final float HIGH_P_ESTIMATION=1.4f/50f;
    public static final float INIT_BUFFER=1;
    public static final float K_MAX=5.0f;

    private static final float S_T_E =3f;  //thread efficient
    public static final float[] SR_TIME_3_3 ={159/S_T_E,490/S_T_E,1611/S_T_E,578/S_T_E};  //3x3 超分时间 4thread
//    public static final float[] SR_TIME_3_3 ={159/S_T_E,490/S_T_E,1611/S_T_E,578/S_T_E};  //3x3 超分时间

    public static final float[] SR_TIME_4_6 ={1350/S_T_E,1367/S_T_E,850/S_T_E,478/S_T_E};  //4x6 超分时间
//    public static final float[] SR_TIME_4_6 ={150/S_T_E,237/S_T_E,444/S_T_E,220/S_T_E};  //4x6 超分时间
//    public static final float[] SR_TIME_5_10={170/S_T_E,270/S_T_E,290/S_T_E,220/S_T_E};  //5X10 超分时间
//    public static final float[] SR_TIME_5_10={200/S_T_E,250/S_T_E,280/S_T_E,350/S_T_E};  //5X10 超分时间 high
//    public static final float[] SR_TIME_5_10={400/S_T_E,600/S_T_E,650/S_T_E,550/S_T_E};  //5X10 超分时间 ablation dl
    public static final float[] SR_TIME_5_10={140/S_T_E,150/S_T_E,180/S_T_E,190/S_T_E}; //5X10 超分时间 ablation dl
//    public static final float[] SR_TIME_5_10={140/S_T_E,150/S_T_E,180/S_T_E,190/S_T_E};  //5X10 超分时间 mid
    public static final float[] SR_TIME = SR_TIME_5_10;


    /**
     * Shader Config
     */
    public static final int SPHERE_VERTICAL_POINT_PARAM=4;
    public static final int SPHERE_HORIZONTAL_POINT_PARAM=4;

    //deprecated
    public static final int SPHERE_TEXTURE_WIDTH=1920;
    public static final int SPHERE_TEXTURE_HEIGHT=1080;
    /**
     * Viewport
     */
    public static final int CLASS_NUM=4;
    public static final int VIEWPORT_STEP=10;
//    public static final int VIEW_B_DEGREE =45;
//    public static final float VIEW_A_DEGREE =55f;
    public static final int VIEW_B_DEGREE =45;
    public static final float VIEW_A_DEGREE =65f;
    public static final int SR_AREA_B_DEGREE =47;
    public static final float SR_AREA_A_DEGREE =67f;



    /**
     * Predictor
     */
    public static final int CHUNK_TIME=1000; //chunk长度
    public static final int PREDICTION_DURATION=0;    //预测多久以后，预测间隔
    public static final int SAMPLING_PERIOD=200; //采样周期 ms
    public static final int SAMPLING_FREQUENCY=1000/SAMPLING_PERIOD; //1s内采样几次数据
    public static final int HISTORY_WINDOW=2000/SAMPLING_PERIOD;  //历史窗口里有多少采样条目,2s窗口
    public static final int PREDICTION_TIME=3000; //预测时间长度
    public static final int PREDICTION_CHUNK_NUM =PREDICTION_TIME/CHUNK_TIME;
    public static final int PREDICTION_POINT=Config.PREDICTION_TIME/Config.SAMPLING_PERIOD; //预测点数

    /**
     * Render
     */
    public static final int RESPONSE_INTERVAL=1000/FRAME_NUM; //100 ms 响应间隔
    public static final int RESPONSE_INTERVAL_SUM=1000/RESPONSE_INTERVAL; //1s会响应多少次
    public static final int PREDICTION_RESPONSE_INTERVAL=RESPONSE_INTERVAL_SUM/SAMPLING_FREQUENCY;   //一个预测间隔等于多少个响应间隔



    /**
     * check config
     */
    public static void checkConfig(){
        if(Config.T_DECODER_SUM ==0){
            Log.d(Config.TAG_UTILS,"Config error 4");
        }
    }

    /**
     * VideoTrack Config
     */



}
