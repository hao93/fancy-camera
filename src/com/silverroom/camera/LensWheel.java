package com.silverroom.camera;

import android.content.Context;
import java.util.Arrays;

/** Uses the same reel interaction as films, with optical availability gating. */
final class LensWheel extends FilmWheel {
    static final int[] MM={14,23,35,50,85,135,230};
    private boolean[] enabled=new boolean[7];private String[] labels={"检测中","检测中","检测中","检测中","检测中","检测中","检测中"};
    interface Blocked {void lens(int mm);}
    Blocked blocked;
    LensWheel(Context c){super(c);films(Arrays.asList(0,1,2,3,4,5,6),1);}
    void availability(boolean[] next,String[] text,int actual,boolean front){
        enabled=next.clone();labels=text.clone();setEnabled(!front);if(!moving()&&!available(state.selected())&&actual>=0&&enabled[actual])films(Arrays.asList(0,1,2,3,4,5,6),actual);invalidate();
    }
    @Override protected float pitch(){return 72*getResources().getDisplayMetrics().density;}
    @Override String title(int id){return MM[id]+"";}
    @Override String subtitle(int id){return labels[id];}
    @Override boolean available(int id){return enabled[id];}
    @Override String category(){return "焦段";}
    @Override boolean compact(){return true;}
    @Override int legalTarget(int index){return LensWheelPolicy.nearest(index,enabled);}
    @Override void tapped(int target){if(!available(target)){if(blocked!=null)blocked.lens(MM[target]);return;}super.tapped(target);}
}
