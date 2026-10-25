package com.silverroom.camera;
import android.opengl.GLES20;import java.util.*;

public final class NativePreviewProbe {
 static int count;static void check(boolean yes,String name){if(!yes)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
 static void paint(float r,float g,float b){GLES20.glClearColor(r,g,b,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);}
 static boolean close(int[] a,int[] b){for(int n=0;n<3;n++)if(Math.abs(a[n]-b[n])>2)return false;return true;}
 public static void run(String python,String bridge,String libraries)throws Exception{
  System.out.println("NATIVE EGL "+GLES20.start(python,bridge,libraries));
  try{
   GLES20.glViewport(0,0,64,64);PreviewFramebuffer frame=new PreviewFramebuffer();paint(.04f,.045f,.04f);check(!frame.draw(64,64),"Native: uninitialized buffers cannot be displayed");
   check(frame.begin(64,64),"Native: owned RGBA framebuffer is complete");paint(.2f,.6f,.9f);check(frame.commit()&&frame.draw(64,64),"Native: actual shipping buffer and presentation shaders commit and draw");int[] blue=GLES20.pixel(32,32);check(close(blue,new int[]{51,153,230,255}),"Native: displayed RGB pixels match the completed source frame");
   boolean stable=true;for(int n=0;n<50;n++){paint(.04f,.045f,.04f);stable&=frame.draw(64,64)&&close(GLES20.pixel(32,32),blue);}check(stable,"Native: a long metadata gap repeatedly restores the complete image instead of black");
   frame.begin(64,64);paint(0,0,0);frame.abort();frame.draw(64,64);check(close(GLES20.pixel(32,32),blue),"Native: an interrupted black candidate cannot replace the displayed image");
   frame.begin(64,64);paint(0,0,0);GLES20.glBindTexture(-1,0);check(!frame.commit(),"Native: actual GL errors reject a frame before publication");frame.draw(64,64);check(close(GLES20.pixel(32,32),blue),"Native: the previous completed texture survives a GPU rendering error");
   boolean alternate=true;for(int n=0;n<30;n++){frame.begin(64,64);paint(n%2==0?.8f:.1f,.4f,.2f);frame.commit();frame.draw(64,64);int[] displayed=GLES20.pixel(32,32);paint(.04f,.045f,.04f);frame.draw(64,64);alternate&=close(displayed,GLES20.pixel(32,32));}check(alternate,"Native: both reusable render targets show identical pixels in live and held presentation");
   frame.begin(64,64);paint(.2f,.6f,.9f);frame.commit();frame.draw(64,64);
   LogicalZoomState gate=new LogicalZoomState();Map<String,Integer> ids=new HashMap<>();ids.put("main",23);gate.reset(ids);gate.begin(23);LogicalZoomState.Tag tag=gate.tag(0);boolean noBlack=true;
   for(int n=0;n<187;n++){long timestamp=1000+n/2;if(n%2==0)gate.image(timestamp);else gate.result(tag,timestamp,"main",1f,0f,100);if(gate.proof()!=null){frame.begin(64,64);paint(.2f,.6f,.9f);frame.commit();}paint(.04f,.045f,.04f);noBlack&=frame.draw(64,64)&&close(GLES20.pixel(32,32),blue)&&close(GLES20.pixel(0,0),blue)&&close(GLES20.pixel(63,63),blue);}
   check(noBlack,"Native: 187 alternating image/result frames retain the complete picture at center and corners with no black inserts");
   int[] last=GLES20.pixel(32,32);check(!frame.begin(8192,8192),"Native: unsupported dimensions reject allocation without replacing the front buffer");frame.draw(64,64);check(close(last,GLES20.pixel(32,32)),"Native: the last visible image survives an allocation rejection");
   GLES20.glViewport(3,5,40,44);frame.begin(64,64);frame.abort();int[] viewport=new int[4];GLES20.glGetIntegerv(GLES20.GL_VIEWPORT,viewport,0);check(Arrays.equals(viewport,new int[]{3,5,40,44}),"Native: candidate cancellation restores the original viewport");
   PreviewLatch latch=new PreviewLatch();latch.consumed(1,100,1000);check(!latch.drain(true,false,1,1040,false,false),"Native sequence: delayed metadata retains its consumed image");check(!latch.drain(true,false,1,1050,true,false),"Native sequence: a proved frame is not skipped by a newer texture notification");latch.displayed(1,100);check(latch.drain(true,false,1,1051,true,false),"Native sequence: successful presentation immediately permits the next image");
   frame.clear();check(!frame.draw(64,64),"Native: pause invalidates retained preview pixels");check(GLES20.glGetError()==0,"Native: no GL errors remain after all frame transitions");
   System.out.println("ALL "+count+" NATIVE EGL PIXEL CHECKS PASSED");
  }finally{GLES20.stop();}
 }
}
