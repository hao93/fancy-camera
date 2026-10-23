package com.silverroom.camera;

/** Frozen at preview/export submission, never read back from changing UI. */
public final class PrintOptions {
    public final int style;public final boolean contact,date,focal,film;
    public PrintOptions(int s,boolean c,boolean d,boolean f,boolean n){style=Math.max(0,Math.min(2,s));contact=c;date=d;focal=f;film=n;}
    public int pages(int photos){return photos<=0?0:contact?(photos+8)/9:photos;}
    public int first(int page){return contact?page*9:page;}
    public int count(int page,int total){return Math.max(0,Math.min(contact?9:1,total-first(page)));}
}
