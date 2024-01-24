package com.mc2.Utils;

import android.util.Log;

import java.util.LinkedList;
import java.util.ListIterator;

public class CheckUtils {
    /**
     *  Call this function in the way that you want to check.
     *  If the object is null, the tracer will get the position of source code which is the place you Call this.
     * @param object
     */
    public static void checkObjIsNull(Object object){
        if(object==null){
            throw new IllegalArgumentException("object is null!");
        }
    }

    @Deprecated
    private void listLearn(){
        LinkedList<Integer> list=new LinkedList<>();

        list.add(1000);
        list.add(2000);
        list.add(4000);

        ListIterator<Integer> iterator =list.listIterator(list.size()); // size has O(1) complexity，the iterator set behind the last e

        Log.d(Config.TAG_UTILS,"0 "+ iterator.hasPrevious()+" "+iterator.hasNext());  // 0 true false
        // 1000 2000 4000 |
        Log.d(Config.TAG_UTILS,"1 "+ iterator.previousIndex()); // 1 2

        Log.d(Config.TAG_UTILS,"2 "+ iterator.previous());  // 2 4000
        // 1000 2000 | 4000

        Log.d(Config.TAG_UTILS,"3 "+ iterator.previousIndex()); // 3 1

        iterator.add(3000);

        // 1000 2000 3000 | 4000
        Log.d(Config.TAG_UTILS,"4 "+ iterator.previousIndex()); // 4 2

    }
}
