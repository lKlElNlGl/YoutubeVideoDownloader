package com.lklengl.youtubevideodownloader;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extract real transfer metrics from yt-dlp output; never fabricate totals or speed. */
public final class DownloadTelemetry {
    private static final Pattern SIZE=Pattern.compile("(?i)\\bof\\s+([0-9.,]+)\\s*(KiB|MiB|GiB|KB|MB|GB)\\b");
    private static final Pattern SPEED=Pattern.compile("(?i)\\bat\\s+([0-9.,]+)\\s*(KiB|MiB|GiB|KB|MB|GB)/s\\b");
    private DownloadTelemetry(){}
    private static double factor(String unit){
        switch(unit.toLowerCase(Locale.ROOT)){
            case "gib":return 1024d*1024*1024;
            case "mib":return 1024d*1024;
            case "kib":return 1024d;
            case "gb":return 1000d*1000*1000;
            case "mb":return 1000d*1000;
            default:return 1000d;
        }
    }
    private static String readable(double value){
        if(value>=1e9)return String.format(new Locale("ru"),"%.1f ГБ",value/1e9);
        if(value>=1e6)return String.format(new Locale("ru"),"%.1f МБ",value/1e6);
        return String.format(new Locale("ru"),"%.0f КБ",value/1000);
    }
    public static String summary(String line,int percent){
        if(line==null||line.isEmpty())return "";
        Matcher total=SIZE.matcher(line),speed=SPEED.matcher(line);
        StringBuilder out=new StringBuilder();
        if(total.find()){
            try{
                double size=Double.parseDouble(total.group(1).replace(',','.'))*factor(total.group(2));
                if(size>0 && size<1e13)out.append(readable(size*Math.min(100,Math.max(0,percent))/100.0))
                    .append(" из ").append(readable(size));
            }catch(NumberFormatException ignored){}
        }
        if(speed.find()){
            try{
                double rate=Double.parseDouble(speed.group(1).replace(',','.'))*factor(speed.group(2));
                if(rate>0 && rate<1e12){
                    if(out.length()>0)out.append(" · ");
                    out.append(readable(rate)).append("/с");
                }
            }catch(NumberFormatException ignored){}
        }
        return out.toString();
    }
}
