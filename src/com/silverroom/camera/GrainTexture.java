package com.silverroom.camera;

import android.opengl.GLES20;
import java.nio.ByteBuffer;

/** One 256KB texture per EGL context; units 0/1 are source planes, unit 2 is the atlas. */
final class GrainTexture {
    private int texture;
    void bind(int program){
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        if(texture==0){int[] name=new int[1];GLES20.glGenTextures(1,name,0);texture=name[0];GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_NEAREST);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT);byte[] pixels=PreviewGrain.pixels();ByteBuffer upload=ByteBuffer.allocateDirect(pixels.length);upload.put(pixels).position(0);GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT,1);GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_LUMINANCE,PreviewGrain.EDGE,PreviewGrain.EDGE,0,GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE,upload);}
        else GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"uGrain"),3);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    }
}
