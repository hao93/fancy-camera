package com.silverroom.camera;

import android.opengl.GLES20;

/** Two owned render targets: an unfinished frame can never replace the last complete one. */
final class PreviewFramebuffer {
    private static final class Slot {int texture,fbo,width,height;boolean valid;}
    private Slot front=new Slot(),back=new Slot();
    private final PreviewHold presenter=new PreviewHold();
    private boolean writing;private int savedFramebuffer;private final int[] savedViewport=new int[4];
    private String problem="";
    private long committed,held;
    String problem(){return problem;}
    long committed(){return committed;}long held(){return held;}
    void clear(){abort();front.valid=back.valid=false;}
    private void drain(){for(int n=0;n<8&&GLES20.glGetError()!=GLES20.GL_NO_ERROR;n++){} }
    private void allocate(Slot slot,int w,int h){
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        int[] name=new int[1];if(slot.texture==0){GLES20.glGenTextures(1,name,0);slot.texture=name[0];}
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,slot.texture);
        if(slot.width!=w||slot.height!=h){
            slot.valid=false;slot.width=w;slot.height=h;
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,w,h,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null);
        }
        if(slot.fbo==0){GLES20.glGenFramebuffers(1,name,0);slot.fbo=name[0];}
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,slot.fbo);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,slot.texture,0);
        if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)!=GLES20.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("预览缓冲不完整");
        int error=GLES20.glGetError();if(error!=GLES20.GL_NO_ERROR){slot.width=slot.height=0;throw new IllegalStateException("预览缓冲分配 GL "+error);}
    }
    boolean begin(int w,int h){
        if(w<=0||h<=0||w>4096||h>4096){problem="预览缓冲尺寸不支持 "+w+"×"+h;return false;}
        abort();drain();int[] saved=new int[1];GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING,saved,0);savedFramebuffer=saved[0];GLES20.glGetIntegerv(GLES20.GL_VIEWPORT,savedViewport,0);writing=true;
        try{allocate(back,w,h);GLES20.glViewport(0,0,w,h);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);return true;}
        catch(RuntimeException e){problem=e.getMessage();back.width=back.height=0;abort();return false;}
    }
    boolean commit(){
        if(!writing)return false;int error=GLES20.glGetError();
        if(error!=GLES20.GL_NO_ERROR){problem="预览帧绘制 GL "+error;abort();return false;}
        restore();back.valid=true;Slot previous=front;front=back;back=previous;committed++;problem="";return true;
    }
    private void restore(){GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,savedFramebuffer);GLES20.glViewport(savedViewport[0],savedViewport[1],savedViewport[2],savedViewport[3]);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);writing=false;}
    void abort(){if(writing)restore();}
    boolean draw(int w,int h){
        if(!front.valid||front.width!=w||front.height!=h)return false;
        boolean drawn=presenter.drawTexture(front.texture,w,h,1);if(!drawn)problem="预览缓冲显示失败";else held++;return drawn;
    }
}
