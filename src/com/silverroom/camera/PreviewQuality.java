package com.silverroom.camera;

/** Preview detail is chosen before app-side focal cropping, independently of photo output. */
public final class PreviewQuality {
    private PreviewQuality(){}
    public static int tier(float mm){float crop=LensPlan.crop(mm);return crop<1.1f?0:crop<1.8f?1:2;}
    public static int count(boolean split){return split?8:5;}
    public static boolean yuv(int profile){return profile<5;}
    public static long budget(float mm,int profile){
        long target=new long[]{2048L*1536,3072L*2048,4096L*3072}[tier(mm)];
        if(profile==0)return target;
        if(profile==1)return Math.min(2048L*1536,target*2/3);
        if(profile==2)return Math.min(1920L*1080,Math.min(2048L*1536,target*2/3)*2/3);
        if(profile==3||profile==6)return 640L*480;
        if(profile==4||profile==7)return 320L*240;
        return 1920L*1080;
    }
    /** Returns an index into the advertised sizes, never invents a stream size. */
    public static int choose(int[][] sizes,long[] durations,long budget,int maxEdge){
        if(sizes==null||sizes.length==0||durations==null||durations.length!=sizes.length)throw new IllegalArgumentException("No preview sizes");
        for(int pass=0;pass<2;pass++){
            int best=-1;double score=Double.MAX_VALUE;
            for(int i=0;i<sizes.length;i++){int w=sizes[i][0],h=sizes[i][1];long pixels=(long)w*h;if(w<=0||h<=0||((w|h)&1)!=0||Math.max(w,h)>maxEdge||pixels>budget)continue;
                if(pass==0&&durations[i]>66666667L)continue;
                double value=Math.abs(w/(double)h-4d/3)*10+Math.abs(Math.log(budget/(double)pixels))*.1;
                if(value<score){score=value;best=i;}
            }if(best>=0)return best;
        }
        int smallest=-1;long pixels=Long.MAX_VALUE;for(int i=0;i<sizes.length;i++){int w=sizes[i][0],h=sizes[i][1];long n=(long)w*h;if(w>0&&h>0&&((w|h)&1)==0&&Math.max(w,h)<=maxEdge&&n<pixels){smallest=i;pixels=n;}}
        if(smallest<0)throw new IllegalArgumentException("No compatible preview size");return smallest;
    }
    /** Even-aligned ROI contains the complete desired focal crop, with a one-pixel guard. */
    public static int[] region(int w,int h,float crop){
        if(w<=0||h<=0||((w|h)&1)!=0||!Float.isFinite(crop)||crop<1)throw new IllegalArgumentException("Invalid preview crop");
        int rw=Math.min(w,(((int)Math.ceil(w/(double)crop)+3)/2)*2),rh=Math.min(h,(((int)Math.ceil(h/(double)crop)+3)/2)*2);
        return new int[]{((w-rw)/2)&~1,((h-rh)/2)&~1,rw,rh};
    }
    public static boolean covers(int w,int h,int[] roi,float crop){return roi!=null&&roi.length==4&&roi[0]<=w*(1-1/crop)/2+.1f&&roi[1]<=h*(1-1/crop)/2+.1f&&roi[0]+roi[2]>=w*(1+1/crop)/2-.1f&&roi[1]+roi[3]>=h*(1+1/crop)/2-.1f;}
    /** Maps full-source UVs into the copied ROI; compose after the existing uCrop. */
    public static void transform(float[] m,int w,int h,int[] roi){
        java.util.Arrays.fill(m,0);m[0]=w/(float)roi[2];m[5]=h/(float)roi[3];m[10]=m[15]=1;m[12]=-roi[0]/(float)roi[2];m[13]=-(h-roi[1]-roi[3])/(float)roi[3];
    }
}
