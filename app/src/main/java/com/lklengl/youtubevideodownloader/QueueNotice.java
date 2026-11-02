package com.lklengl.youtubevideodownloader;
public final class QueueNotice {
 private QueueNotice(){}
 private static android.widget.Toast active;
 public static void show(android.app.Activity context,String message){
  if(active!=null)active.cancel();
  android.widget.LinearLayout panel=new android.widget.LinearLayout(context);
  panel.setOrientation(android.widget.LinearLayout.HORIZONTAL);
  panel.setGravity(android.view.Gravity.CENTER_VERTICAL);
  int d=(int)(context.getResources().getDisplayMetrics().density+0.5f);
  panel.setPadding(15*d,11*d,15*d,11*d);
  panel.setBackground(ThemeStyle.rounded(ThemeStyle.cardColor(context),ThemeStyle.border(context),12*d));
  android.widget.ImageView check=new android.widget.ImageView(context);
  check.setImageDrawable(new UiIcons("check",0xFF40BA7C,20*d));
  panel.addView(check,new android.widget.LinearLayout.LayoutParams(23*d,23*d));
  android.widget.TextView label=new android.widget.TextView(context);
  label.setText(UiStrings.display(context,message));label.setTextColor(ThemeStyle.text(context));label.setTextSize(13);
  android.widget.LinearLayout.LayoutParams lp=new android.widget.LinearLayout.LayoutParams(-2,-2);
  lp.setMargins(9*d,0,0,0);panel.addView(label,lp);
  android.widget.Toast toast=new android.widget.Toast(context);
  toast.setView(panel);toast.setGravity(android.view.Gravity.BOTTOM|android.view.Gravity.CENTER_HORIZONTAL,0,65*d);
  toast.setDuration(android.widget.Toast.LENGTH_LONG);active=toast;toast.show();
 }
 public static String message(int count,boolean priority){
  if(count!=1)return "Добавлено в очередь: "+count+" видео";
  return priority?"Приоритетная загрузка добавлена":"Добавлено в очередь";
 }
}