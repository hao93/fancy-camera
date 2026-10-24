package android.opengl;
import java.nio.Buffer;
import java.util.*;
/** Controlled GPU command trace. Not a native GL implementation or performance benchmark. */
public final class GLES20 {
    public static final int GL_TEXTURE0=0,GL_TEXTURE2=2,GL_TEXTURE_2D=3553,GL_VERTEX_SHADER=35633,GL_FRAGMENT_SHADER=35632,GL_COMPILE_STATUS=35713,GL_LINK_STATUS=35714,GL_TEXTURE_MIN_FILTER=10241,GL_TEXTURE_MAG_FILTER=10240,GL_LINEAR=9729,GL_TEXTURE_WRAP_S=10242,GL_TEXTURE_WRAP_T=10243,GL_CLAMP_TO_EDGE=33071,GL_RGBA=6408,GL_UNSIGNED_BYTE=5121,GL_NO_ERROR=0,GL_FLOAT=5126,GL_TRIANGLE_STRIP=5,GL_COLOR_BUFFER_BIT=16384;
    public static int allocations,copies,draws,clears,unit,bound,version=1,presented=-1,error,copyError;private static int next=1;
    private static final Map<Integer,Integer> frames=new HashMap<>();
    public static int glCreateShader(int type){return next++;}public static void glShaderSource(int id,String s){}public static void glCompileShader(int id){}public static void glGetShaderiv(int id,int p,int[] out,int offset){out[offset]=1;}public static void glDeleteShader(int id){}
    public static int glCreateProgram(){return next++;}public static void glAttachShader(int p,int s){}public static void glLinkProgram(int p){}public static void glGetProgramiv(int p,int key,int[] out,int offset){out[offset]=1;}public static void glDeleteProgram(int p){}
    public static void glActiveTexture(int id){unit=id;}public static void glGenTextures(int n,int[] out,int offset){out[offset]=next++;}public static void glBindTexture(int target,int id){bound=id;}public static void glTexParameteri(int a,int b,int c){}
    public static void glTexImage2D(int a,int b,int c,int w,int h,int border,int f,int t,Buffer buffer){allocations++;}
    public static void glCopyTexSubImage2D(int target,int level,int xo,int yo,int x,int y,int w,int h){copies++;if(copyError!=0){error=copyError;return;}frames.put(bound,version);}
    public static int glGetError(){int result=error;error=0;return result;}public static void glViewport(int x,int y,int w,int h){}public static void glUseProgram(int p){}public static int glGetUniformLocation(int p,String name){return 0;}public static void glUniform1i(int u,int v){}public static int glGetAttribLocation(int p,String name){return 0;}public static void glEnableVertexAttribArray(int a){}public static void glVertexAttribPointer(int a,int s,int t,boolean normalized,int stride,Buffer data){}
    public static void glDrawArrays(int mode,int first,int n){draws++;presented=frames.containsKey(bound)?frames.get(bound):-1;}
    public static void glClearColor(float r,float g,float b,float a){}public static void glClear(int mask){clears++;presented=0;}
}
