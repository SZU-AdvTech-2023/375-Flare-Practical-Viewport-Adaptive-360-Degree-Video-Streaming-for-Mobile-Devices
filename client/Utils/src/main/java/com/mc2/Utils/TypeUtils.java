package com.mc2.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

//
//import com.unity3d.player.UnityPlayer;
//
public class TypeUtils<T> {

//    public static void updateTexture(){
//        Log.d(Config.TAG_UTILS,"UpdateTexture");
//        UnityPlayer.UnitySendMessage("Canvas","UpdateTexture","");
//    }

    public static String getStr(HashMap vMap){
        StringBuilder viewMicroStr= new StringBuilder("");
        int viewMicroMapLen=vMap.keySet().size();
        int vMMIdx=0;
        for(Object t:vMap.keySet()){
            viewMicroStr.append(t);
            if(vMMIdx<viewMicroMapLen-1){
                viewMicroStr.append("|");
            }
            vMMIdx++;
        }
        return viewMicroStr.toString();
    }

    public static String getStr(List<Integer> list){
        StringBuilder viewMicroStr= new StringBuilder("");
        int viewMicroMapLen=list.size();
        int vMMIdx=0;
        for(Object t:list){
            viewMicroStr.append(t);
            if(vMMIdx<viewMicroMapLen-1){
                viewMicroStr.append("|");
            }
            vMMIdx++;
        }
        return viewMicroStr.toString();
    }

    public static int[]  convertList(List<Integer> list){
        int[] res= new int[list.size()];
        int i=0;
        for(int t:list){
            res[i]=t;
            i++;
        }
        return res;
    }


//    public static Tile[]  convertTList(List<Tile> list){
//        Tile[] res= new Tile[list.size()];
//        int i=0;
//        for(Tile t:list){
//            res[i]=t;
//            i++;
//        }
//        return res;
//    }


//    public static List<Tile> convertArray(Tile[] list){
//        LinkedList<Tile> res=new LinkedList<Tile>();
//        for(int i=0;i<list.length;i++){
//            res.add(list[i]);
//        }
//        return res;
//    }

    public static int[] convertKeySetToArry(Set<Integer> set){
        int i=0;
        int[] res=new int[set.size()];
        for (Integer t : set) {
            res[i]=t;
            i++;
        }
        return res;
    }

    public static List<Integer> convertKeySetToList(Set<Integer> set){
        List<Integer> res=new LinkedList<Integer>();
        for (Integer t : set) {
            res.add(t);
        }
        return res;
    }


}
