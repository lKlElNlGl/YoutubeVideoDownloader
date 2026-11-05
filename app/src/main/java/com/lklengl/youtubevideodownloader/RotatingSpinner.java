package com.lklengl.youtubevideodownloader;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
/** Same arrow up while a native dropdown is open, down after selection/dismissal. */
public final class RotatingSpinner extends Spinner {
 private ImageView arrow;
 private AdapterView.OnItemSelectedListener chosen;
 public RotatingSpinner(Context context){super(context,Spinner.MODE_DROPDOWN);}
 public void setArrow(ImageView indicator){arrow=indicator;}
 private void rotate(boolean expanded){
  if(arrow!=null)arrow.animate().rotation(expanded?180f:0f).setDuration(150).start();
 }
 @Override public boolean performClick(){
  rotate(true);return super.performClick();
 }
 @Override public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener){
  chosen=listener;
  super.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
   @Override public void onItemSelected(AdapterView<?> p,View v,int position,long id){
    rotate(false);
    if(chosen!=null)chosen.onItemSelected(p,v,position,id);
   }
   @Override public void onNothingSelected(AdapterView<?> p){
    rotate(false);
    if(chosen!=null)chosen.onNothingSelected(p);
   }
  });
 }
 @Override public void onWindowFocusChanged(boolean focus){
  super.onWindowFocusChanged(focus);
  if(focus)rotate(false);
 }
}
