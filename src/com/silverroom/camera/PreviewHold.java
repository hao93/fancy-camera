package com.silverroom.camera;

import android.opengl.GLES20;
import java.nio.*;

/** GPU-only last rendered frame. A camera close never discards it; EGL recreation does. */
final class PreviewHold {
    static final String VERTEX="attribute vec2 aP;varying vec2 vT;void main(){gl_Position=vec4(aP,0.,1.);vT=aP*.5+.5;}";
    static final String FRAGMENT="precision mediump float;uniform sampler2D uFrame;uniform float uOpacity;varying vec2 vT;void main(){vec4 c=texture2D(uFrame,vT);gl_FragColor=vec4(c.rgb,c.a*uOpacity);}";
    private int texture,program,width,height;private boolean valid;
    private final FloatBuffer quad=ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer();
    PreviewHold(){quad.put(new float[]{-1,-1,1,-1,-1,1,1,1}).position(0);}
    void clear(){valid=false;}
    private int shader(int type,String source){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,source);GLES20.glCompileShader(s);int[] ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0){GLES20.glDeleteShader(s);throw new IllegalStateException("Hold shader");}return s;}
    private void prepare(){if(program!=0)return;int v=shader(GLES20.GL_VERTEX_SHADER,VERTEX),f=shader(GLES20.GL_FRAGMENT_SHADER,FRAGMENT);int p=GLES20.glCreateProgram();GLES20.glAttachShader(p,v);GLES20.glAttachShader(p,f);GLES20.glLinkProgram(p);GLES20.glDeleteShader(v);GLES20.glDeleteShader(f);int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0){GLES20.glDeleteProgram(p);throw new IllegalStateException("Hold link");}program=p;}
    void save(int w,int h){
        if(w<=0||h<=0||w>4096||h>4096){valid=false;return;}
        try{for(int i=0;i<8&&GLES20.glGetError()!=GLES20.GL_NO_ERROR;i++){}GLES20.glActiveTexture(GLES20.GL_TEXTURE2);if(texture==0){int[] id=new int[1];GLES20.glGenTextures(1,id,0);texture=id[0];}GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
            if(w!=width||h!=height){valid=false;width=w;height=h;GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,w,h,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null);}
            GLES20.glCopyTexSubImage2D(GLES20.GL_TEXTURE_2D,0,0,0,0,0,w,h);if(GLES20.glGetError()==GLES20.GL_NO_ERROR)valid=true;else if(!valid){width=height=0;}
        }finally{GLES20.glActiveTexture(GLES20.GL_TEXTURE0);}
    }
    boolean draw(int w,int h){return draw(w,h,1);}
    boolean draw(int w,int h,float opacity){
        if(!valid||width!=w||height!=h)return false;
        return drawTexture(texture,w,h,opacity);
    }
    boolean drawTexture(int source,int w,int h,float opacity){
        if(source==0||w<=0||h<=0)return false;
        try{for(int i=0;i<8&&GLES20.glGetError()!=GLES20.GL_NO_ERROR;i++){}prepare();GLES20.glViewport(0,0,w,h);GLES20.glUseProgram(program);GLES20.glActiveTexture(GLES20.GL_TEXTURE2);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,source);GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"uFrame"),2);GLES20.glUniform1f(GLES20.glGetUniformLocation(program,"uOpacity"),Math.max(0,Math.min(1,opacity)));if(opacity<1){GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);}quad.position(0);int a=GLES20.glGetAttribLocation(program,"aP");GLES20.glEnableVertexAttribArray(a);GLES20.glVertexAttribPointer(a,2,GLES20.GL_FLOAT,false,0,quad);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);return GLES20.glGetError()==GLES20.GL_NO_ERROR;}
        catch(RuntimeException failure){valid=false;return false;}finally{GLES20.glDisable(GLES20.GL_BLEND);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);}
    }
}
