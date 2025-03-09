package ch.nikleberg.bettershared.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Album.class, Media.class},
        views = {CloudProviderViews.AlbumView.class, CloudProviderViews.MediaView.class},
        version = 10)
public abstract class RoomsDB extends RoomDatabase {
    public abstract AlbumDao albumDao();

    public abstract MediaDao mediaDao();

    private static volatile RoomsDB INSTANCE;

    public static synchronized RoomsDB getDatabase(final Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context, RoomsDB.class, "database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }
}