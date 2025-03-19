package ch.nikleberg.bettershared.cp;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.ext.SdkExtensions;
import android.provider.CloudMediaProvider;
import android.provider.CloudMediaProviderContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import ch.nikleberg.bettershared.data.ProviderRepository;
import ch.nikleberg.bettershared.gui.MainActivity;

/**
 * A Cloud Media Provider for Microsoft OneDrive (and potentially other MS drive-like products like
 * Sites or OneDrive for Business).
 * <p>
 * This CloudMediaProvider implementation will not show up per default as an available Cloud Media
 * App. This is because Google restricts the possible cloud media providers to a configured
 * allowlist. See: <a href="https://developer.android.com/guide/topics/providers/cloud-media-provider#eligibility">cloud-media-provider#eligibility</a>
 * <p>
 * Thankfully, this can be overruled via <code>adb</code> and <code>device_config</code>. To do so,
 * connect your phone via USB or WiFi to your computer (where you have ADB installed) and:
 * <ol>
 *     <li>start a shell with <code>adb shell</code></li>
 *     <li>get the current configuration with <code>device_config list mediaprovider</code></li>
 *     <li>we can either: extend the allowlist, or disable it</li>
 *         <ul>
 *             <li>to extend, run <code>device_config put mediaprovider allowed_cloud_providers _existing_value_from_above_,ch.nikleberg.bettershared</code></li>
 *             <li>to disable, run <code>device_config put mediaprovider cloud_media_enforce_provider_allowlist false</code></li>
 *         </ul>
 *     </li>
 *     <li>this setting will automatically be reverted by Google, to prevent this, run <code>device_config set_sync_disabled_for_tests persistent</code></li>
 *     <li>if you wish to revert the setting(s), run <code>device_config set_sync_disabled_for_tests none</code> and it will be reverted after reboot</li>
 * </ol>
 * <p>
 * Note so self: While debugging, filtering logcat for:
 * <code>tag:PickerSyncController</code>
 * <code>tag:SelectedMediaPreloader</code>
 * lets you find what went wrong when calling or processing this provider.
 */

public class Provider extends CloudMediaProvider {
    private static final String TAG = Provider.class.getSimpleName();

    private static final long API_TIMEOUT_GET_INFO = 100;
    private static final long API_TIMEOUT_OPEN_PREVIEW = 500;
    private static final long API_TIMEOUT_OPEN_MEDIA = 500;

