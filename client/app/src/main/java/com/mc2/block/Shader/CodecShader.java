package com.mc2.block.Shader;


import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import com.mc2.Utils.DebugCfg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CodecShader {
    // 顶点着色器
    private static final String vertexShaderCode =
            "#version 300 es                       \n" +    //着色器版本
                    "in vec4 a_Position;           \n" +    //输入名为a_Position的vec4的4分量向量，？
                    "in vec2 a_TexCoord;           \n" +    //输入名为a_TexCoord的vec2的2分量向量，若有layout (location = 0)则表示这个变量的position是顶点属性0
                    "out vec2 v_TexCoord;          \n" +    //输出名为v_TexCoord的vec2的2分量向量
                    "void main() {                 \n" +
                    "   gl_Position = a_Position;  \n" +    //它将a_Position输入属性拷贝到名为gl_Position的特殊输出变量
                    "   v_TexCoord = a_TexCoord;   \n" +
                    "}                             \n";

    // 片段着色器
    private static final String ObjectShaderCode =
            "#version 300 es                                                 \n" +
                    "#extension GL_OES_EGL_image_external_essl3 : require    \n" +
                    "precision mediump float;                                \n" +  //声明着色器中浮点变量的默认精度
                    "in vec2 v_TexCoord;                                     \n" +  //输入名为v_TexCoord的vec2的2分量向量
                    "out vec4 fragColor;                                     \n" +  //输出名为fragColor的vec4的4分量向量
                    "uniform samplerExternalOES s_Texture;                   \n" +
                    "void main() {                                           \n" +
                    "   fragColor = texture(s_Texture, v_TexCoord);          \n" +
                    "}                                                       \n";

    // 顶点坐标
    private final float[] vertexData = {
            -1f, 1f,
            1f, 1f,
            -1f, -1f,
            1f, -1f,
    };
    private final FloatBuffer vertexBuffer;
    private final int vertexVBO;

    private final float[] textureData = {   //显示是正放
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f,

    };

    // 纹理坐标
//    private final float[] textureData = {   //正常坐标系显示是倒置
//            0f, 0f,
//            1f, 0f,
//            0f, 1f,
//            1f, 1f,
//    };

    private final FloatBuffer textureBuffer;
    private final int textureVBO;

    private final int shaderProgram;
    private final int a_Position;
    private final int a_TexCoord;
    private final int s_Texture;

    private final int oesTextureId;
    private final int FBO;

    public CodecShader( int oesTextureId) {

        this.oesTextureId = oesTextureId;
        FBO = ShaderUtils.createFBO();

        int[] vbo = new int[2];
        GLES30.glGenBuffers(2, vbo, 0);

        //分配内存空间,每个浮点型占4字节空间
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);
        vertexVBO = vbo[0];

        //分配内存空间,每个浮点型占4字节空间
        textureBuffer = ByteBuffer.allocateDirect(textureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);
        textureVBO = vbo[1];

        shaderProgram = ShaderUtils.buildProgram(vertexShaderCode, ObjectShaderCode);
        GLES30.glUseProgram(shaderProgram);
        a_Position = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        a_TexCoord = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        s_Texture = GLES30.glGetUniformLocation(shaderProgram, "s_Texture");

    }

    public void draw(int width,int height,int texId) {

        //1ns=10^-6 ms
//        long t1=System.nanoTime();

        GLES30.glViewport(0, 0, width, height);

        if(DebugCfg.DEBUG_OPENGL){
            ShaderUtils.checkError("0");
        }

        //清理材质
        GLES30.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);


        if(DebugCfg.DEBUG_OPENGL){
            ShaderUtils.checkError("1");
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, FBO);


        //绑定到FBO OES->texture
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, texId, 0);


//        if(GLES30.glCheckFramebufferStatus(FBO) != GLES30.GL_FRAMEBUFFER_COMPLETE){
//            Log.d(Config.TAG_SHADER,"fbo status wrong");
//        }

        if(DebugCfg.DEBUG_OPENGL){
            ShaderUtils.checkError("2");
        }

        GLES30.glUseProgram(shaderProgram);

        if(DebugCfg.DEBUG_OPENGL){
            ShaderUtils.checkError("3");
        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexVBO);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexData.length * 4, vertexBuffer, GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(a_Position);
        GLES30.glVertexAttribPointer(a_Position, 2, GLES30.GL_FLOAT, false, 2 * 4, 0); //size为2则表示一个顶点有两个float，stride 2*4（float）=8个值一个步长，即一个顶点占有的数据量大小

        if(DebugCfg.DEBUG_OPENGL){
            ShaderUtils.checkError("4");
        }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, textureVBO);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, textureData.length * 4, textureBuffer, GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(a_TexCoord);
        GLES30.glVertexAttribPointer(a_TexCoord, 2, GLES30.GL_FLOAT, false, 2 * 4, 0); //size为2则表示一个顶点有两个float，stride 2*4（float）=8个值一个步长，即一个顶点占有的数据量大小

        if(DebugCfg.DEBUG_OPENGL){
            ShaderUtils.checkError("5");
        }

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        //激活OES材质

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId);

        GLES30.glUniform1i(s_Texture, 0);

        if(DebugCfg.DEBUG_OPENGL){
            ShaderUtils.checkError("6");
        }
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        if(DebugCfg.DEBUG_OPENGL){
            ShaderUtils.checkError("7");
        }

//        //save to file
//        if(texId==509||texId==569||texId==599){
//            ShaderUtils.saveWindow2Bitmap( Config.TILE_TEXTURE_WIDTH, Config.TILE_TEXTURE_HEIGHT, String.valueOf(texId));
//            ShaderUtils.saveTexture2Image(texId,Config.TILE_TEXTURE_WIDTH,Config.TILE_TEXTURE_HEIGHT, "unityTex"+String.valueOf(texId));
//        }

        if(DebugCfg.DEBUG_OPENGL){
            ShaderUtils.checkError("8");
        }

        GLES30.glDisableVertexAttribArray(a_Position);
        GLES30.glDisableVertexAttribArray(a_TexCoord);

        //解绑
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        if(DebugCfg.DEBUG_OPENGL){
            ShaderUtils.checkError("9");
        }

    }


}
