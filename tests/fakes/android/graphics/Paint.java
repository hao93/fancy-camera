package android.graphics;
import java.awt.Font;
import java.awt.font.FontRenderContext;
public final class Paint {
    public static final int ANTI_ALIAS_FLAG=1,FILTER_BITMAP_FLAG=2;public enum Style{FILL,STROKE}
    int color=0xff000000;float size=12,stroke=1;Style style=Style.FILL;
    public Paint(int flags){}public void setColor(int c){color=c;}public void setTextSize(float s){size=s;}public void setTypeface(Typeface type){}public void setStyle(Style s){style=s;}public void setStrokeWidth(float value){stroke=value;}
    Font font(){return new Font("Microsoft YaHei",Font.PLAIN,12).deriveFont(Math.max(1,size));}
    public float measureText(String text){return (float)font().getStringBounds(text,new FontRenderContext(null,true,true)).getWidth();}
}
