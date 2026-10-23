package com.silverroom.camera;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Read-only, URI-granted view of exactly one verified private APK; no path supplied by callers. */
public final class UpdateFileProvider extends ContentProvider {
    public static Uri uri(Context c){return Uri.parse("content://"+c.getPackageName()+".updates/apk/pending-update.apk");}
    private File file(Uri uri)throws FileNotFoundException{try{if(!uri(getContext()).equals(uri))throw new IOException("Unknown APK URI");File f=UpdatePolicy.pending(getContext().getCacheDir());if(!f.isFile())throw new IOException("APK not ready");return f;}catch(IOException e){throw new FileNotFoundException(e.getMessage());}}
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){return "application/vnd.android.package-archive";}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(cols);try{File f=file(uri);Object[] row=new Object[cols.length];for(int i=0;i<cols.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(cols[i])?"FancyCamera-update.apk":OpenableColumns.SIZE.equals(cols[i])?f.length():null;cursor.addRow(row);}catch(FileNotFoundException ignored){}return cursor;}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException("Read only");}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException("Read only");}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException("Read only");}
}
