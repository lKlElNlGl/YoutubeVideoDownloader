package com.lklengl.youtubevideodownloader;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** User-facing queue policy. No Android dependencies: easy to test. */
public final class QueuePolicy {
    private QueuePolicy(){}
    public static boolean needsCancellation(String status){return DownloadQueue.RUNNING.equals(status);}
    public static String videoId(String address){
        if(address==null)return "";
        try{
            URI uri=new URI(address.trim());
            String host=uri.getHost();
            if(host==null)return address.trim();
            host=host.toLowerCase(Locale.ROOT);
            String path=uri.getPath()==null?"":uri.getPath();
            if(host.equals("youtu.be"))return path.replaceFirst("^/","").split("/")[0];
            if(!host.equals("youtube.com")&&!host.endsWith(".youtube.com"))return address.trim();
            if(path.startsWith("/shorts/")||path.startsWith("/live/"))
                return path.split("/").length>2?path.split("/")[2]:"";
            if(path.equals("/watch")){
                String query=uri.getRawQuery();
                if(query!=null)for(String pair:query.split("&")){
                    String[] bits=pair.split("=",2);
                    if(bits.length==2&&bits[0].equals("v"))
                        return URLDecoder.decode(bits[1],StandardCharsets.UTF_8.name());
                }
            }
        }catch(Exception ignored){}
        return address.trim();
    }
    public static boolean hasActiveDuplicate(List<DownloadQueue.Item> jobs,String link){
        String target=videoId(link);
        if(target.isEmpty())return false;
        for(DownloadQueue.Item i:jobs)
            if((DownloadQueue.WAITING.equals(i.status)||DownloadQueue.RUNNING.equals(i.status))
                &&!i.deleteRequested&&target.equals(videoId(i.url)))return true;
        return false;
    }
    public static String errorSummary(String message){
        if(message==null||message.trim().isEmpty())return "Ошибка загрузки";
        String lower=message.toLowerCase(Locale.ROOT);
        for(String status:new String[]{"403","404","429","401","500","503"})
            if(lower.contains("http error "+status)||lower.contains("http "+status))
                return "Ошибка загрузки: HTTP "+status;
        if(lower.contains("ssl")||lower.contains("certificate"))return "Ошибка сети (SSL)";
        if(lower.contains("timed out")||lower.contains("timeout"))return "Превышено время ожидания";
        String[] lines=message.trim().split("\\r?\\n");
        String last=lines[lines.length-1].trim().replaceFirst("^(ERROR:|Ошибка:)\\s*","");
        if(last.isEmpty())return "Ошибка загрузки";
        return last.length()>110?last.substring(0,107)+"…":last;
    }
}
