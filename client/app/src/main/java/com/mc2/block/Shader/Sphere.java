package com.mc2.block.Shader;

import static com.mc2.block.Shader.ShaderUtils.checkError;

import android.opengl.GLES30;

import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


public class Sphere {
    private static final int sPositionDataSize = 3;
    private static final int sTextureCoordinateDataSize = 2;

    private FloatBuffer[] mVertexBuffers;
    private FloatBuffer[] mTextureBuffers;
    private ShortBuffer mIndexBuffer;
    private int mNumIdx;
    private int mNumVer;
    private int mNumTex;

    private final float vD; //点间隔角度
    private final float hD;
    private final int tVRange;   //一个tile占用点范围
    private final int tHRange;
    private final float PI = (float) Math.PI;
    private final float PI_2 = (float) (Math.PI / 2);
    private final int pointSum;

    private final int mTRowNum;   //tile scheme
    private final int mTColNum;
    private final int mTSum;

    public Sphere(float radius, int tRowNum, int tColNum) {
        int vPointSum=Config.SPHERE_VERTICAL_POINT_PARAM*tRowNum;
        int hPointSum=Config.SPHERE_HORIZONTAL_POINT_PARAM*tColNum;

        mTRowNum=tRowNum;
        mTColNum=tColNum;
        mTSum=tRowNum*tColNum;

        mVertexBuffers=new FloatBuffer[mTSum];
        mTextureBuffers=new FloatBuffer[mTSum];

        vD = 1f/(float)vPointSum;  //jing xian vertical
        hD = 1f/(float)hPointSum; //fan shape
        tVRange=vPointSum/mTRowNum;  //tile行范围
        tHRange=hPointSum/mTColNum;  //tile列范围

        int vIdx, hIdx;
        float x, y, z;
        int rowIdx,colIdx;

//        int numPoint = (vertical + 1) * (horizon + 1);
        pointSum = (tVRange+1) * (tHRange+1);
        float[] vertexData = new float[pointSum * 3];
        float[] textureData = new float[pointSum * 2];
        short[] indices = new short[pointSum * 6];

        for(int tIdx=0;tIdx<mTSum;tIdx++){
            rowIdx=tIdx/mTColNum;
            colIdx=tIdx%mTColNum;

            //store the float type in shared mem and load when drawing
            //纹理映射2d-3d
            int t = 0, v = 0;
            for(vIdx = rowIdx*tVRange; vIdx < (rowIdx+1)*tVRange+1; vIdx++) {
                for(hIdx = colIdx*tHRange; hIdx < (colIdx+1)*tHRange+1; hIdx++) {
                    x = (float) (Math.cos(2*PI * hIdx * hD) * Math.sin( PI * vIdx * vD ));
                    y = (float) Math.sin( PI_2 - PI * vIdx * vD );
                    z = (float) (Math.sin(2*PI * hIdx * hD) * Math.sin( PI * vIdx * vD ));
                    textureData[t++] = hIdx*hD*mTColNum;
                    textureData[t++] = vIdx*vD*mTRowNum;

                    vertexData[v++] = x * radius;
                    vertexData[v++] = y * radius;
                    vertexData[v++] = z * radius;
                }
            }

            mNumVer = vertexData.length * 4;
            mNumTex = textureData.length * 4;
            // initialize vertex byte buffer for shape coordinates
            mVertexBuffers[tIdx] = ByteBuffer.allocateDirect(vertexData.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(vertexData);
            mVertexBuffers[tIdx].position(0);

            // initialize texture byte buffer for shape coordinates
            mTextureBuffers[tIdx] = ByteBuffer.allocateDirect(textureData.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(textureData);
            mTextureBuffers[tIdx].position(0);
        }


        //this could be reused
        //球体绘制坐标索引，用于  glDrawElements
        int counter = 0;
        int sectorsPlusOne = hPointSum/mTColNum + 1;
        for(vIdx = 0; vIdx < tVRange; vIdx++){
            for(hIdx = 0; hIdx < tHRange; hIdx++) {
                indices[counter++] = (short) (vIdx * sectorsPlusOne + hIdx);       //(a)
                indices[counter++] = (short) ((vIdx+1) * sectorsPlusOne + (hIdx));    //(b)
                indices[counter++] = (short) ((vIdx) * sectorsPlusOne + (hIdx+1));  // (c)
                indices[counter++] = (short) ((vIdx) * sectorsPlusOne + (hIdx+1));  // (c)
                indices[counter++] = (short) ((vIdx+1) * sectorsPlusOne + (hIdx));    //(b)
                indices[counter++] = (short) ((vIdx+1) * sectorsPlusOne + (hIdx+1));  // (d)
            }
        }

        // initialize byte buffer for the draw list
        mIndexBuffer = ByteBuffer.allocateDirect(indices.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(indices);
        mIndexBuffer.position(0);
        mNumIdx =indices.length;


    }

    public void uploadVertexBuffer(int positionHandle,int tIdx){
        GLES30.glVertexAttribPointer(positionHandle, sPositionDataSize, GLES30.GL_FLOAT, false, 0, mVertexBuffers[tIdx]);
        if(DebugCfg.DEBUG_OPENGL){
            checkError("glVertexAttribPointer maPosition");
        }
        GLES30.glEnableVertexAttribArray(positionHandle);
        if(DebugCfg.DEBUG_OPENGL){
            checkError("glEnableVertexAttribArray maPositionHandle");
        }
    }

    public void uploadTextureBuffer(int textureCoordinateHandle,int tIdx){
        GLES30.glVertexAttribPointer(textureCoordinateHandle, sTextureCoordinateDataSize, GLES30.GL_FLOAT, false, 0, mTextureBuffers[tIdx]);
        if(DebugCfg.DEBUG_OPENGL){
            checkError("glVertexAttribPointer maTextureHandle");
        }
        GLES30.glEnableVertexAttribArray(textureCoordinateHandle);
        if(DebugCfg.DEBUG_OPENGL){
            checkError("glEnableVertexAttribArray maTextureHandle");
        }
    }


    public void draw() {
        mIndexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, mNumIdx, GLES30.GL_UNSIGNED_SHORT, mIndexBuffer);
    }

    public void endDraw(){

    }
}
