package com.lklengl.youtubevideodownloader;
/** Show the default-folder action only when a custom directory is in use. */
public final class FolderRules {
    private FolderRules(){}
    public static boolean isCustom(String uri){return uri!=null&&!uri.isEmpty();}
}
