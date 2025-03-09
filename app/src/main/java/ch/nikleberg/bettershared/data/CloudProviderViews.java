package ch.nikleberg.bettershared.data;

import android.provider.CloudMediaProviderContract;

import androidx.room.DatabaseView;

/**
 * Create table views that conform to the requirements in <code>CloudMediaProviderContract</code>.
 * <p>
 * The contract requires <code>id</code> columns of type String. But strings can't be
 * auto-incremented by SQL/room. So the actual tables have a long-typed <code>_id</code>
 * column that the following view translates to a string id with "ID_" + _id.
 * <p>
 * For the media table, some required but not implemented columns are "faked" with constant values.
 * <p>
 *
 * @apiNote These classes are not meant to be instantiated.
 */
public class CloudProviderViews {
    // "Fake" the String-typed "id" column as required by CloudMediaProviderContract.
    @DatabaseView("SELECT 'ID_' || _id AS " + CloudMediaProviderContract.AlbumColumns.ID
            + ", " + CloudMediaProviderContract.AlbumColumns.DISPLAY_NAME
            + ", " + CloudMediaProviderContract.AlbumColumns.DATE_TAKEN_MILLIS
            + ", " + CloudMediaProviderContract.AlbumColumns.MEDIA_COUNT
            + ", " + CloudMediaProviderContract.AlbumColumns.MEDIA_COVER_ID
            + " FROM album"
            + " ORDER BY " + CloudMediaProviderContract.AlbumColumns.DATE_TAKEN_MILLIS + " DESC")
    static class AlbumView {
        public AlbumView() {
            throw new AssertionError("this class is not meant to be instantiated");
        }
    }

    // "Fake" additional columns as required by CloudMediaProviderContract.
    @DatabaseView("SELECT 'ID_' || _id AS " + CloudMediaProviderContract.MediaColumns.ID
            + ", " + CloudMediaProviderContract.MediaColumns.SIZE_BYTES
            + ", " + CloudMediaProviderContract.MediaColumns.DATE_TAKEN_MILLIS
            + ", " + CloudMediaProviderContract.MediaColumns.MIME_TYPE
            + ", " + CloudMediaProviderContract.MediaColumns.STANDARD_MIME_TYPE_EXTENSION_NONE + " AS " + CloudMediaProviderContract.MediaColumns.STANDARD_MIME_TYPE_EXTENSION
            + ", " + CloudMediaProviderContract.MediaColumns.HEIGHT
            + ", " + CloudMediaProviderContract.MediaColumns.WIDTH
            + ", " + CloudMediaProviderContract.MediaColumns.ORIENTATION
            + ", " + CloudMediaProviderContract.MediaColumns.DURATION_MILLIS
            + ", " + CloudMediaProviderContract.MediaColumns.SYNC_GENERATION
            + ", 0 AS " + CloudMediaProviderContract.MediaColumns.IS_FAVORITE
            + ", NULL AS " + CloudMediaProviderContract.MediaColumns.MEDIA_STORE_URI
            + ", album_id" // required to filter for per-album media
            + " FROM media"
            + " ORDER BY " + CloudMediaProviderContract.MediaColumns.DATE_TAKEN_MILLIS + " DESC")
    static class MediaView {
        public MediaView() {
            throw new AssertionError("this class is not meant to be instantiated");
        }
    }

    private CloudProviderViews() {
    }

    public static long stringIdToLongId(String id) {
        if (null == id) return -1L;
        // transform cloud provider specific string-typed id back to long-typed id.
        String stripped = id.replaceFirst("^ID_", "");
        return Long.parseLong(stripped);
    }
}
