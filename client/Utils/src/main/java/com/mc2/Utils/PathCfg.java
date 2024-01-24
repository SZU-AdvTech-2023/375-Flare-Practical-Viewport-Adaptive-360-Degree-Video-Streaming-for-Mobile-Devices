package com.mc2.Utils;

import android.util.Log;

import java.io.File;

public class PathCfg {
    private static String PATH_APP;
    private static int PATH_APP_LEN;

    public static void setPath(String fileDirPath){
        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_UTILS){
            CheckUtils.checkObjIsNull(fileDirPath);
        }
        PATH_APP = fileDirPath;
        String[] folderSet=fileDirPath.split("/");
        PATH_APP_LEN=folderSet.length;
        if(DebugCfg.SHOW_FILE_PATH){
                Log.d(Config.TAG_UTILS,"DIR_APP "+fileDirPath);
        }

    }

    public static String getPathApp(){
        if(DebugCfg.CHECK_GLOBAL && DebugCfg.DEBUG_UTILS){
            CheckUtils.checkObjIsNull(PATH_APP);
        }
        return PATH_APP;
    }


    public static void initPrePth(String path){
        CheckUtils.checkObjIsNull(path);
        // 截取前缀文件夹
        String prePath;
        String[] splitedStr=path.split("/");
        splitedStr[splitedStr.length-1]="";
        prePath= String.join("/",splitedStr);

        File fPrePath = new File(prePath);
        if (!fPrePath.exists()) {
            fPrePath.mkdirs();
            if(DebugCfg.DEBUG_UTILS){
                Log.d(Config.TAG_UTILS, "mkdir " + prePath);
            }
        }
    }

    public static String getRelativePath(String path){
        CheckUtils.checkObjIsNull(path);
        int pLen=PATH_APP.length();
        if(path.length()<pLen){
            throw new IllegalArgumentException("getRelativePath error, error path:"+path);
        }
        return path.substring(pLen);
    }

    public static void checkFileExist(String path){
        CheckUtils.checkObjIsNull(path);
        File file = new File(path);
        if(!file.exists()){
            throw new IllegalArgumentException("checkFileExist error, file path:"+path);
        }
    }

}
