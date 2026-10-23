package com.silverroom.camera;
import java.util.function.*;

/** Commits pages one at a time; cancellation never removes previously committed output. */
public final class PrintBatch {
    public interface Task<T>{T render(int page)throws Exception;void save(T output,int page)throws Exception;void release(T output);}
    public static final class Report {public final int completed;public final boolean cancelled;public final String error;Report(int n,boolean c,String e){completed=n;cancelled=c;error=e;}}
    public static <T> Report run(int pages,Task<T> task,BooleanSupplier stop,IntConsumer progress){int count=0;try{for(int page=0;page<pages;page++){if(stop.getAsBoolean())return new Report(count,true,"");T image=task.render(page);try{if(stop.getAsBoolean())return new Report(count,true,"");task.save(image,page);count++;progress.accept(count);}finally{task.release(image);}}return new Report(count,false,"");}catch(Exception|OutOfMemoryError failure){boolean cancelled=stop.getAsBoolean()||failure instanceof java.io.InterruptedIOException;return new Report(count,cancelled,cancelled?"":failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage());}}
    private PrintBatch(){}
}
