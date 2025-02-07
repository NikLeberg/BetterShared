package ch.nikleberg.bettershared.db;

import java.util.List;

import ch.nikleberg.bettershared.db.AlbumDataSource.Error;

public interface AlbumObserver {
    default void onAlbumLoad(List<Album> albums) {
        for (Album album : albums) {
            onAlbumAdded(album);
        }
    }

    void onAlbumAdded(Album album);

    default void onAlbumChanged(Album album) {
        onAlbumRemoved(album);
        onAlbumAdded(album);
    }

    void onAlbumRemoved(Album album);

    default void onAlbumError(Error error, Album album) {
        throw new RuntimeException("onAlbumError called with error: " + error.name());
    }
}