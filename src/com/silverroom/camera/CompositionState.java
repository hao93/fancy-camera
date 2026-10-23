package com.silverroom.camera;

public final class CompositionState {
    public boolean on,level,cross;public int lines=1;
    public boolean visible(){return on&&(level||cross||lines>0);}
    public void quick(){if(visible()){on=false;return;}if(!level&&!cross&&lines==0)lines=1;on=true;}
    public static int safeLines(int value){return Math.max(0,Math.min(3,value));}
}
