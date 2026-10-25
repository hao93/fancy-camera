package com.silverroom.camera;
import android.graphics.*;import android.graphics.drawable.Drawable;

/** Original wrapper artwork, using product-inspired color families without copied logos/images. */
class FilmLabelDrawable extends Drawable {
    private final Paint paint=new Paint(3);private final FilmLabelStyle style;private final String title;private final int iso;private int alpha=255;
    FilmLabelDrawable(int stock){this(stock,FilmLabelStyle.of(stock).mark(),FilmLabelStyle.of(stock).iso());}
    FilmLabelDrawable(int stock,String label,int speed){style=FilmLabelStyle.of(stock);title=label==null?FilmCatalog.reelName(stock):label;iso=speed;}
    private void fill(Canvas c,int color,float l,float t,float r,float b){paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setAlpha(alpha);c.drawRect(l,t,r,b,paint);}
    private void text(Canvas c,String value,float x,float y,float width,float size,int color,boolean bold){paint.setTypeface(Typeface.create(bold?"sans-serif-black":"sans-serif",0));paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(size);if(bold)paint.setTextSize(size*Math.max(.6f,Math.min(1,(width-2)/Math.max(1,paint.measureText(value)))*.98f));paint.setColor(color);paint.setAlpha(alpha);int end=value.length();while(end>0&&paint.measureText(value.substring(0,end)+(end<value.length()?"…":""))>width)end--;c.drawText(value.substring(0,end)+(end<value.length()?"…":""),x,y,paint);}
    @Override public void draw(Canvas c){Rect b=getBounds();if(b.width()<=0||b.height()<=0)return;float w=b.width(),h=b.height();c.save();c.translate(b.left,b.top);Path mask=new Path();mask.addRoundRect(0,0,w,h,h*.07f,h*.07f,Path.Direction.CW);c.clipPath(mask);fill(c,style.paper,0,0,w,h);
        if(style.pattern==1){fill(c,style.accent,0,0,w*.09f,h);fill(c,style.band,0,h*.76f,w,h);}
        else if(style.pattern==2){fill(c,style.band,0,h*.72f,w,h);paint.setColor(style.accent);paint.setAlpha(alpha);Path ribbon=new Path();ribbon.moveTo(0,h*.68f);ribbon.lineTo(w,h*.56f);ribbon.lineTo(w,h*.63f);ribbon.lineTo(0,h*.75f);ribbon.close();c.drawPath(ribbon,paint);}
        else if(style.pattern==4){fill(c,style.accent,0,h*.62f,w,h*.67f);fill(c,style.band,0,h*.73f,w,h);}
        else{fill(c,style.band,0,h*.74f,w,h);fill(c,style.accent,0,h*.69f,w,h*.74f);}
        // HP5-inspired white/green/black hierarchy; lettering remains our own.
        int ink=style.id==28?style.accent:style.ink;
        text(c,title,w*.09f,h*.49f,w*.82f,h*.29f,ink,true);
        text(c,"FANCY / "+style.type(),w*.09f,h*.17f,w*.82f,h*.075f,style.ink,false);
        text(c,iso>0?"ISO "+iso:style.type(),w*.09f,h*.91f,w*.82f,h*.10f,style.bandInk,true);
        fill(c,style.accent,w*.76f,h*.59f,w*.91f,h*.62f);c.restore();
    }
    @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
    @Override public void setColorFilter(ColorFilter value){paint.setColorFilter(value);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
