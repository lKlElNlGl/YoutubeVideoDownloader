package com.lklengl.youtubevideodownloader;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

/** Non-blocking five-second refresh result, independent of navigation. */
public final class RefreshNotice {
 private RefreshNotice(){}
 public static void show(Activity activity,boolean success){
  if(activity.isFinishing()||activity.isDestroyed())return;
  FrameLayout host=activity.findViewById(android.R.id.content);
  if(host==null)return;
  int d=ThemeStyle.dp(activity,1);
  LinearLayout panel=new LinearLayout(activity);
  panel.setGravity(Gravity.CENTER_VERTICAL);
  panel.setPadding(14*d,11*d,14*d,11*d);
  panel.setBackground(ThemeStyle.rounded(ThemeStyle.buttonFill(activity),ThemeStyle.buttonStroke(activity),12*d));
  panel.setElevation(ThemeStyle.dp(activity,10));
  ImageView icon=new ImageView(activity);
  icon.setImageDrawable(new UiIcons(success?"check":"info",
    success?0xFF42C986:ThemeStyle.RED,20*d));
  panel.addView(icon,new LinearLayout.LayoutParams(22*d,22*d));
  TextView message=new TextView(activity);
  message.setText(UiStrings.display(activity,success?
    "Информация о плейлисте обновлена":"Не удалось обновить информацию о плейлисте"));
  message.setTextColor(ThemeStyle.text(activity));message.setTextSize(14);
  LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(0,-2,1);
  labelParams.setMargins(10*d,0,0,0);panel.addView(message,labelParams);
  FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
  p.setMargins(12*d,0,12*d,75*d);
  host.addView(panel,p);
  panel.setAlpha(0f);panel.setTranslationY(12*d);
  panel.animate().alpha(1f).translationY(0).setDuration(180).start();
  new Handler(Looper.getMainLooper()).postDelayed(()->{
   if(panel.getParent()!=null)panel.animate().alpha(0f).setDuration(180)
    .withEndAction(()->{
     if(panel.getParent() instanceof ViewGroup)((ViewGroup)panel.getParent()).removeView(panel);
    }).start();
  },5000);
 }
}
