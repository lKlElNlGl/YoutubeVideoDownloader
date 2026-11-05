package com.lklengl.youtubevideodownloader;
public final class QueueNotice {
 private QueueNotice(){}
 private static java.lang.ref.WeakReference<android.view.View> active=new java.lang.ref.WeakReference<>(null);
 /** In-app overlay is used instead of a system Toast (Android 12+ changes custom Toast rendering). */
 public static void show(android.app.Activity context,String message){show(context,message,null);}
 public static void show(android.app.Activity context,String message,Runnable action){
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
  int fill=ThemeStyle.isDark(context)?Palette050.NIGHT_POPUP:ThemeStyle.buttonFill(context);
  int stroke=ThemeStyle.isDark(context)?Palette050.NIGHT_POPUP_BORDER:ThemeStyle.border(context);
  android.graphics.drawable.GradientDrawable surface=ThemeStyle.rounded(fill,stroke,12*d);
  surface.setStroke(ThemeStyle.dp(context,1),stroke);
  panel.setBackground(surface);
  panel.setElevation(ThemeStyle.dp(context,12));
  android.widget.ImageView check=new android.widget.ImageView(context);
  check.setImageDrawable(new UiIcons("check",0xFF42C986,20*d));
  panel.addView(check,new android.widget.LinearLayout.LayoutParams(23*d,23*d));
  android.widget.TextView label=new android.widget.TextView(context);
  label.setText(UiStrings.display(context,message));label.setTextColor(ThemeStyle.text(context));
  label.setTextSize(14);
  android.widget.LinearLayout.LayoutParams lp=new android.widget.LinearLayout.LayoutParams(
   action==null?-2:0,-2,action==null?0:1);
  lp.setMargins(9*d,0,0,0);panel.addView(label,lp);
  if(action!=null){
   android.widget.TextView jump=new android.widget.TextView(context);
   jump.setText(UiStrings.display(context,"В очередь")+" ›");
   jump.setTextSize(14);jump.setTypeface(null,android.graphics.Typeface.BOLD);
   jump.setTextColor(ThemeStyle.RED);
   jump.setPadding(12*d,7*d,3*d,7*d);
   panel.addView(jump);
   jump.setOnClickListener(v->{action.run();if(panel.getParent() instanceof android.view.ViewGroup)
    ((android.view.ViewGroup)panel.getParent()).removeView(panel);});
  }
  android.widget.FrameLayout.LayoutParams layout=new android.widget.FrameLayout.LayoutParams(
   action==null?-2:-1,-2,android.view.Gravity.BOTTOM|android.view.Gravity.CENTER_HORIZONTAL);
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
  },7000);
 }
 public static String message(int count,boolean priority){
  if(count!=1)return "Добавлено в очередь: "+count+" видео";
  return priority?"Приоритетная загрузка добавлена":"Добавлено в очередь";
 }
}