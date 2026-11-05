package com.lklengl.youtubevideodownloader;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.TextView;
/** One title can auto-marquee, yet drag horizontally while a finger is held down. */
public final class TitleMarquee {
 private TitleMarquee(){}
 public static void show(TextView title,boolean active){
  title.setSingleLine(true);title.setHorizontallyScrolling(true);
  title.setEllipsize(active?TextUtils.TruncateAt.MARQUEE:TextUtils.TruncateAt.END);
  title.setMarqueeRepeatLimit(-1);title.setSelected(active);
  if(!active)title.scrollTo(0,0);
 }
 public static void bind(TextView title,Runnable select){
  title.setOnTouchListener(new android.view.View.OnTouchListener(){
   float start;int initial;boolean dragged;
   @Override public boolean onTouch(android.view.View v,MotionEvent event){
    int action=event.getActionMasked();
    if(action==MotionEvent.ACTION_DOWN){
     start=event.getX();initial=title.getScrollX();dragged=false;
     select.run();title.setSelected(false);return true;
    }
    if(action==MotionEvent.ACTION_MOVE){
     int distance=Math.round(start-event.getX());
     if(Math.abs(distance)>ThemeStyle.dp(title.getContext(),3))dragged=true;
     if(dragged){
      float full=title.getPaint().measureText(title.getText().toString());
      int max=Math.max(0,(int)Math.ceil(full)-title.getWidth()+title.getCompoundPaddingLeft()+title.getCompoundPaddingRight());
      title.scrollTo(Math.max(0,Math.min(max,initial+distance)),0);
     }
     return true;
    }
    if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
     title.postDelayed(()->title.setSelected(true),200);
     return true;
    }
    return true;
   }
  });
 }
}
