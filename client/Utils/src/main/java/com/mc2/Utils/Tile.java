package com.mc2.Utils;

public class Tile {
    public Chunk parent;
    public String videoPath;
    public int[] tex;

    /**
     * pos mean the rendering position
     * It has four float: x1,y1; x2,y2
     */
    public float[] pos;
    public int idx;
    //macro
    public int rowLen;  //行长
    public int colLen;  //列长

    public int resolution; //原始缝合后分辨率
    public int width; //tile视频帧宽度
    public int height; //tile视频帧高度

    //dc
    public boolean isToDc=false;    //是否进入解码器
    public float decodingT=-1;

    /**
     * init a tile when the first decision stage. `isSR` and others will fill in the second stage.
     * @param i_videoPath
     * @param i_pos
     * @param i_resolution
     */
    public Tile(String i_videoPath,
                float[] i_pos,
                int i_idx,
                int i_resolution,
                int i_width,
                int i_height,
                int i_rowLen,
                int i_colLen
                ){
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_MEDIA_SOURCE){
            CheckUtils.checkObjIsNull(i_videoPath);
            CheckUtils.checkObjIsNull(i_pos);
        }
        videoPath=i_videoPath;
        pos=i_pos;
        idx =i_idx;
        resolution=i_resolution;
        width=i_width;
        height=i_height;
        rowLen=i_rowLen;
        colLen=i_colLen;
    }

    public void release(){

    }
}
