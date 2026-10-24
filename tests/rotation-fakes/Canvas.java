package android.graphics;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.*;

/** Matrix traces only: exercises shipping onDraw without claiming native font or GPU rendering. */
public class Canvas {
    public final List<String> texts=new ArrayList<>();
    public final List<Float> widths=new ArrayList<>(),textAngles=new ArrayList<>(),pathAngles=new ArrayList<>(),shapeAngles=new ArrayList<>();
    public final List<float[]> textBounds=new ArrayList<>(),roundRects=new ArrayList<>();
    private AffineTransform matrix=new AffineTransform();private final Deque<AffineTransform> stack=new ArrayDeque<>();
    private float angle(){return (float)Math.toDegrees(Math.atan2(matrix.getShearY(),matrix.getScaleX()));}
    public void drawRoundRect(float a,float b,float c,float d,float e,float f,Paint p){shapeAngles.add(angle());roundRects.add(new float[]{a,b,c,d});}
    public void drawCircle(float x,float y,float r,Paint p){shapeAngles.add(angle());}
    public void drawLine(float a,float b,float c,float d,Paint p){shapeAngles.add(angle());}
    public void save(){stack.push(new AffineTransform(matrix));}
    public void restore(){matrix=stack.pop();}
    public void translate(float x,float y){matrix.translate(x,y);}
    public void scale(float x,float y){matrix.scale(x,y);}
    public void rotate(float degrees,float x,float y){matrix.rotate(Math.toRadians(degrees),x,y);}
    public void clipRect(float a,float b,float c,float d){}
    public void drawRect(float a,float b,float c,float d,Paint p){shapeAngles.add(angle());}
    public void drawPath(Path p,Paint paint){pathAngles.add(angle());}
    public void drawText(String text,float x,float y,Paint p){
        texts.add(text);float width=p.measureText(text);widths.add(width);textAngles.add(angle());
        float left=x-(p.getTextAlign()==Paint.Align.CENTER?width/2:0);Paint.FontMetrics fm=p.getFontMetrics();
        float[] bounds={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        for(float px:new float[]{left,left+width})for(float py:new float[]{y+fm.ascent,y+fm.descent}){
            Point2D point=matrix.transform(new Point2D.Float(px,py),null);float xx=(float)point.getX(),yy=(float)point.getY();
            bounds[0]=Math.min(bounds[0],xx);bounds[1]=Math.min(bounds[1],yy);bounds[2]=Math.max(bounds[2],xx);bounds[3]=Math.max(bounds[3],yy);
        }
        textBounds.add(bounds);
    }
}
