package com.silverroom.camera;

import android.graphics.Bitmap;
import android.opengl.*;
import java.nio.*;

/** Small popup-only FBO. The main framebuffer and photo pipeline remain untouched. */
final class PreviewReadback {
    private int externalProgram,yuvProgram,fbo,color,width,height;private ByteBuffer bytes;
    private final FloatBuffer quad=ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final java.util.Map<Integer,int[]> uniforms=new java.util.HashMap<>();
    private final GrainTexture grain;
    PreviewReadback(GrainTexture shared){grain=shared;quad.put(new float[]{-1,-1,0,0,1,-1,1,0,-1,1,0,1,1,1,1,1}).position(0);}
    private int shader(int type,String code){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,code);GLES20.glCompileShader(s);int[] ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0){String reason=GLES20.glGetShaderInfoLog(s);GLES20.glDeleteShader(s);throw new IllegalStateException(reason);}return s;}
    private int program(String fragment){int vertex=shader(GLES20.GL_VERTEX_SHADER,CameraShaders.VERTEX),pixel=shader(GLES20.GL_FRAGMENT_SHADER,fragment),p=GLES20.glCreateProgram();GLES20.glAttachShader(p,vertex);GLES20.glAttachShader(p,pixel);GLES20.glLinkProgram(p);GLES20.glDeleteShader(vertex);GLES20.glDeleteShader(pixel);int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0){GLES20.glDeleteProgram(p);throw new IllegalStateException("Preview program link failed");}return p;}
    private void target(int w,int h){
        if(fbo!=0&&bytes!=null&&width==w&&height==h)return;releaseTarget();width=w;height=h;int[] id=new int[1];
        GLES20.glGenTextures(1,id,0);color=id[0];GLES20.glActiveTexture(GLES20.GL_TEXTURE2);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,color);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,w,h,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null);
        GLES20.glGenFramebuffers(1,id,0);fbo=id[0];GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo);GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,color,0);if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)!=GLES20.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Preview target unsupported");bytes=ByteBuffer.allocateDirect(w*h*4);
    }
    Bitmap read(boolean yuv,int external,int[] planes,float[] model,float[] transform,float crop,int vw,int vh){
        int[] size=PreviewPixels.size(vw,vh);
        try{
            if(yuv&&yuvProgram==0)yuvProgram=program(CameraShaders.RAW_YUV);if(!yuv&&externalProgram==0)externalProgram=program(CameraShaders.RAW_EXTERNAL);target(size[0],size[1]);
            int p=yuv?yuvProgram:externalProgram;GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo);GLES20.glViewport(0,0,width,height);GLES20.glClearColor(0,0,0,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);GLES20.glUseProgram(p);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);if(yuv){GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,planes[0]);GLES20.glActiveTexture(GLES20.GL_TEXTURE1);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,planes[1]);GLES20.glUniform1i(GLES20.glGetUniformLocation(p,"uY"),0);GLES20.glUniform1i(GLES20.glGetUniformLocation(p,"uVU"),1);}else{GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,external);GLES20.glUniform1i(GLES20.glGetUniformLocation(p,"uTex"),0);}
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(p,"uM"),1,false,model,0);GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(p,"uST"),1,false,transform,0);GLES20.glUniform1f(GLES20.glGetUniformLocation(p,"uCrop"),crop);
            quad.position(0);int ap=GLES20.glGetAttribLocation(p,"aP");GLES20.glEnableVertexAttribArray(ap);GLES20.glVertexAttribPointer(ap,2,GLES20.GL_FLOAT,false,16,quad);quad.position(2);int at=GLES20.glGetAttribLocation(p,"aT");GLES20.glEnableVertexAttribArray(at);GLES20.glVertexAttribPointer(at,2,GLES20.GL_FLOAT,false,16,quad);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
            bytes.clear();GLES20.glReadPixels(0,0,width,height,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,bytes);int error=GLES20.glGetError();if(error!=GLES20.GL_NO_ERROR)throw new IllegalStateException("Preview read failed: "+error);bytes.position(0);return Bitmap.createBitmap(PreviewPixels.argb(bytes,width,height),width,height,Bitmap.Config.ARGB_8888);
        }finally{GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);GLES20.glViewport(0,0,vw,vh);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);}
    }
    PreviewAtlas atlas(boolean yuv,int program,int external,int[] planes,float[] model,float[] transform,float crop,int vw,int vh,int[] ids,Film.Settings base,int edge,long captured){
        int[] tile=PreviewAtlas.layout(vw,vh,ids.length,edge);int columns=tile[2],rows=(ids.length+columns-1)/columns;
        try{
            target(tile[0]*columns,tile[1]*rows);GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo);GLES20.glClearColor(0,0,0,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);GLES20.glUseProgram(program);grain.bind(program);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);if(yuv){GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,planes[0]);GLES20.glActiveTexture(GLES20.GL_TEXTURE1);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,planes[1]);GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"uY"),0);GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"uVU"),1);}else{GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,external);GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"uTex"),0);}
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program,"uM"),1,false,model,0);GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program,"uST"),1,false,transform,0);GLES20.glUniform1f(GLES20.glGetUniformLocation(program,"uCrop"),crop);
            quad.position(0);int ap=GLES20.glGetAttribLocation(program,"aP");GLES20.glEnableVertexAttribArray(ap);GLES20.glVertexAttribPointer(ap,2,GLES20.GL_FLOAT,false,16,quad);quad.position(2);int at=GLES20.glGetAttribLocation(program,"aT");GLES20.glEnableVertexAttribArray(at);GLES20.glVertexAttribPointer(at,2,GLES20.GL_FLOAT,false,16,quad);
            int[] u=uniforms.get(program);if(u==null){String[] names={"uGain","uLift","uTone","uShadow","uHighlight","uShoulder","uPost","uFx","uMix","uQuality"};u=new int[names.length];for(int i=0;i<u.length;i++)u[i]=GLES20.glGetUniformLocation(program,names[i]);uniforms.put(program,u);}
            for(int i=0;i<ids.length;i++){Film.Settings variant=base.copy();variant.stock=ids[i];float[] p=Film.parameters(variant);GLES20.glUniform3f(u[0],p[0],p[1],p[2]);GLES20.glUniform3f(u[1],p[3],p[4],p[5]);GLES20.glUniform4f(u[2],p[6],p[7],p[8],p[9]);GLES20.glUniform3f(u[3],p[10],p[11],p[12]);GLES20.glUniform3f(u[4],p[13],p[14],p[15]);GLES20.glUniform1f(u[5],p[16]);GLES20.glUniform3f(u[6],p[17],p[18],p[19]);GLES20.glUniform4f(u[7],p[20],p[21],p[22],p[23]);GLES20.glUniform2f(u[8],FilmStrength.clamp(base.strength),FilmStrength.exposure(base.exposure));GLES20.glUniform2f(u[9],ImageQuality.clamp(base.skin,0,1),base.adaptiveGrain?1:0);GLES20.glViewport(i%columns*tile[0],i/columns*tile[1],tile[0],tile[1]);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);}
            bytes.clear();GLES20.glReadPixels(0,0,width,height,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,bytes);if(GLES20.glGetError()!=GLES20.GL_NO_ERROR)throw new IllegalStateException("Atlas read failed");byte[] copy=new byte[width*height*4];bytes.position(0);bytes.get(copy);return new PreviewAtlas(tile[0],tile[1],ids,copy,captured);
        }finally{GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);GLES20.glViewport(0,0,vw,vh);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);}
    }
    void releaseTarget(){if(fbo!=0)GLES20.glDeleteFramebuffers(1,new int[]{fbo},0);if(color!=0)GLES20.glDeleteTextures(1,new int[]{color},0);fbo=color=0;bytes=null;}
}



