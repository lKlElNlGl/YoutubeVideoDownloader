package com.lklengl.youtubevideodownloader;

/** Immutable preview data; survives a view rebuild without relying on TextView contents. */
public final class PreviewSnapshot {
    public final String source, title, duration, resolutions, formats;
    public final boolean playlist;
    public final int videoCount;
    private PreviewSnapshot(String source,String title,String duration,String resolutions,String formats,boolean playlist,int count){
        this.source=source;this.title=title;this.duration=duration;this.resolutions=resolutions;this.formats=formats;
        this.playlist=playlist;this.videoCount=count;
    }
    public static PreviewSnapshot video(String source,String title,String duration,String resolutions){
        return video(source,title,duration,resolutions,"");
    }
    public static PreviewSnapshot video(String source,String title,String duration,String resolutions,String formats){
        return new PreviewSnapshot(source,title,duration,resolutions,formats,false,0);
    }
    public static PreviewSnapshot playlist(String source,String title,int count){
        return new PreviewSnapshot(source,title,"","","",true,count);
    }
    public boolean matches(String link){return link!=null&&!link.isEmpty()&&source.equals(link.trim());}
}
