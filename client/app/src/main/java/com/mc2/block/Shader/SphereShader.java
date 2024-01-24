package com.mc2.block.Shader;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.mc2.Utils.CheckUtils;
import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;
import com.mc2.Utils.Tile;
import com.mc2.block.Render.ViewPort;

import java.nio.IntBuffer;

public class SphereShader implements Shader{

    //顶点着色器
    private static final String vertexShaderCode =
    "#version 300 es                                  \n"+
        "layout (location = 0) in vec4 a_Position;    \n"+
        "layout (location = 1) in vec2 a_TexCoord;     \n"+
        "uniform mat4 u_Matrix;                       \n"+ //矩阵
        "out vec2 vTexCoord;                          \n"+
        "void main() {                                \n"+
            "gl_Position  = u_Matrix * a_Position;    \n"+ //顶点乘以投影矩阵
            "//gl_PointSize = 10.0;                   \n"+ //TODO ?
            "vTexCoord = a_TexCoord;                   \n"+ //传入纹理顶点到片元着色器
        "}                                            \n";

    //片段着色器
    private static final String fragmentShaderCode=
    "#version 300 es                                       \n"+
        "precision mediump float;                          \n"+
        "uniform sampler2D s_Texture;                      \n"+ //接收刚才顶点着色器传入的纹理坐标(s,t)
        "in vec2 vTexCoord;                                \n"+
        "out vec4 vFragColor;                              \n"+
        "void main() {                                     \n"+
        "    vFragColor = texture(s_Texture,vTexCoord);    \n"+ //纹理数据和纹理顶点输入其中
        "}                                                 \n";

    // in GM
    private final int shaderProgram;
    private final int a_Position;
    private final int a_TexCoord;
    private final int s_Texture;
    private final int u_Matrix;
//    private int[] mToBeDelTex=new int[1];

    // in SHM
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mMVPMatrix = new float[16];

    private int mWidth=Config.SPHERE_TEXTURE_WIDTH;
    private int mHeight=Config.SPHERE_TEXTURE_HEIGHT;
    private Sphere[] mSphere;
    private ViewPort mViewPort;


    public SphereShader(ViewPort viewPort){
        if(DebugCfg.CHECK_INPUT){
           CheckUtils.checkObjIsNull(viewPort);
        }
        mViewPort=viewPort;

        mSphere=new Sphere[Config.TILE_SCHEME_NUM]; //对每种tiling方案都提前准备好渲染数据
        for(int tsIdx=0;tsIdx<Config.TILE_SCHEME_NUM;tsIdx++){
            mSphere[tsIdx]=new Sphere(20,
                    Config.TILE_ROW_NUM_SET[tsIdx],
                    Config.TILE_COLUMN_NUM_SET[tsIdx]);
        }

        shaderProgram = ShaderUtils.buildProgram(vertexShaderCode, fragmentShaderCode);
        GLES30.glUseProgram(shaderProgram);
        a_Position = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        a_TexCoord = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        s_Texture = GLES30.glGetUniformLocation(shaderProgram, "s_Texture");
        u_Matrix = GLES20.glGetUniformLocation(shaderProgram, "u_Matrix");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        GLES30.glViewport(0,0,width,height);

        float ratio = (float) width / height;
        ratio=1.4f;
        Log.e(Config.TAG_SHADER,"ratio "+ratio);
        ratio=2.1472223f; //对应67度视口
        mWidth=width;
        mHeight=height;
        Matrix.perspectiveM(projectionMatrix, 0, 90f, ratio, 1f, 20f);  //TODO zFar
        Matrix.setLookAtM(viewMatrix,
                0, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 1.0f, 0.0f);
    }

    @Override
    public void prepareDraw() {
        //1ns=10^-6 ms
        GLES30.glViewport(0, 0, mWidth,mHeight );
        GLES30.glClearColor(0f, 0f, 0f, 0f);  //alpha 0 is transparent 1 is opaque
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        if(DebugCfg.DEBUG_OPENGL){
            ShaderUtils.checkError("1");
        }

        //calculate viewport
        Matrix.setIdentityM(modelMatrix, 0); //初始化空矩阵
        Matrix.rotateM(modelMatrix, 0, (float) -mViewPort.xAngle, 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, (float) -mViewPort.yAngle, 0, 1, 0);
        Matrix.rotateM(modelMatrix, 0, (float) -mViewPort.zAngle, 0, 0, 1);

        //calculate view and projection
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);   //模型矩阵乘上摄像头视角矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);  //投影矩阵
        GLES30.glUniformMatrix4fv(u_Matrix, 1, false, mMVPMatrix, 0);
        if(DebugCfg.DEBUG_OPENGL) {
            ShaderUtils.checkError("2");
        }
    }

    @Override
    public void draw(int tSIdx, Tile tile, int fIdx) {
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_OPENGL){
            CheckUtils.checkObjIsNull(tile);
            CheckUtils.checkObjIsNull(tile.tex);
        }
        if(Config.ENABLE_DRAW){ //是否渲染画面
            //use program
            GLES30.glUseProgram(shaderProgram);

            if(DebugCfg.DEBUG_OPENGL) {
                ShaderUtils.checkError("3");
            }

            //load point
//            mSphere.uploadVertexBuffer(a_Position,tile.idx1);
//            mSphere.uploadTextureBuffer(a_TexCoord,tile.idx1);
            mSphere[tSIdx].uploadVertexBuffer(a_Position,tile.idx);
            mSphere[tSIdx].uploadTextureBuffer(a_TexCoord,tile.idx);


            if(DebugCfg.DEBUG_OPENGL) {
                ShaderUtils.checkError("4");
            }

            //bind texture
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tile.tex[fIdx]);

            GLES30.glUniform1i(s_Texture, 0);

            if(DebugCfg.DEBUG_OPENGL) {
                ShaderUtils.checkError("5");
            }

            //draw
            mSphere[tSIdx].draw();

            if(DebugCfg.DEBUG_OPENGL) {
                ShaderUtils.checkError("6");
            }
        }
    }

    @Override
    public void recycle(Tile tile){
        if(tile.tex!=null){
            GLES30.glDeleteTextures(Config.FRAME_NUM, IntBuffer.wrap(tile.tex));
        }
    }

    @Override
    public void endDraw(int fIdx) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        if(DebugCfg.DEBUG_OPENGL) {
            ShaderUtils.checkError("9");
        }
    }
}
