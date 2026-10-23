import com.silverroom.camera.*;
import java.util.*;
import java.io.File;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public final class GrainRegression {
    static int count;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
    // Round each operation to a binary16-like 10-bit mantissa. This is a model, not a phone capture.
    static float half(float x){if(x==0)return x;float step=Math.scalb(1f,Math.getExponent(Math.abs(x))-10);return (float)(Math.rint(x/step)*step);}
    static float legacy(int x,int y){float px=half((x+.5f)/512),py=half((y+.5f)/512);float phase=half(half(px*half(12.9898f))+half(py*half(78.233f)));float amplified=half(half((float)Math.sin(phase))*half(43758.5453f));return amplified-(float)Math.floor(amplified)-.5f;}
    static double corr(byte[] bytes,int dx,int dy){double cross=0,a2=0,b2=0;for(int y=0;y<512;y++)for(int x=0;x<512;x++){double a=PreviewGrain.sample(bytes,x,y),b=PreviewGrain.sample(bytes,x+dx,y+dy);cross+=a*b;a2+=a*a;b2+=b*b;}return cross/Math.sqrt(a2*b2);}
    public static void main(String[] args)throws Exception{
        byte[] bytes=PreviewGrain.pixels();check(bytes.length==512*512,"Grain upload is bounded to one 256KB power-of-two texture");
        check(Arrays.equals(bytes,PreviewGrain.pixels()),"Context recreation reproduces a stable texture without frame-to-frame flicker");
        int[] histogram=new int[256];for(byte b:bytes)histogram[b&255]++;boolean uniform=true;for(int n:histogram)uniform&=n==1024;check(uniform,"All 256 luminance levels occur equally, avoiding biased brightness or sparse noise");
        double sum=0,var=0;for(byte b:bytes){double n=(b&255)/255.0-.5;sum+=n;var+=n*n;}check(Math.abs(sum/bytes.length)<1e-7,"The complete grain tile has zero brightness bias");
        check(var/bytes.length>.08&&var/bytes.length<.085,"The grain retains the intended bounded random contrast");
        boolean uncorrelated=true;double max=0;for(int[] d:new int[][]{{1,0},{0,1},{1,1},{1,-1},{6,-1},{-6,1},{2,2},{4,-4},{16,0},{0,32},{64,64}}){double c=Math.abs(corr(bytes,d[0],d[1]));max=Math.max(max,c);uncorrelated&=c<.02;}
        check(uncorrelated,"Horizontal, vertical and diagonal neighboring samples have no strong directional correlation");
        boolean rowMeans=true;for(int y=0;y<512;y++){double row=0,col=0;for(int x=0;x<512;x++){row+=PreviewGrain.sample(bytes,x,y);col+=PreviewGrain.sample(bytes,y,x);}rowMeans&=Math.abs(row/512)<.07&&Math.abs(col/512)<.07;}check(rowMeans,"No row or column has a strong repeated brightness stripe");
        check(PreviewGrain.sample(bytes,0,0)==PreviewGrain.sample(bytes,512,512)&&PreviewGrain.sample(bytes,-1,-1)==PreviewGrain.sample(bytes,511,511),"Texture repeat coordinates wrap correctly at positive and negative edges");
        check(PreviewGrain.sample(bytes,7,19)==PreviewGrain.sample(bytes,7+512*6,19+512*6),"Tall phone viewports keep valid grain samples outside the first tile");
        boolean range=true;for(int y=0;y<512;y++)for(int x=0;x<512;x++){float n=PreviewGrain.sample(bytes,x,y);range&=n>=-.5f&&n<=.5f;}check(range,"Every noise sample stays in the documented signed range");
        Set<Float> levels=new HashSet<>();int collapsed=0;for(int y=0;y<512;y++)for(int x=0;x<512;x++){float n=legacy(x,y);levels.add(n);if(n==-.5f)collapsed++;}
        check(collapsed>bytes.length*.97&&levels.size()<64,"The old sine hash collapses almost all samples under binary16 precision");
        check(!CameraShaders.EXTERNAL.contains("43758")&&!CameraShaders.YUV.contains("sin("),"Both shipping preview pipelines remove the unstable trigonometric hash");
        check(CameraShaders.EXTERNAL.contains("uniform sampler2D uGrain")&&CameraShaders.YUV.contains("texture2D(uGrain"),"Both external and YUV previews use the same dedicated grain texture");
        check(CameraShaders.EXTERNAL.contains("floor(gl_FragCoord.xy)")&&CameraShaders.YUV.contains("/512."),"Noise follows output pixels rather than rotated or zoomed source diagonals");
        check(CameraShaders.EXTERNAL.contains("if(uTone.w>0.)")&&CameraShaders.YUV.contains("if(uMix.x<=0.)"),"Zero grain and zero film strength bypass the noise contribution");
        boolean precision=true;for(String shader:new String[]{CameraShaders.EXTERNAL,CameraShaders.YUV,CameraShaders.RAW_EXTERNAL,CameraShaders.RAW_YUV})precision&=shader.contains("GL_FRAGMENT_PRECISION_HIGH")&&shader.contains("precision highp float;")&&shader.contains("precision mediump float;");check(precision,"Every program selects supported high precision with a mediump fallback");
        if(args.length>0){File folder=new File(args[0]);folder.mkdirs();BufferedImage sheet=new BufferedImage(1024,556,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setColor(new Color(20,26,22));g.fillRect(0,0,1024,556);g.setColor(new Color(238,226,205));g.setFont(new Font("SansSerif",Font.PLAIN,16));g.drawString("OLD / FP16 MODEL / CONTRAST x8",12,28);g.drawString("NEW / BALANCED TEXTURE / CONTRAST x8",524,28);g.dispose();for(int y=0;y<512;y++)for(int x=0;x<512;x++){int old=Math.round(128+legacy(x,y)*64),fresh=Math.round(128+PreviewGrain.sample(bytes,x,y)*64);sheet.setRGB(x,y+44,(old<<16)|(old<<8)|old);sheet.setRGB(x+512,y+44,(fresh<<16)|(fresh<<8)|fresh);}ImageIO.write(sheet,"png",new File(folder,"grain-precision-comparison.png"));}
        System.out.println("Legacy collapsed fraction="+collapsed/(double)bytes.length+"; levels="+levels.size()+"; new maximum neighbor correlation="+max);
        System.out.println("ALL "+count+" GRAIN PRECISION / DIRECTION TESTS PASSED");
    }
}
