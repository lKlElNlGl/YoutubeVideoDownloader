package com.lklengl.youtubevideodownloader;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

public final class UpdateFileProvider extends ContentProvider {
    static final String FILE = "app-update.apk";

    static Uri uri(android.content.Context context) {
        return Uri.parse("content://" + context.getPackageName() + ".updates/" + FILE);
    }

    private File verifiedPath(Uri uri) throws FileNotFoundException {
        if (!("/" + FILE).equals(uri.getPath())) throw new FileNotFoundException("Invalid update path");
        File file = new File(new File(getContext().getCacheDir(), "updates"), FILE);
        if (!file.isFile()) throw new FileNotFoundException("Update not downloaded");
        return file;
    }

    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "application/vnd.android.package-archive"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(verifiedPath(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] columns, String selection, String[] args, String sortOrder) {
        try {
            File file = verifiedPath(uri);
            String[] names = columns == null ? new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE} : columns;
            Object[] values = new Object[names.length];
            for (int i=0; i<names.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(names[i])) values[i]=FILE;
                if (OpenableColumns.SIZE.equals(names[i])) values[i]=file.length();
            }
            MatrixCursor cursor = new MatrixCursor(names,1);
            cursor.addRow(values);
            return cursor;
        }catch(FileNotFoundException ex){return null;}
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
