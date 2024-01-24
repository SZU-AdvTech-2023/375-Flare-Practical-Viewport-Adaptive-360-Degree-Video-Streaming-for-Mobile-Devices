package com.mc2.Utils;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

//线程安全的队列
//public  class SyncQueue<T> {
//    private ConcurrentLinkedQueue<T> mQueue;
//
//    /**
//     * 初始化队列
//     */
//    public SyncQueue(){
//        mQueue=new ConcurrentLinkedQueue<T>();
//    }
//
//    /**
//     * 是否为空
//     */
//    public boolean isEmpty(){
//        return mQueue.isEmpty();
//    }
//
//    public int size(){return mQueue.size();}
//
//    public Object[] toArray(){
//        return mQueue.toArray();
//    }
//
//    /** 入队  **/
//    public boolean offer(T object){
//        if(DebugCfg.CHECK_INPUT && DebugCfg.DEBUG_UTILS){
//            CheckUtils.checkObjIsNull(object);
//        }
//       return mQueue.offer(object);
//    }
//
//    public void clear(){
//        mQueue.clear();
//    }
//
//    /** 出队  若队伍为空则返回空 **/
//    public T poll(){
//        return mQueue.poll();
//    }
//
//}

//线程安全的队列
public  class SyncQueue<T> {
    private short head;     //-32768至32767
    private short end;
    private short maxSize = 80;
    private T[] array;  //array 的大小应该比maxSize大1
    private String TAG = "Queue";
    private T tmp;

    /**
     * 设定队列大小
     **/
    public SyncQueue() {
        array = (T[]) new Object[maxSize + 1];
        head = 0;
        end = 0;

    }

    /**
     * 是否为空
     **/
    public synchronized boolean isEmpty() {
        if (end == head)
            return true;
        else
            return false;
    }

    /**
     * 队列长度
     */
    public synchronized int size(){
        return (end - head + maxSize) % maxSize;
    }

    /**
     * 是否为满
     **/
    public synchronized boolean isFull() {
        if ((end + 1 + maxSize) % maxSize == head)
            return true;
        else
            return false;
    }


    /**
     * 入队
     **/
    public synchronized boolean offer(T input) {
        if ((end + 1 + maxSize) % maxSize == head) {
            Log.e(Config.TAG_UTILS,"SyncQueue offer out of len!");
            return false;
        } else {
            array[end] = input;
            end = (short) ((end + 1 + maxSize) % maxSize);
            return true;
        }
    }

    /**
     * 出队  会越界的！！！
     **/
    public synchronized T poll() {
        if (end == head) {
//            Log.d(TAG,"队伍为空");
            return null;

        } else {
            tmp = array[head];
            head = (short) ((head + 1 + maxSize) % maxSize);
            return tmp;
        }
    }

    /**
     * 把队列清空，返回一个数组
     **/
    public synchronized List<T> flush() {
        short len = (short) ((end - head + maxSize) % maxSize);
        List<T> list=new LinkedList<T>();
        if (len == 0) {
            return list;
        } else {
            for (int outIndex = 0; outIndex < len; outIndex++) {
                list.add(array[head]);
                head = (short) ((head + 1 + maxSize) % maxSize);
            }
            return list;
        }
    }


    /**
     * 清空队列
     */
    public synchronized void clear() {
        short len = (short) ((end - head + maxSize) % maxSize);
        for (int outIndex = 0; outIndex < len; outIndex++) {
            array[head]=null;
            head = (short) ((head + 1 + maxSize) % maxSize);
        }
    }

    /**
     * 用一个数组重置队列
     **/
    public synchronized boolean resetQue(short[] input) {
        short len = (short) input.length;
        if (len > maxSize) {
            Log.e(TAG, "too long len>maxSize" + len + " " + maxSize);
            return false;
        }
        head = 0;
        end = 0;
        for (int inputIndex = 0; inputIndex < len; inputIndex++) {
            offer(array[inputIndex]);
        }
        return true;
    }

}


