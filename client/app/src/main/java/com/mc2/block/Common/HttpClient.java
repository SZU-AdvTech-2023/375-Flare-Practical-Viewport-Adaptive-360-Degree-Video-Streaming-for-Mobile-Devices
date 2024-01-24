package com.mc2.block.Common;

import android.util.Log;
import com.mc2.Utils.Chunk;
import com.mc2.Utils.Config;
import com.mc2.Utils.Tile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpClient {
    private OkHttpClient client;
    // 创建一个ExecutorService实例
    private ExecutorService executorService;
    public HttpClient(){
        client=new OkHttpClient();
        executorService = Executors.newCachedThreadPool();
    }


    public void reqMpd(){
        // 创建一个HttpUrl对象，设置请求的url和参数
        HttpUrl reqUrl = new HttpUrl.Builder()
                .scheme("http")
                .host(Config.SERVER_ADDRESS)
                .port(Config.SERVER_PORT)
                .addPathSegment("mpd")
                .build();
//        Log.d(Config.TAG_HTTP,"url "+reqUrl);
        Request request = new Request.Builder()
                .url(reqUrl)
//                .header("Connection", "Upgrade") // 指定协议升级
//                .header("Upgrade", "h2c") // 指定连接类型为HTTP/2 ClearText
                .build();

        try (Response response = client.newCall(request).execute()) {
            Protocol protocol = response.protocol();
            Log.d(Config.TAG_HTTP,"response protocol "+protocol.toString());
            if (response.isSuccessful()) {
                // 获取响应体
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String mpd = responseBody.string();
                    Log.d(Config.TAG_HTTP,"mpd: "+mpd);
                }
            } else {
                // 响应失败，打印状态码和信息
                System.out.println("code: " + response.code());
                System.out.println("message: " + response.message());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    public int reqChunk(Chunk chunk){
//        // 创建一个HttpUrl对象，设置请求的url和参数
//        int size=0;
//        for(Tile t:chunk.microList){
//            size+=reqTile(Config.VIDEO_NAME,chunk.id,t.resolution,t.rowLen, t.colLen,t.idx,t.videoPath);
//        }
//        return size;
//    }

    public int reqChunk(Chunk chunk) {
        int size = 0;
        // 使用Future来跟踪异步任务的结果
        List<Future<Integer>> futureList = new ArrayList<>();

        for (Tile t : chunk.microList) {
            // 提交任务到ExecutorService并获取Future对象
            Future<Integer> future = executorService.submit(() -> {
                return reqTile(Config.VIDEO_NAME, chunk.id, t.resolution, t.rowLen, t.colLen, t.idx, t.videoPath);
            });
            futureList.add(future);
        }

        // 等待所有任务完成并获取总大小
        for (Future<Integer> future : futureList) {
            try {
                size += future.get(); // 阻塞直到任务完成并返回结果
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return size;
    }

    private int reqTile(String videoName,int chunkId,int resolution,int rowLen,int colLen,int tIdx,String videoPath){
        int dataLen=0;
        HttpUrl reqUrl = new HttpUrl.Builder()
                .scheme("http")
                .host(Config.SERVER_ADDRESS)
                .port(Config.SERVER_PORT)
                .addPathSegment("video")
                .addQueryParameter("video_name",videoName) // video_id是一个Number类型的变量，表示视频id号
                .addQueryParameter("chunk_id", String.valueOf(chunkId)) // chunk_id是一个Number类型的变量，表示chunk id号
                .addQueryParameter("resolution", String.valueOf(resolution)) // resolution是一个Number类型的变量，表示分辨率
                .addQueryParameter("tiling_scheme", rowLen+","+colLen) // x_id是一个Number类型的变量，表示tile的row下标
                .addQueryParameter("tile_index", String.valueOf(tIdx)) // y_id是一个Number类型的变量，表示tile的column下标
                .build();

        Log.d(Config.TAG_HTTP,"reqTile "+reqUrl);
        Request request = new Request.Builder()
                .url(reqUrl)
//                .header("Connection", "Upgrade") // 指定协议升级
//                .header("Upgrade", "h2c") // 指定连接类型为HTTP/2 ClearText
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Protocol protocol = response.protocol();
                Log.d(Config.TAG_HTTP,"response protocol "+protocol.toString());
                // 获取响应体
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    // 获取响应的视频数据
                    byte[] data = responseBody.bytes();
                    dataLen=data.length;
                    // 保存或播放视频数据
                    File video = new File(videoPath);
                    OutputStream vS = new FileOutputStream(video);
                    vS.write(data);
                    vS.close();
                }
            } else {
                Log.e(Config.TAG_HTTP,"code: " + response.code());
                Log.e(Config.TAG_HTTP,"message: " + response.body());
            }
        } catch (IOException e) {
            Log.e(Config.TAG_HTTP,"error: "+e.toString());
            throw new RuntimeException(e);
        }

        return dataLen;
    }

}
