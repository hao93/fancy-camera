package android.graphics;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/** Host raster substitute only. Android typography, filtering and GPU behavior are not emulated. */
public final class Canvas {
    private final Bitmap bitmap;private final Graphics2D g;private final Deque<AffineTransform> stack=new ArrayDeque<>();
    public static final java.util.List<String> texts=new ArrayList<>();public static final java.util.List<float[]> bounds=new ArrayList<>(),lines=new ArrayList<>();
    public Canvas(Bitmap target){bitmap=target;g=target.buffer.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);}
    private void paint(Paint p){g.setColor(new Color(p.color,true));g.setStroke(new BasicStroke(Math.max(.1f,p.stroke)));g.setFont(p.font());}
    public void drawColor(int color){g.setColor(new Color(color,true));g.fillRect(0,0,bitmap.getWidth(),bitmap.getHeight());}
    public void drawRect(float l,float t,float r,float b,Paint p){paint(p);Rectangle2D rect=new Rectangle2D.Float(l,t,r-l,b-t);if(p.style==Paint.Style.FILL)g.fill(rect);else g.draw(rect);}
    public void drawBitmap(Bitmap source,Object src,RectF destination,Paint p){if(source.isRecycled())throw new AssertionError("Recycled source drawn");AffineTransform transform=new AffineTransform();transform.translate(destination.left,destination.top);transform.scale((destination.right-destination.left)/source.getWidth(),(destination.bottom-destination.top)/source.getHeight());g.drawImage(source.buffer,transform,null);}
    public void drawText(String text,float x,float y,Paint p){paint(p);g.drawString(text,x,y);texts.add(text);bounds.add(new float[]{x,y,p.measureText(text),p.size});}
    public void drawLine(float x,float y,float xx,float yy,Paint p){paint(p);g.draw(new Line2D.Float(x,y,xx,yy));lines.add(new float[]{x,y,xx,yy});}
    public void save(){stack.push(g.getTransform());}public void restore(){g.setTransform(stack.pop());}public void translate(float x,float y){g.translate(x,y);}public void scale(float x,float y){g.scale(x,y);}public void rotate(float angle,float x,float y){g.rotate(Math.toRadians(angle),x,y);}
}
