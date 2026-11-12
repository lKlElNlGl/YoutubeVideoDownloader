package com.lklengl.youtubevideodownloader;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

/**
 * Compact Spinner with a bounded, genuinely rounded dropdown.
 * Unlike a native Spinner popup, the *scrollable rows* are clipped to the rounded
 * container, so an intermediate rectangular row cannot protrude at scroll edges.
 */
public final class RotatingSpinner extends Spinner {
 private ImageView arrow;
 private AdapterView.OnItemSelectedListener chosen;
 private PopupWindow popup;
 private Runnable afterDismiss;
 private java.util.function.IntConsumer onPopupPick;
 private int sectionBreak=-1;

 public RotatingSpinner(Context context){
  super(context,Spinner.MODE_DROPDOWN);
  setOnItemSelectedListener(null);
 }
 public void setArrow(ImageView indicator){arrow=indicator;}
 /** Divider between first and second group; not an adapter item or a selectable quality. */
 public void setSectionBreak(int index){sectionBreak=index;}
 public boolean isPopupOpen(){return popup!=null&&popup.isShowing();}
 public void setOnPopupDismiss(Runnable callback){afterDismiss=callback;}
 /** Called synchronously on an explicit menu pick, before recycled row rebinding. */
 public void setOnPopupChoiceSelected(java.util.function.IntConsumer callback){onPopupPick=callback;}
 private int dp(float value){return ThemeStyle.dp(getContext(),value);}
 private void rotate(boolean expanded){
  if(arrow!=null)arrow.animate().rotation(expanded?180f:0f).setDuration(150).start();
 }

 @Override public boolean performClick(){
  if(isPopupOpen())return true;
  final SpinnerAdapter original=getAdapter();
  if(original==null||original.getCount()==0||!isEnabled())return false;
  final int split=sectionBreak>0&&sectionBreak<original.getCount()?sectionBreak:-1;
  final int dividerHeight=dp(13),itemHeight=dp(ThemeStyle.POPUP_ITEM_HEIGHT_DP);
  final BaseAdapter rows=new BaseAdapter(){
   @Override public int getCount(){return original.getCount()+(split>=0?1:0);}
   @Override public Object getItem(int position){
    int index=Release062Rules.popupIndexToChoice(position,split);
    return index<0?null:original.getItem(index);
   }
   @Override public long getItemId(int position){
    int index=Release062Rules.popupIndexToChoice(position,split);
    return index<0?-1:original.getItemId(index);
   }
   @Override public boolean areAllItemsEnabled(){return split<0;}
   @Override public boolean isEnabled(int position){
    return Release062Rules.popupIndexToChoice(position,split)>=0;
   }
   @Override public View getView(int position,View convert,ViewGroup parent){
    int index=Release062Rules.popupIndexToChoice(position,split);
    if(index<0){
     LinearLayout holder=new LinearLayout(getContext());
     holder.setGravity(Gravity.CENTER_VERTICAL);
     holder.setPadding(dp(12),0,dp(12),0);
     View line=new View(getContext());
     line.setBackgroundColor(ThemeStyle.border(getContext()));
     holder.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
     holder.setLayoutParams(new AbsListView.LayoutParams(-1,dividerHeight));
     return holder;
    }
    // getDropDownView keeps each activity's approved fonts, colors and row highlights.
    return original.getDropDownView(index,null,parent);
   }
  };

  final FrameLayout clipped=new FrameLayout(getContext());
  clipped.setBackground(ThemeStyle.popupBackground(getContext()));
  clipped.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
  clipped.setClipToOutline(true);
  clipped.setClipChildren(true);
  clipped.setClipToPadding(true);
  final ListView entries=new ListView(getContext());
  entries.setAdapter(rows);
  entries.setBackgroundColor(Color.TRANSPARENT);
  entries.setDivider(null);entries.setDividerHeight(0);
  entries.setOverScrollMode(View.OVER_SCROLL_NEVER);
  entries.setVerticalScrollBarEnabled(true);
  entries.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
  entries.setPadding(dp(1),dp(1),dp(1),dp(1));
  clipped.addView(entries,new FrameLayout.LayoutParams(-1,-1));

  int requested=getDropDownWidth();
  int screen=getResources().getDisplayMetrics().widthPixels;
  int width=Math.min(screen-dp(16),Math.max(getWidth(),requested>0?requested:getWidth()));
  int selected=Math.max(0,Math.min(original.getCount()-1,getSelectedItemPosition()));
  int px[]=new int[2];getLocationOnScreen(px);
  int targetX=Math.max(dp(8),Math.min(px[0],screen-dp(8)-width));
  int rowCount=original.getCount();
  int expected=rowCount*itemHeight+(split>=0?dividerHeight:0)+dp(2);
  PopupWindow window=new PopupWindow(clipped,width,expected,true);
  window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
  window.setOutsideTouchable(true);
  window.setClippingEnabled(true);
  window.setElevation(dp(8));
  window.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
  int maxHeight=window.getMaxAvailableHeight(this,dp(-20));
  window.setHeight(Math.max(itemHeight,Math.min(expected,maxHeight-dp(3))));
  popup=window;
  window.setOnDismissListener(()->{
   popup=null;rotate(false);
   if(afterDismiss!=null)post(afterDismiss);
  });
  entries.setOnItemClickListener((parent,view,position,id)->{
   int value=Release062Rules.popupIndexToChoice(position,split);
   if(value<0||value>=original.getCount())return;
   if(onPopupPick!=null)onPopupPick.accept(value);
   setSelection(value);
   window.dismiss();
  });
  try{
   rotate(true);
   window.showAsDropDown(this,targetX-px[0],-dp(20),Gravity.START);
   final int choice=Release062Rules.choiceIndexToPopup(selected,split);
   entries.post(()->{
    if(window.isShowing())entries.setSelectionFromTop(choice,
      Math.max(dp(4),entries.getHeight()/2-itemHeight/2));
   });
   return true;
  }catch(RuntimeException ex){
   popup=null;rotate(false);throw ex;
  }
 }
 @Override public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener){
  chosen=listener;
  super.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
   @Override public void onItemSelected(AdapterView<?> p,View v,int position,long id){
    if(!isPopupOpen())rotate(false);
    if(chosen!=null)chosen.onItemSelected(p,v,position,id);
   }
   @Override public void onNothingSelected(AdapterView<?> p){
    if(!isPopupOpen())rotate(false);
    if(chosen!=null)chosen.onNothingSelected(p);
   }
  });
 }
 @Override public void onWindowFocusChanged(boolean focus){
  super.onWindowFocusChanged(focus);
  if(focus&&!isPopupOpen())rotate(false);
 }
 @Override protected void onDetachedFromWindow(){
  if(popup!=null)popup.dismiss();
  super.onDetachedFromWindow();
 }
}
