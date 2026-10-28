package com.lklengl.youtubevideodownloader;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Small vector icons, independent of fonts and Android launcher resources. */
public final class UiIcons extends Drawable {
    private final String name;
    private final int color, size;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    public UiIcons(String name,int color,int size){
        this.name=name;this.color=color;this.size=size;
    }
    @Override public int getIntrinsicWidth(){return size;}
    @Override public int getIntrinsicHeight(){return size;}
    @Override public void draw(Canvas c){
        Rect b=getBounds();c.save();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);
        p.setColor(color);p.setStrokeWidth(1.9f);p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);
        switch(name){
            case "download":
                c.drawLine(12,3,12,16,p);c.drawLine(7,11,12,16,p);c.drawLine(17,11,12,16,p);
                c.drawLine(4,19,20,19,p);c.drawLine(4,19,4,16,p);c.drawLine(20,19,20,16,p);break;
            case "queue":
                for(int y:new int[]{6,12,18}){c.drawCircle(4,y,1,p);c.drawLine(9,y,20,y,p);}break;
            case "clock":
                c.drawCircle(12,12,9,p);c.drawLine(12,7,12,12,p);c.drawLine(12,12,16,14,p);break;
            case "quality":
                c.drawRoundRect(new RectF(2.5f,4,21.5f,17),2,2,p);
                c.drawLine(9,20,15,20,p);c.drawLine(12,17,12,20,p);break;
            case "sun":
                c.drawCircle(12,12,4,p);
                for(int i=0;i<8;i++){double a=i*Math.PI/4;float x=(float)Math.cos(a),y=(float)Math.sin(a);
                    c.drawLine(12+7*x,12+7*y,12+10*x,12+10*y,p);}break;
            case "moon":
                Path crescent=new Path();crescent.moveTo(20.5f,14);
                crescent.cubicTo(18.5f,20,11,22,6.5f,17.5f);
                crescent.cubicTo(2,13,4,5.5f,10,3.5f);
                crescent.cubicTo(7.5f,8.5f,13.2f,16.8f,20.5f,14);c.drawPath(crescent,p);break;
            case "phone":
                c.drawRoundRect(new RectF(6,2,18,22),2.5f,2.5f,p);c.drawLine(10,19,14,19,p);break;
            case "chevron":
                c.drawLine(6,9,12,15,p);c.drawLine(12,15,18,9,p);break;
            case "trash":
                c.drawLine(4,6,20,6,p);c.drawLine(9,4,15,4,p);
                c.drawRoundRect(new RectF(6.5f,7,17.5f,21),1.3f,1.3f,p);
                c.drawLine(10,10,10,17,p);c.drawLine(14,10,14,17,p);break;
            case "close":
                c.drawLine(5,5,19,19,p);c.drawLine(19,5,5,19,p);break;
            case "settings":
                Path gear=new Path();
                for(int i=0;i<32;i++){
                    double a=i*2*Math.PI/32-Math.PI/2;
                    float radius=(i%4==0||i%4==3)?10.2f:7.5f;
                    float px=12+(float)Math.cos(a)*radius;
                    float py=12+(float)Math.sin(a)*radius;
                    if(i==0)gear.moveTo(px,py);else gear.lineTo(px,py);
                }
                gear.close();c.drawPath(gear,p);c.drawCircle(12,12,3.2f,p);break;
            case "play":
                Path triangle=new Path();triangle.moveTo(7,4);triangle.lineTo(19,12);
                triangle.lineTo(7,20);triangle.close();c.drawPath(triangle,p);break;
            case "list":
                c.drawLine(5,6,20,6,p);c.drawLine(5,12,20,12,p);c.drawLine(5,18,20,18,p);break;
            default:
                c.drawCircle(12,12,9,p);c.drawLine(12,10,12,17,p);c.drawCircle(12,7,0.7f,p);
        }
        c.restore();
    }
    @Override public void setAlpha(int a){p.setAlpha(a);}
    @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