    private ProviderRepository repo = null;
    private Intent configIntent = null;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate");
        repo = new ProviderRepository(requireContext());
        configIntent = MainActivity.getLaunchIntent(requireContext());
        return true;
    }

    @NonNull
    @Override
    public Bundle onGetMediaCollectionInfo(@NonNull Bundle extras) {
        logMethodCall("onGetMediaCollectionInfo", extras);

        try {
            loginOrThrow(API_TIMEOUT_GET_INFO);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Could not log in", e);
        }

        Bundle info = new Bundle();
        info.putString(CloudMediaProviderContract.MediaCollectionInfo.MEDIA_COLLECTION_ID, repo.getMediaCollectionId());
        info.putLong(CloudMediaProviderContract.MediaCollectionInfo.LAST_MEDIA_SYNC_GENERATION, repo.getSyncGeneration());
        info.putString(CloudMediaProviderContract.MediaCollectionInfo.ACCOUNT_NAME, repo.getAccountName());
        info.putParcelable(CloudMediaProviderContract.MediaCollectionInfo.ACCOUNT_CONFIGURATION_INTENT, configIntent);
        logMethodCall("onGetMediaCollectionInfo", info);

        return info;
    }

    @NonNull
    @Override
    public Cursor onQueryAlbums(@NonNull Bundle extras) {
        // TODO: handle extra CloudMediaProviderContract.EXTRA_SYNC_GENERATION
        // TODO: handle extra CloudMediaProviderContract.EXTRA_PAGE_TOKEN
        // TODO: handle extra CloudMediaProviderContract.EXTRA_PAGE_SIZE
        // TODO: handle extra Intent.EXTRA_MIME_TYPES
        // TODO: handle extra ??? CloudMediaProviderContract.EXTRA_ALBUM_ID
        // TODO: handle extra ??? CloudMediaProviderContract.EXTRA_SIZE_LIMIT_BYTES
        logMethodCall("onQueryAlbums", extras);

        Cursor cursor = repo.getAlbums();
        Bundle bundle = new Bundle();
        bundle.putString(CloudMediaProviderContract.EXTRA_MEDIA_COLLECTION_ID, repo.getMediaCollectionId());
        cursor.setExtras(bundle);
        return cursor;
    }

    @NonNull
    @Override
    public Cursor onQueryMedia(@NonNull Bundle extras) {
        // TODO: handle extra CloudMediaProviderContract.EXTRA_SYNC_GENERATION
        // TODO: handle extra CloudMediaProviderContract.EXTRA_PAGE_TOKEN
        // TODO: handle extra CloudMediaProviderContract.EXTRA_PAGE_SIZE
        logMethodCall("onQueryMedia", extras);

        ArrayList<String> honored = new ArrayList<>();
        long syncGeneration = extras.getLong(CloudMediaProviderContract.EXTRA_SYNC_GENERATION, -1);
        if (-1 != syncGeneration) {
            Log.w(TAG, "onQueryMedia: ignoring sync generation " + syncGeneration);
        }
        String pageToken = extras.getString(CloudMediaProviderContract.EXTRA_PAGE_TOKEN);
        if (null != pageToken) {
            Log.w(TAG, "onQueryMedia: ignoring page token " + pageToken);
        }
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 8) {
            int pageSize = extras.getInt(CloudMediaProviderContract.EXTRA_PAGE_SIZE, -1);
            if (-1 != pageSize) {
                Log.w(TAG, "onQueryMedia: ignoring page size " + pageSize);
            }
        }
        String albumId = extras.getString(CloudMediaProviderContract.EXTRA_ALBUM_ID, null);
        if (null != albumId) {
            honored.add(CloudMediaProviderContract.EXTRA_ALBUM_ID);
        }

        Cursor cursor = (albumId == null) ? repo.getMedias() : repo.getMediasByAlbumId(albumId);

        Bundle bundle = new Bundle();
        bundle.putString(CloudMediaProviderContract.EXTRA_MEDIA_COLLECTION_ID, repo.getMediaCollectionId());
        bundle.putStringArrayList(ContentResolver.EXTRA_HONORED_ARGS, honored);
        cursor.setExtras(bundle);

        return cursor;
    }

    @NonNull
    @Override
    public Cursor onQueryDeletedMedia(@NonNull Bundle extras) {
        logMethodCall("onQueryDeletedMedia", extras);

        Cursor cursor = repo.getDeletedMedias();

        Bundle bundle = new Bundle();
        bundle.putString(CloudMediaProviderContract.EXTRA_MEDIA_COLLECTION_ID, repo.getMediaCollectionId());
        cursor.setExtras(bundle);

        return cursor;
    }

    @NonNull
    @Override
    public AssetFileDescriptor onOpenPreview(@NonNull String mediaId, @NonNull Point size, @Nullable Bundle extras, @Nullable CancellationSignal cancel) throws FileNotFoundException {
        logMethodCall("onOpenPreview", extras);
        loginOrThrow(API_TIMEOUT_OPEN_PREVIEW);

        if (null != extras) {
            if (extras.containsKey(CloudMediaProviderContract.EXTRA_PREVIEW_THUMBNAIL)) {
                boolean isPreviewThumbnail = extras.getBoolean(CloudMediaProviderContract.EXTRA_PREVIEW_THUMBNAIL, false);
                Log.w(TAG, "onOpenPreview: ignoring extra CloudMediaProviderContract.EXTRA_PREVIEW_THUMBNAIL: " + isPreviewThumbnail);
            }
            if (extras.containsKey("android.provider.extra.MEDIASTORE_THUMB")) {
                boolean isMediaStoreThumb = extras.getBoolean("android.provider.extra.MEDIASTORE_THUMB", false);
                Log.w(TAG, "onOpenPreview: ignoring extra CloudMediaProviderContract.EXTRA_MEDIASTORE_THUMB: " + isMediaStoreThumb);
            }
        }

        try {
            return repo.openMediaPreview(mediaId, size.x, size.y, cancel);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "onOpenPreview exit with exception", e);
            throw e;
        }
    }

    @NonNull
    @Override
    public ParcelFileDescriptor onOpenMedia(@NonNull String mediaId, @Nullable Bundle extras, @Nullable CancellationSignal cancel) throws FileNotFoundException {
        logMethodCall("onOpenMedia", extras);
        loginOrThrow(API_TIMEOUT_OPEN_MEDIA);

        return repo.openMedia(mediaId, cancel);
    }

    private void logMethodCall(String name, Bundle extras) {
        if (null == extras) {
            Log.w(TAG, name + ": --no extras--");
        } else {
            extras.keySet();
            Log.w(TAG, name + ": " + extras);
        }
    }

    private void loginOrThrow(long timeout) throws IllegalStateException {
        if (repo.isLoggedIn()) return;
        try {
            repo.loginWithTimeout(timeout);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Login timed out", e);
        }
    }
}
