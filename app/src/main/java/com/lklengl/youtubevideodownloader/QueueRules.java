package com.lklengl.youtubevideodownloader;

import java.util.List;

/** Small policy class kept independent from Android APIs for fast unit tests. */
public final class QueueRules {
    private QueueRules(){}
    public static int activeCount(List<DownloadQueue.Item> list) {
        int count=0;
        for(DownloadQueue.Item item:list) {
            if(DownloadQueue.WAITING.equals(item.status) || DownloadQueue.RUNNING.equals(item.status) || DownloadQueue.PAUSED.equals(item.status))count++;
        }
        return count;
    }
    /** Insert urgent jobs before waiting jobs without interrupting a currently active download. */
    public static void prependPriority(List<DownloadQueue.Item> items,List<DownloadQueue.Item> urgent) {
        DownloadQueue.Item running=null;
        for(DownloadQueue.Item item:items)if(DownloadQueue.RUNNING.equals(item.status)){running=item;break;}
        if(running!=null){items.remove(running);items.add(0,running);}
        items.addAll(running==null?0:1,urgent);
    }
    public static DownloadQueue.Item nextWaiting(List<DownloadQueue.Item> items) {
        for(DownloadQueue.Item item:items)
            if(DownloadQueue.WAITING.equals(item.status))return item;
        return null;
    }
    public static String filenameTemplate() {return DownloadOptions.OUTPUT_TEMPLATE;}
    public static String readableDuration(int seconds) {
        if(seconds<0)return "—";
        if(seconds>=3600)return String.format(java.util.Locale.ROOT,"%d:%02d:%02d",seconds/3600,(seconds%3600)/60,seconds%60);
        return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);
    }
}
