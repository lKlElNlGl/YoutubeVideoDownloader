package com.lklengl.youtubevideodownloader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** File formats derived from actual yt-dlp streams. MP3 is available by FFmpeg conversion. */
public final class FormatChoices {
    private final List<String> values;
    private final List<String> labels;
    private FormatChoices(List<String> values,List<String> labels){
        this.values=Collections.unmodifiableList(values);
        this.labels=Collections.unmodifiableList(labels);
    }
    public static FormatChoices fallback(){
        return new FormatChoices(new ArrayList<>(Arrays.asList("mp4","mp3")),
            new ArrayList<>(Arrays.asList("Видео MP4","Аудио MP3 · 192 кбит/с")));
    }
    public static FormatChoices fromDetected(String metadata){
        if(metadata==null||metadata.trim().isEmpty())return fallback();
        String x=","+metadata.toLowerCase(Locale.ROOT).replace(" ","")+",";
        boolean mp4=x.contains(",mp4,");
        boolean webm=x.contains(",webm,");
        boolean m4a=x.contains(",m4a,");
        ArrayList<String> values=new ArrayList<>(),labels=new ArrayList<>();
        values.add("mp4");labels.add("Видео MP4");
        if(webm){values.add("webm");labels.add("Видео WebM");}
        if(mp4||webm){values.add("mkv");labels.add("Видео MKV");}
        values.add("mp3");labels.add("Аудио MP3 · 192 кбит/с");
        if(m4a||mp4){values.add("m4a");labels.add("Аудио M4A");}
        return new FormatChoices(values,labels);
    }
    public List<String> values(){return values;}
    public List<String> labels(){return labels;}
    public boolean isAudio(String format){return "mp3".equals(format)||"m4a".equals(format);}
}
