package com.lklengl.youtubevideodownloader;

import java.util.List;

/** Small policy class kept independent from Android APIs for fast unit tests. */
public final class QueueRules {
    private QueueRules(){}
    public static int activeCount(List<DownloadQueue.Item> list) {
        int count=0;
        for(DownloadQueue.Item item:list) {
            if(DownloadQueue.WAITING.equals(item.status) || DownloadQueue.RUNNING.equals(item.status))count++;
        }
        return count;
    }
    public static String filenameTemplate() {return DownloadOptions.OUTPUT_TEMPLATE;}
    public static String readableDuration(int seconds) {
        if(seconds<0)return "—";
        if(seconds>=3600)return String.format(java.util.Locale.ROOT,"%d:%02d:%02d",seconds/3600,(seconds%3600)/60,seconds%60);
        return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);
    }
}
