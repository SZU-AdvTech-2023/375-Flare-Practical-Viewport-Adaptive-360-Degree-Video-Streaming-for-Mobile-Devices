package com.mc2.block.Shader;



import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLES32;
import android.opengl.GLUtils;
import android.text.format.Formatter;
import android.util.Log;

import com.mc2.Utils.Config;
import com.mc2.Utils.DebugCfg;
import com.mc2.Utils.PathCfg;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ShaderUtils {

    // 根据类型编译着色器
    public static int compileShader(int type, String shaderCode) {
        // 根据不同的类型创建着色器 ID
        final int shaderObjectId = GLES30.glCreateShader(type);
        if (shaderObjectId == 0) {
            return 0;
        }
        // 将着色器 ID 和着色器程序内容连接
        GLES30.glShaderSource(shaderObjectId, shaderCode);
        // 编译着色器
        GLES30.glCompileShader(shaderObjectId);
        // 为验证编译结果是否失败
        final int[] compileStatus = new int[1];
        // glGetShaderiv函数比较通用，在着色器阶段和 OpenGL 程序阶段都会通过它来验证结果。
        GLES30.glGetShaderiv(shaderObjectId, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            Log.d(Config.TAG_SHADER,"compileShader error --> " + shaderCode);
            // 失败则删除
            GLES30.glDeleteShader(shaderObjectId);
            return 0;
        }
        return shaderObjectId;
    }


    // 创建 OpenGL 程序和着色器链接
    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        // 创建 OpenGL 程序 ID
        final int programObjectId = GLES30.glCreateProgram();
        if (programObjectId == 0) {
            return 0;
        }
        // 链接上 顶点着色器
        GLES30.glAttachShader(programObjectId, vertexShaderId);
        // 链接上 片段着色器
        GLES30.glAttachShader(programObjectId, fragmentShaderId);
        // 链接着色器之后，链接 OpenGL 程序
        GLES30.glLinkProgram(programObjectId);
        // 验证链接结果是否失败
        final int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(programObjectId, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.d(Config.TAG_SHADER,"linkProgram error");
            // 失败则删除 OpenGL 程序
            GLES30.glDeleteProgram(programObjectId);
            return 0;
        }
        return programObjectId;
    }


    // 链接了 OpenGL 程序后，就是验证 OpenGL 是否可用。
    public static boolean validateProgram(int programObjectId) {
        GLES30.glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        GLES30.glGetProgramiv(programObjectId, GLES30.GL_VALIDATE_STATUS, validateStatus, 0);
        return validateStatus[0] != 0;
    }

    // 创建 OpenGL 程序过程
    public static int buildProgram(String vertexShaderSource, String fragmentShaderSource) {
        // 编译顶点着色器
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource);
        // 编译片段着色器
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderSource);
        int program = linkProgram(vertexShader, fragmentShader);
        boolean valid = validateProgram(program);
        Log.d(Config.TAG_SHADER,"buildProgram valid = " + valid);
        return program;
    }

    public static int createFBO() {
        int[] fbo = new int[1];
        GLES30.glGenFramebuffers(fbo.length, fbo, 0);
        return fbo[0];
    }

    public static int createVAO() {
        int[] vao = new int[1];
        GLES30.glGenVertexArrays(vao.length, vao, 0);
        return vao[0];
    }

    public static int createVBO() {
        int[] vbo = new int[1];
        GLES30.glGenBuffers(2, vbo, 0);
        return vbo[0];
    }

    public static int[] createTileTexturesSet(int width,int height){
        int[] textures = new int[Config.FRAME_NUM];
        GLES30.glGenTextures(textures.length, textures, 0);

        for(int idx=0;idx<Config.FRAME_NUM;idx++) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[idx]);
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0,
                    GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);
