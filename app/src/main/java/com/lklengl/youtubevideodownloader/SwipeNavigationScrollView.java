package com.lklengl.youtubevideodownloader;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import java.util.function.IntConsumer;

/** Horizontal page gestures without intercepting vertical scrolling or editable controls. */
public final class SwipeNavigationScrollView extends ScrollView {
    private float originX,originY;
    private boolean eligible,capturing;
    private int pageIndex;
    private IntConsumer pageChanged;
    private final int threshold;

    public SwipeNavigationScrollView(Context context){
        super(context);
        threshold=Math.max(ThemeStyle.dp(context,65),ViewConfiguration.get(context).getScaledTouchSlop()*6);
    }
    public void setPageIndex(int page){pageIndex=page;}
    public void setPageChanged(IntConsumer listener){pageChanged=listener;}

    private static boolean overInput(View view,float x,float y){
        if(view.getVisibility()!=View.VISIBLE)return false;
        if(view instanceof EditText || view instanceof Spinner) {
            int[] loc=new int[2];view.getLocationOnScreen(loc);
            if(x>=loc[0]&&x<loc[0]+view.getWidth()&&y>=loc[1]&&y<loc[1]+view.getHeight())return true;
        }
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)
                if(overInput(group.getChildAt(i),x,y))return true;
        }
        return false;
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN) {
            originX=e.getX();originY=e.getY();
            eligible=!overInput(this,e.getRawX(),e.getRawY());
            capturing=false;
            return super.onInterceptTouchEvent(e);
        }
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE && eligible) {
            float dx=e.getX()-originX,dy=e.getY()-originY;
            if(Math.abs(dx)>threshold && Math.abs(dx)>Math.abs(dy)*1.5f) {
                capturing=true;return true;
            }
        }
        return super.onInterceptTouchEvent(e);
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(capturing){
            if(e.getActionMasked()==MotionEvent.ACTION_UP) {
                int target=SwipeRules.target(pageIndex,e.getX()-originX,e.getY()-originY,threshold);
                if(target!=pageIndex && pageChanged!=null)pageChanged.accept(target);
                capturing=false;
            }else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)capturing=false;
            return true;
        }
        return super.onTouchEvent(e);
    }
}
