package com.lklengl.youtubevideodownloader;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.TextView;
/** Automatic marquee for the active title; only taps select rows. */
public final class TitleMarquee {
 private TitleMarquee(){}
 public static void show(TextView title,boolean active){
  title.setSingleLine(true);title.setHorizontallyScrolling(true);
  title.setEllipsize(active?TextUtils.TruncateAt.MARQUEE:TextUtils.TruncateAt.END);
  title.setMarqueeRepeatLimit(-1);title.setSelected(active);
  if(!active)title.scrollTo(0,0);
 }
 public static void bind(TextView title,Runnable select){
  title.setOnTouchListener(null);
  title.setOnClickListener(v->select.run());
 }
}
