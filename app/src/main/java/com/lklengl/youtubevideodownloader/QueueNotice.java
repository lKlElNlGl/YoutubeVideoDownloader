package com.lklengl.youtubevideodownloader;
public final class QueueNotice {
 private QueueNotice(){}
 private static java.lang.ref.WeakReference<android.view.View> active=new java.lang.ref.WeakReference<>(null);
 /** In-app overlay is used instead of a system Toast (Android 12+ changes custom Toast rendering). */
 public static void show(android.app.Activity context,String message){
  android.widget.FrameLayout host=context.findViewById(android.R.id.content);
  if(host==null || context.isFinishing())return;
  android.view.View previous=active.get();
  if(previous!=null && previous.getParent() instanceof android.view.ViewGroup)
   ((android.view.ViewGroup)previous.getParent()).removeView(previous);
  android.widget.LinearLayout panel=new android.widget.LinearLayout(context);
  panel.setOrientation(android.widget.LinearLayout.HORIZONTAL);
  panel.setGravity(android.view.Gravity.CENTER_VERTICAL);
  int d=ThemeStyle.dp(context,1);
  panel.setPadding(15*d,12*d,15*d,12*d);
  panel.setBackground(ThemeStyle.rounded(ThemeStyle.cardColor(context),ThemeStyle.border(context),12*d));
  panel.setElevation(ThemeStyle.dp(context,8));
  android.widget.ImageView check=new android.widget.ImageView(context);
  check.setImageDrawable(new UiIcons("check",0xFF42C986,20*d));
  panel.addView(check,new android.widget.LinearLayout.LayoutParams(23*d,23*d));
  android.widget.TextView label=new android.widget.TextView(context);
  label.setText(UiStrings.display(context,message));label.setTextColor(ThemeStyle.text(context));
  label.setTextSize(13);
  android.widget.LinearLayout.LayoutParams lp=new android.widget.LinearLayout.LayoutParams(-2,-2);
  lp.setMargins(9*d,0,0,0);panel.addView(label,lp);
  android.widget.FrameLayout.LayoutParams layout=new android.widget.FrameLayout.LayoutParams(
   -2,-2,android.view.Gravity.BOTTOM|android.view.Gravity.CENTER_HORIZONTAL);
  layout.setMargins(12*d,0,12*d,75*d);
  host.addView(panel,layout);active=new java.lang.ref.WeakReference<>(panel);
  panel.setAlpha(0f);panel.setTranslationY(16*d);
  panel.animate().alpha(1f).translationY(0).setDuration(180).start();
  new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{
   if(panel.getParent() instanceof android.view.ViewGroup)
    panel.animate().alpha(0f).setDuration(180).withEndAction(()->{
     if(panel.getParent() instanceof android.view.ViewGroup)
      ((android.view.ViewGroup)panel.getParent()).removeView(panel);
    }).start();
  },3000);
 }
 public static String message(int count,boolean priority){
  if(count!=1)return "Добавлено в очередь: "+count+" видео";
  return priority?"Приоритетная загрузка добавлена":"Добавлено в очередь";
 }
}