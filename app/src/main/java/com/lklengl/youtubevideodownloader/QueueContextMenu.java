package com.lklengl.youtubevideodownloader;

import android.app.Activity;
import android.view.*;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.Drawable;
import android.widget.*;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

/** One consistent menu for all queue actions, icon on every row, edge-to-edge danger divider. */
public final class QueueContextMenu {
 private QueueContextMenu(){}
 public static void show(Activity activity,View anchor,LinkedHashMap<String,String> actions,Consumer<String> chosen){
  int dp=ThemeStyle.dp(activity,1);
  LinearLayout content=new LinearLayout(activity);
  content.setOrientation(LinearLayout.VERTICAL);
  content.setPadding(0,dp,0,dp);
  content.setBackground(ThemeStyle.popupBackground(activity));
  PopupWindow popup=new PopupWindow(content,ThemeStyle.dp(activity,250),-2,true);
  popup.setBackgroundDrawable(ThemeStyle.popupBackground(activity));
  popup.setOutsideTouchable(true);popup.setElevation(ThemeStyle.dp(activity,8));
  popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
  int i=0;int count=actions.size();
  for(java.util.Map.Entry<String,String> action:actions.entrySet()){
   String label=action.getKey();
   boolean danger=label.equals("Удалить запись");
   if(QueueMenuPolicy.needsSeparator(count,i,danger)){
    View divider=new View(activity);
    divider.setBackgroundColor(ThemeStyle.isDark(activity)?0xFF697385:0xFFD4D8E0);
    LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(-1,ThemeStyle.dp(activity,1));
    line.setMargins(0,ThemeStyle.dp(activity,3),0,ThemeStyle.dp(activity,3));
    content.addView(divider,line);
   }
   LinearLayout row=new LinearLayout(activity);row.setOrientation(LinearLayout.HORIZONTAL);
   row.setGravity(Gravity.CENTER_VERTICAL);
   row.setPadding(ThemeStyle.dp(activity,12),0,ThemeStyle.dp(activity,12),0);
   int ink=danger?(ThemeStyle.isDark(activity)?0xFFFF7777:0xFFCF293B):ThemeStyle.text(activity);
   row.setBackground(new RippleDrawable(ColorStateList.valueOf(ThemeStyle.isDark(activity)?0x45FFFFFF:0x22000000),
       ThemeStyle.popupRowBackground(activity,false,i,count),null));
   ImageView icon=new ImageView(activity);
   icon.setImageDrawable(new UiIcons(action.getValue(),ink,ThemeStyle.dp(activity,19)));
   row.addView(icon,new LinearLayout.LayoutParams(ThemeStyle.dp(activity,23),ThemeStyle.dp(activity,23)));
   TextView title=new TextView(activity);title.setText(label);title.setTextSize(14);title.setTextColor(ink);
   LinearLayout.LayoutParams text=new LinearLayout.LayoutParams(-1,-2);
   text.setMargins(ThemeStyle.dp(activity,10),0,0,0);row.addView(title,text);
   content.addView(row,new LinearLayout.LayoutParams(-1,ThemeStyle.dp(activity,47)));
   row.setOnClickListener(v->{popup.dismiss();chosen.accept(label);});
   i++;
  }
  int width=ThemeStyle.dp(activity,250);
  int menuHeight=ThemeStyle.dp(activity,47)*count+2*dp+
      (count>1&&actions.containsKey("Удалить запись")?ThemeStyle.dp(activity,7):0);
  android.graphics.Rect bounds=new android.graphics.Rect();
  activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(bounds);
  int[] location=new int[2];anchor.getLocationOnScreen(location);
  int left=Math.max(bounds.left+dp,Math.min(location[0]+anchor.getWidth()-width,bounds.right-width-dp));
  int below=location[1]+anchor.getHeight();
  int above=location[1]-menuHeight;
  int y=below+menuHeight<=bounds.bottom?below:above>=bounds.top?above:
      Math.max(bounds.top,Math.min(below,bounds.bottom-menuHeight));
  popup.setClippingEnabled(true);
  popup.showAtLocation(activity.getWindow().getDecorView(),Gravity.LEFT|Gravity.TOP,left,y);
 }
}