//            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

        }
        return textures;
    }


    public static int createOESTextureID(int width, int height) {
        int[] texture = new int[1];
        GLES30.glGenTextures(texture.length, texture, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES30.glTexImage2D(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0, GLES30.GL_RGBA, width, height, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
//        GLES30.glGenerateMipmap(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        return texture[0];
    }

    @Deprecated
    public static int[] t_createTenOESTextureID() {
        int[] texture = new int[1500];
        GLES30.glGenTextures(texture.length, texture, 0);
        long t1=System.currentTimeMillis();
        for(int idx=0;idx<1500;idx++){
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[idx]);
            GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
            GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

//            GLES30.glGenerateMipmap(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
//            Log.d(TAG,"texture id "+texture[idx]);
        }
        long t2=System.currentTimeMillis();
        Log.d(Config.TAG_SHADER,"create 1500 texture cost time "+(t2-t1));
        return texture;
    }

    @Deprecated
    public static int create2DTextureId(int width, int height) {
        int[] textures = new int[1];
        GLES30.glGenTextures(textures.length, textures, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        return textures[0];
    }

    @Deprecated
    public static int[] t_create2DTextureId(Context context,int width, int height) {
        int[] textures = new int[1500];
        GLES30.glGenTextures(textures.length, textures, 0);
        long t1=System.currentTimeMillis();
        for(int idx=0;idx<1500;idx++) {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[idx]);
                GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0,
                        GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
                GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
                GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
//                GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
//                Log.d(TAG,"texture id "+textures[idx]);
        }
        long t2=System.currentTimeMillis();
        Log.d(Config.TAG_SHADER,"create 1500 texture cost time "+(t2-t1));
//        t_getAppMemoryInfo(context);
        return textures;
    }

    // Get a MemoryInfo object for the device's current memory status.
    @Deprecated
    private static void t_getAppMemoryInfo(Context context){
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
//设备内存
        manager.getMemoryInfo(mi);
        String totalMem = Formatter.formatFileSize(context, mi.totalMem);
        System.out.println("设备的运行总内存 totalMem: " + totalMem);
        String availMem = Formatter.formatFileSize(context, mi.availMem);
        System.out.println("设备剩余运行内存 availMem: " + availMem);
//最大分配内存获取方法1
        int memory = manager.getMemoryClass();
        System.out.println("app可分配最大内存memory: " + memory);
//最大分配内存获取方法2
        float maxMemory = (float) (Runtime.getRuntime().maxMemory() * 1.0 / (1024 * 1024));
        System.out.println("app可分配最大内存 maxMemory: " + maxMemory);
//当前分配的总内存
        float totalMemory = (float) (Runtime.getRuntime().totalMemory() * 1.0 / (1024 * 1024));
        System.out.println("app当前分配内存totalMemory: " + totalMemory);
//当前分配使用剩余内存
        float freeMemory = (float) (Runtime.getRuntime().freeMemory() * 1.0 / (1024 * 1024));
        System.out.println("app当前分配剩余内存freeMemory: " + freeMemory);
    }

    //查看当前线程EGL情况
    public static void logCurrent(String msg) {
        EGLDisplay display;
        EGLContext context;
        EGLSurface surface;

        display = EGL14.eglGetCurrentDisplay();
        context = EGL14.eglGetCurrentContext();
        surface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        Log.i(Config.TAG_SHADER, "Current EGL (" + msg + "): display=" + display + ", context=" + context +
                ", surface=" + surface);
    }


    /**
     * Capture Window to Bitmap
     * The texture size must equal to config's texture size
     * @param fileName
     */
    public static void saveWindow2Bitmap(int tWidth,int tHeight,String fileName) {

        ByteBuffer rgbaBuf = ByteBuffer.allocateDirect(tWidth * tHeight * 4);
        rgbaBuf.position(0);
        GLES32.glReadPixels(0, 0, tWidth, tHeight, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, rgbaBuf);

        checkError(" GLES32.glReadPixels");

        BufferedOutputStream bos = null;
        try {
            String prePath=PathCfg.getPathApp()+"/capture/";
            File path = new File(prePath);
            if (!path.exists()) {
                path.mkdirs();
                if(DebugCfg.DEBUG_MEDIA_SOURCE){
                    Log.d(Config.TAG_SHADER, "mkdir " + prePath);
                }
            }
            Log.d(Config.TAG_SHADER,"saveBitmap savePath "+prePath+fileName+".png");
            bos = new BufferedOutputStream(new FileOutputStream(prePath+fileName+".png"));
            Bitmap bmp = Bitmap.createBitmap(tWidth, tHeight, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(rgbaBuf);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bmp.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.flush();
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void loadTexture(int texId, Bitmap bitmap) {
        if (texId== 0) {
            Log.e(Config.TAG_SHADER, "Could not generate a new OpenGL textureId object.");
        }
//        final BitmapFactory.Options options = new BitmapFactory.Options();
//        //这里需要加载原图未经缩放的数据
//        options.inScaled = false;
//        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
        if (bitmap == null) {
            Log.e(Config.TAG_SHADER, "bitmap 加载失败");
        }
        // 绑定纹理到OpenGL
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId);

        //设置默认的纹理过滤参数
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

        // 加载bitmap到纹理中
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

        // 生成MIP贴图
//        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);

        // 数据如果已经被加载进OpenGL,则可以回收该bitmap
        bitmap.recycle();

        // 取消绑定纹理
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    /**
     * Capture texture to Bitmap
     * The texture size must equal to config's texture size
     * @param textureId
     * @param fileName
     */
    public static void saveTexture2Image(int textureId,int tWidth,int tHeight,String fileName) {
        int[] frameBuffer = new int[1];
        GLES32.glGenFramebuffers(1, frameBuffer, 0);

        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, frameBuffer[0]);
        GLES32.glFramebufferTexture2D(GLES32.GL_FRAMEBUFFER, GLES32.GL_COLOR_ATTACHMENT0, GLES32.GL_TEXTURE_2D, textureId, 0);

        GLES32.glDrawBuffers(1, new int[]{GLES32.GL_COLOR_ATTACHMENT0}, 0);

        if (GLES32.glCheckFramebufferStatus(GLES32.GL_FRAMEBUFFER) != GLES32.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(Config.TAG_SHADER, "framebuffer not complete");
            return;
        }
        saveWindow2Bitmap(tWidth,tHeight,fileName);
        GLES32.glDeleteFramebuffers(1, IntBuffer.wrap(frameBuffer));
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0);
        ShaderUtils.checkError("saveTextureToImage");
    }

    public static void checkError(String msg) {
        Log.e(Config.TAG_SHADER,msg + " -- error --> " + GLES30.glGetError());
    }

}
