package ch.nikleberg.bettershared.cp;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.CloudMediaProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileNotFoundException;

public class Provider extends CloudMediaProvider {
    /**
     * @param bundle
     * @return
     */
    @NonNull
    @Override
    public Bundle onGetMediaCollectionInfo(@NonNull Bundle bundle) {
        return null;
    }

    /**
     * @param bundle
     * @return
     */
    @NonNull
    @Override
    public Cursor onQueryMedia(@NonNull Bundle bundle) {
        return null;
    }

    /**
     * @param bundle
     * @return
     */
    @NonNull
    @Override
    public Cursor onQueryDeletedMedia(@NonNull Bundle bundle) {
        return null;
    }

    /**
     * @param s
     * @param point
     * @param bundle
     * @param cancellationSignal
     * @return
     * @throws FileNotFoundException
     */
    @NonNull
    @Override
    public AssetFileDescriptor onOpenPreview(@NonNull String s, @NonNull Point point, @Nullable Bundle bundle, @Nullable CancellationSignal cancellationSignal) throws FileNotFoundException {
        throw new FileNotFoundException();
    }

    /**
     * @param s
     * @param bundle
     * @param cancellationSignal
     * @return
     * @throws FileNotFoundException
     */
    @NonNull
    @Override
    public ParcelFileDescriptor onOpenMedia(@NonNull String s, @Nullable Bundle bundle, @Nullable CancellationSignal cancellationSignal) throws FileNotFoundException {
        throw new FileNotFoundException();
    }

    /**
     * @return
     */
    @Override
    public boolean onCreate() {
        return false;
    }
}
