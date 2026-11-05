package com.lklengl.youtubevideodownloader;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Separate per-stream and whole-file progress for yt-dlp multi-stream downloads. */
public final class DownloadProgressTracker {
    private static final Pattern TOTAL=Pattern.compile("(?i)\\bof\\s+~?([0-9.,]+)\\s*(GiB|MiB|KiB|GB|MB|KB)\\b");
    public static final class Status {
        public final int streamPercent,totalPercent;
        public final String streamLabel;
        public final boolean estimated;
        Status(int stream,int total,String label,boolean approx){
            streamPercent=stream;totalPercent=total;streamLabel=label;estimated=approx;
        }
    }
    private final boolean audioOnly;
    private int phase=0,stream=0,overall=0;
    private double firstBytes=0,secondBytes=0;
    private boolean processing=false;
    public DownloadProgressTracker(boolean audioOnly){this.audioOnly=audioOnly;}
    private static double bytes(String line){
        if(line==null)return 0;
        Matcher m=TOTAL.matcher(line);
        if(!m.find())return 0;
        try {
            double n=Double.parseDouble(m.group(1).replace(",",""));
            String unit=m.group(2).toLowerCase(Locale.ROOT);
            if(unit.startsWith("gi"))return n*1073741824d;
            if(unit.startsWith("mi"))return n*1048576d;
            if(unit.startsWith("ki"))return n*1024d;
            if(unit.equals("gb"))return n*1000000000d;
            if(unit.equals("mb"))return n*1000000d;
            return n*1000d;
        } catch(NumberFormatException ex){return 0;}
    }
    public synchronized Status observe(int raw,String line){
        int p=Math.max(0,Math.min(100,raw));
        // yt-dlp resets its percentage when the next selected format begins.
        if(!audioOnly && phase==0 && stream>=90 && p+25<stream){
            phase=1;stream=0;
        }
        stream=Math.max(stream,p); // Never regress within a single media stream.
        double size=bytes(line);
        if(size>0){
            if(phase==0)firstBytes=Math.max(firstBytes,size);
            else secondBytes=Math.max(secondBytes,size);
        }
        int candidate;
        boolean estimated=!audioOnly && !(firstBytes>0&&secondBytes>0);
        if(audioOnly)candidate=stream;
        else if(phase==0)candidate=(int)Math.round(stream*.90);
        else if(firstBytes>0&&secondBytes>0)
            candidate=(int)Math.round(100*(firstBytes+secondBytes*stream/100d)/(firstBytes+secondBytes));
        else candidate=90+(int)Math.round(stream*.09);
        overall=Math.max(overall,Math.min(98,candidate));
        return new Status(stream,overall,audioOnly||phase==1?"Аудио":"Видео",estimated);
    }
    public synchronized Status processing(){
        processing=true;overall=Math.max(overall,99);
        return new Status(stream,overall,"Обработка",false);
    }
    public synchronized Status saving(){
        overall=Math.max(overall,99);
        return new Status(stream,overall,"Сохранение",false);
    }
}
