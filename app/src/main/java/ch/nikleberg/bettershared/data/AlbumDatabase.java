package ch.nikleberg.bettershared.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Album.class}, version = 1)
public abstract class AlbumDatabase extends RoomDatabase {
    public abstract AlbumDao albumDao();

    private static volatile AlbumDatabase INSTANCE;

    public static synchronized AlbumDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.inMemoryDatabaseBuilder(context, AlbumDatabase.class)
                    .build();
            //INSTANCE = Room.databaseBuilder(context, AlbumDatabase.class, "album_database")
            //        .build();
        }
        return INSTANCE;
    }
}