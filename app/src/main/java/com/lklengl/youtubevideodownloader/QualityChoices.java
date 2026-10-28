package com.lklengl.youtubevideodownloader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/** Quality choices from a video's real yt-dlp heights, or the familiar fallback list. */
public final class QualityChoices {
    private final List<String> values;
    private final List<String> labels;
    private QualityChoices(List<String> values,List<String> labels){
        this.values=Collections.unmodifiableList(values);
        this.labels=Collections.unmodifiableList(labels);
    }
    public static QualityChoices fallback(){
        List<String> codes=new ArrayList<>(),names=new ArrayList<>();
        for(String q:DownloadOptions.QUALITIES){
            codes.add(q);names.add("best".equals(q)?"Максимальное":q+"p");
        }
        return new QualityChoices(codes,names);
    }
    public static QualityChoices fromDetected(String metadata){
        if(metadata==null||metadata.isEmpty())return fallback();
        TreeSet<Integer> heights=new TreeSet<>(Collections.reverseOrder());
        for(String raw:metadata.split(",")){
            String s=raw.trim().toLowerCase(java.util.Locale.ROOT);
            if(!s.matches("[0-9]{3,4}p"))continue;
            String num=s.substring(0,s.length()-1);
            if(DownloadOptions.isQuality(num))heights.add(Integer.parseInt(num));
        }
        if(heights.isEmpty())return fallback();
        List<String> values=new ArrayList<>(Arrays.asList("best"));
        List<String> labels=new ArrayList<>(Arrays.asList("Максимальное"));
        for(int height:heights){values.add(String.valueOf(height));labels.add(height+"p");}
        return new QualityChoices(values,labels);
    }
    public List<String> values(){return values;}
    public List<String> labels(){return labels;}
}
