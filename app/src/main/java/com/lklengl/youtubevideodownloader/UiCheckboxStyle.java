package com.lklengl.youtubevideodownloader;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.widget.CheckBox;

/** The checked glyph is white independent of Android/OEM button-tint rules. */
public final class UiCheckboxStyle {
 private UiCheckboxStyle(){}
 public static void apply(CheckBox box){
  Context c=box.getContext();
  box.setButtonTintList(null);
  int size=ThemeStyle.dp(c,21);
  StateListDrawable states=new StateListDrawable();
  states.addState(new int[]{android.R.attr.state_checked},new Mark(size,true,0xFFEF2830));
  states.addState(new int[]{},new Mark(size,false,ThemeStyle.isDark(c)?0xFFF4F5F8:0xFF596477));
  box.setButtonDrawable(states);
  box.setMinHeight(ThemeStyle.dp(c,38));
 }
 private static final class Mark extends Drawable {
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final int size,color;private final boolean selected;
  Mark(int size,boolean selected,int color){this.size=size;this.selected=selected;this.color=color;}
  @Override public int getIntrinsicWidth(){return size;}
  @Override public int getIntrinsicHeight(){return size;}
  @Override public void draw(Canvas canvas){
   Rect b=getBounds();canvas.save();canvas.translate(b.left,b.top);
   canvas.scale(b.width()/24f,b.height()/24f);
   paint.setColor(color);paint.setStrokeWidth(2.2f);
   if(selected){
    paint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(2,2,22,22,2.2f,2.2f,paint);
    paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(2.6f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
    Path tick=new Path();tick.moveTo(6.7f,12.4f);tick.lineTo(10.7f,16.1f);tick.lineTo(17.5f,8);
    canvas.drawPath(tick,paint);
   }else{
    paint.setStyle(Paint.Style.STROKE);
    canvas.drawRoundRect(3,3,21,21,1.5f,1.5f,paint);
   }
   canvas.restore();
  }
  @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
  @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
  @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
}