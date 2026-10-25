package com.silverroom.camera;

/** Shared geometry in normalized output pixels. No photograph is ever stretched or cropped. */
public final class PrintLayout {
    public final int width,height;public final float left,top,photoWidth,photoHeight,textX,textY,font,lineHeight;
    private PrintLayout(int w,int h,float l,float t,float pw,float ph,float x,float y,float f,float line){width=w;height=h;left=l;top=t;photoWidth=pw;photoHeight=ph;textX=x;textY=y;font=f;lineHeight=line;}
    public static PrintLayout single(int w,int h,int style,int lines,int edge){return layout(w,h,style,lines,edge,.025f);}
    public static PrintLayout contact(int w,int h,int style,int lines,int edge){return layout(w,h,style,lines,edge,.045f);}
    private static PrintLayout layout(int w,int h,int style,int lines,int edge,float fontRatio){if(w<=0||h<=0||edge<=0)throw new IllegalArgumentException("Invalid photo dimensions");float shortEdge=Math.min(w,h),margin=style==0?0:shortEdge*.04f,font=shortEdge*fontRatio,line=font*1.45f;lines=style==0?0:Math.max(0,Math.min(5,lines));float caption=lines==0?0:line*lines+margin*.6f,bottom=style==2?Math.max(shortEdge*.18f,margin+caption):margin+caption;float totalW=w+2*margin,totalH=h+margin+bottom,scale=Math.min(1,edge/Math.max(totalW,totalH));return new PrintLayout(Math.max(1,Math.round(totalW*scale)),Math.max(1,Math.round(totalH*scale)),margin*scale,margin*scale,w*scale,h*scale,margin*scale,(margin+h+margin*.45f+font)*scale,font*scale,line*scale);}
    public static int[] sheetSize(int edge){float factor=edge/4096f;return new int[]{Math.max(1,Math.round(2896*factor)),Math.max(1,edge)};}
    public static float[] cell(int index,int width,int height){if(index<0||index>8)throw new IllegalArgumentException("Invalid contact cell");float margin=width*.045f,gap=width*.025f,w=(width-2*margin-2*gap)/3,h=(height-2*margin-2*gap)/3;return new float[]{margin+(index%3)*(w+gap),margin+(index/3)*(h+gap),w,h};}
    public static float[] fit(float sourceWidth,float sourceHeight,float x,float y,float w,float h){float scale=Math.min(w/sourceWidth,h/sourceHeight);return new float[]{x+(w-sourceWidth*scale)/2,y+(h-sourceHeight*scale)/2,sourceWidth*scale,sourceHeight*scale};}
}
