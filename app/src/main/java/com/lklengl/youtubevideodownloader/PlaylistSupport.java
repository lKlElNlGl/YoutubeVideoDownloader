package com.lklengl.youtubevideodownloader;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class PlaylistSupport {
    private PlaylistSupport() {}
    public static boolean isPlaylist(String value) {
        String url;
        try { url=DownloadOptions.validateUrl(value); } catch(Exception e){return false;}
        try {
            URI parsed=new URI(url);
            String query=parsed.getRawQuery();
            if(query==null)return false;
            for(String param:query.split("&")) {
                int equals=param.indexOf('=');
                if(equals<=0)continue;
                String name=URLDecoder.decode(param.substring(0,equals), StandardCharsets.UTF_8.name());
                if("list".equals(name) && equals<param.length()-1)return true;
            }
        }catch(Exception ignored){}
        return false;
    }
    public static String videoUrl(String videoId) {
        if(videoId==null || !videoId.matches("[A-Za-z0-9_-]{5,64}"))
            throw new IllegalArgumentException("Недопустимый идентификатор видео");
        return "https://www.youtube.com/watch?v="+videoId;
    }
    public static String selectionCount(int selected,int total) {
        return selected+" из "+total;
    }
}
