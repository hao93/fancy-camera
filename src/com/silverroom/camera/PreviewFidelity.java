package com.silverroom.camera;

/** A temporary late-frame downgrade can recover; quality is never stuck at 80px. */
public final class PreviewFidelity {
    private boolean detail;private int edge=PreviewPixels.DEFAULT_EDGE,late,fresh;
    public synchronized int edge(){return edge;}
    public synchronized void mode(boolean large){detail=large;edge=large?PreviewPixels.MAX_EDGE:PreviewPixels.DEFAULT_EDGE;late=fresh=0;}
    public synchronized void late(){fresh=0;if(++late>=2){edge=Math.max(detail?320:192,edge*3/4);late=0;}}
    public synchronized void fresh(){late=0;if(++fresh>=12){edge=Math.min(detail?PreviewPixels.MAX_EDGE:PreviewPixels.DEFAULT_EDGE,edge+32);fresh=0;}}
}
