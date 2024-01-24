package com.mc2.block;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.mc2.Utils.PathCfg;
import com.mc2.Utils.Config;
import com.mc2.block.Render.GLSurfaceViewEGL14;


public class MainActivity extends AppCompatActivity {
    private GLSurfaceViewEGL14 glSurfaceView;
    private BLOCK mBLOCK;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //调用hide()方法将标题栏隐藏起来
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.hide();
        }
        //全屏显示，隐藏状态栏
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED); //<gsl_ldd_control:553>: ioctl fd 95 code 0xc0300945 (IOCTL_KGSL_GPUOBJ_ALLOC) failed: errno 12 Out of memory
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PathCfg.setPath(this.getFilesDir().toString());
        setupDir();
        setupDirectories();
//        setupDirectories();
        Context context=this.getApplicationContext();
        glSurfaceView = findViewById(R.id.glSurface_view);
        mBLOCK =new BLOCK(PathCfg.getPathApp(),context,glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(3);
        glSurfaceView.setRenderer(mBLOCK);

//        glSurfaceView.setLayerType(View.LAYER_TYPE_SOFTWARE, null); //<gsl_ldd_control:553>: ioctl fd 95 code 0xc0300945 (IOCTL_KGSL_GPUOBJ_ALLOC) failed: errno 12 Out of memory
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        Log.d(Config.TAG_BLOCK,"hopping it just create one time");

        // for testing
//        TestDecoder decoder=new TestDecoder(PathCfg.getPathApp()+"/init.m4s",PathCfg.getPathApp()+"/stream.m4s",this);
    }


    private void setupDir(){
        File videoDir = new File(PathCfg.getPathApp(), "video");
        videoDir.mkdir();
    }

    private void setupDirectories() {
        //Make inner directory structures
        File init = new File(PathCfg.getPathApp(), "/video/init.m4s");
        File stream = new File(PathCfg.getPathApp(), "stream.m4s");

        try {
            InputStream initFile = getResources().openRawResource(R.raw.init);
            OutputStream modelOutputStream = new FileOutputStream(init);
            byte[] data = new byte[initFile.available()];
            initFile.read(data);
            modelOutputStream.write(data);
            initFile.close();
            modelOutputStream.close();

            InputStream videoInputStream = getResources().openRawResource(R.raw.stream0);
            OutputStream videoOutputStream = new FileOutputStream(stream);
            byte[] videoData = new byte[videoInputStream.available()];
            videoInputStream.read(videoData);
            videoOutputStream.write(videoData);
            videoInputStream.close();
            videoOutputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}