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
            case "check":
                Path tick=new Path();tick.moveTo(4,12);tick.lineTo(10,18);tick.lineTo(20,6);c.drawPath(tick,p);break;
            case "clipboard":
                c.drawRoundRect(new RectF(5,5,20,22),2,2,p);
                c.drawRoundRect(new RectF(9,2,16,8),1,1,p);break;
            case "folder-open": {
                // Lucide-like folder-open: independent lid and slanted open lower edge.
                Path lid=new Path();
                lid.moveTo(2,10);lid.lineTo(2,6);lid.quadTo(2,4,4,4);
                lid.lineTo(9,4);lid.lineTo(11.5f,7);lid.lineTo(19,7);
                lid.quadTo(21,7,21,9);c.drawPath(lid,p);
                Path open=new Path();open.moveTo(3.8f,11);open.lineTo(20,11);
                open.quadTo(22.5f,11,21.5f,13.3f);
                open.lineTo(19.4f,19.2f);open.quadTo(19.1f,20,18,20);
                open.lineTo(4.5f,20);open.quadTo(2.3f,20,2,18);
                open.lineTo(3.8f,11);c.drawPath(open,p);break;
            }
            case "folder-edit": {
                Path folder=new Path();folder.moveTo(3,10);folder.lineTo(3,6);
                folder.quadTo(3,4,5,4);folder.lineTo(9,4);
                folder.lineTo(11.5f,7);folder.lineTo(19,7);
                folder.quadTo(21,7,21,9);folder.lineTo(21,12);
                c.drawPath(folder,p);
                c.drawLine(3,10,3,19,p);c.drawLine(3,19,12,19,p);
                Path pencil=new Path();pencil.moveTo(13.5f,18.8f);
                pencil.lineTo(12.5f,21);pencil.lineTo(14.8f,20);
                pencil.lineTo(21,13.8f);pencil.lineTo(18.7f,11.5f);
                pencil.lineTo(13.5f,16.7f);pencil.close();c.drawPath(pencil,p);
                c.drawLine(17.6f,12.7f,19.8f,15,p);
                break;
            }
            case "en":
            case "ru":
                c.drawRoundRect(new RectF(1,3,23,21),3,3,p);
                p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);
                p.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.BOLD));
                p.setTextSize(10);c.drawText("en".equals(name)?"EN":"RU",12,15.5f,p);break;
            case "pause":
                p.setStyle(Paint.Style.FILL);
                c.drawRoundRect(new RectF(6,4,10,20),1,1,p);
                c.drawRoundRect(new RectF(14,4,18,20),1,1,p);
                break;
            case "more":
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(12,5,1.8f,p);c.drawCircle(12,12,1.8f,p);
                c.drawCircle(12,19,1.8f,p);
                break;
            case "folder":
                Path folder=new Path();folder.moveTo(2,7);folder.lineTo(2,19);
                folder.quadTo(2,21,4,21);folder.lineTo(20,21);folder.quadTo(22,21,22,19);
                folder.lineTo(22,9);folder.quadTo(22,7,20,7);folder.lineTo(11,7);
                folder.lineTo(9,4);folder.lineTo(4,4);folder.quadTo(2,4,2,7);
                c.drawPath(folder,p);break;
            case "app-window":
                c.drawRoundRect(new RectF(2,4,22,20),2,2,p);
                c.drawLine(2,9,22,9,p);c.drawCircle(6,6.5f,0.6f,p);
                c.drawCircle(9,6.5f,0.6f,p);break;
            case "refresh-cw":
                c.drawArc(new RectF(3,3,21,21),35,285,false,p);
                c.drawLine(18,3,21,9,p);c.drawLine(21,9,15,9,p);break;
            case "undo-2": {
                Path turn=new Path();turn.moveTo(9,14);turn.lineTo(4,9);
                turn.lineTo(9,4);c.drawPath(turn,p);
                Path body=new Path();body.moveTo(4,9);body.lineTo(14.5f,9);
                body.cubicTo(21.5f,9,21.5f,20,14.5f,20);body.lineTo(11,20);
                c.drawPath(body,p);break;
            }
            case "arrow-outline-up":
            case "arrow-outline-down": {
                Path outline=new Path();
                if(name.equals("arrow-outline-up")){
                    outline.moveTo(12,2);outline.lineTo(22,12);outline.lineTo(16.5f,12);
                    outline.lineTo(16.5f,22);outline.lineTo(7.5f,22);
                    outline.lineTo(7.5f,12);outline.lineTo(2,12);
                }else{
                    outline.moveTo(12,22);outline.lineTo(22,12);outline.lineTo(16.5f,12);
                    outline.lineTo(16.5f,2);outline.lineTo(7.5f,2);
                    outline.lineTo(7.5f,12);outline.lineTo(2,12);
                }
                outline.close();p.setStrokeWidth(1.5f);c.drawPath(outline,p);break;
            }
            case "info":
                c.drawCircle(12,12,9,p);c.drawLine(12,10,12,17,p);
                c.drawCircle(12,7,0.8f,p);break;
            case "plus":
                c.drawLine(12,4,12,20,p);c.drawLine(4,12,20,12,p);break;
            case "sliders":
                c.drawLine(3,6,21,6,p);c.drawLine(3,12,21,12,p);
                c.drawLine(3,18,21,18,p);
                p.setStyle(Paint.Style.FILL);c.drawCircle(8,6,3,p);
                c.drawCircle(16,12,3,p);c.drawCircle(10,18,3,p);break;
            case "download":
                c.drawLine(12,3,12,16,p);c.drawLine(7,11,12,16,p);c.drawLine(17,11,12,16,p);
                c.drawLine(4,19,20,19,p);c.drawLine(4,19,4,16,p);c.drawLine(20,19,20,16,p);break;
            case "queue":
                for(int y:new int[]{6,12,18}){c.drawCircle(4,y,1,p);c.drawLine(9,y,20,y,p);}break;
            case "check-circle":
                c.drawCircle(12,12,9,p);
                c.drawLine(7,12,10,15,p);c.drawLine(10,15,17,8,p);break;
            case "alert-triangle":
                Path warning=new Path();warning.moveTo(12,2);
                warning.lineTo(22,21);warning.lineTo(2,21);warning.close();
                c.drawPath(warning,p);c.drawLine(12,9,12,14,p);
                p.setStyle(Paint.Style.FILL);c.drawCircle(12,18,1.1f,p);break;
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
