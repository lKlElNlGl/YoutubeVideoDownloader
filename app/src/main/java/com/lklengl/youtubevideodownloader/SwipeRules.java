package com.lklengl.youtubevideodownloader;
/** Pure gesture policy: horizontal movement must outweigh vertical scrolling. */
public final class SwipeRules {
    private SwipeRules(){}
    public static int target(int current,float dx,float dy,float threshold){
        if(Math.abs(dx)<threshold || Math.abs(dx)<Math.abs(dy)*1.5f)return current;
        return Math.max(0,Math.min(2,current+(dx<0?1:-1)));
    }
}
