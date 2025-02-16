package ch.nikleberg.bettershared.ms;

import androidx.annotation.Nullable;

import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveCollectionResponse;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DriveUtils {
    public static final List<String> DRIVE_SCOPES = List.of("Files.Read.All");
    public static final List<String> SELECT_DRIVES = List.of("id", "driveType");
    public static final List<String> SELECT_DRIVE_ITEMS_FOLDERS = List.of("id", "name", "folder");
    public static final List<String> SELECT_DRIVE_ITEMS_FILES = List.of("id", "name", "file");
    public static final List<String> SELECT_DRIVE_ITEM_FOLDER = List.of("id", "name", "folder", "parentReference");

    private DriveUtils() {
    }

    public static CompletableFuture<List<Drive>> getDrives(GraphServiceClient graph, @Nullable List<String> select) {
        return GraphUtils.getPagedAsync(graph, () -> graph.drives().get(request -> {
            if (null != select) {
                assert request.queryParameters != null;
                request.queryParameters.select = select.toArray(new String[0]);
            }
        }), new ArrayList<>(), DriveCollectionResponse::createFromDiscriminatorValue);
    }

    public static CompletableFuture<List<Drive>> getDrives(GraphServiceClient graph) {
        return getDrives(graph, null);
    }

    public static CompletableFuture<DriveItem> getDriveItem(GraphServiceClient graph, String driveId, String itemId, @Nullable List<String> select) {
        return GraphUtils.getAsync(() -> graph.drives().byDriveId(driveId).items().byDriveItemId(itemId).get());
    }

    public static CompletableFuture<DriveItem> getDriveItem(GraphServiceClient graph, String driveId, String itemId) {
        return getDriveItem(graph, driveId, itemId, null);
    }

    public static CompletableFuture<List<DriveItem>> getDriveItems(GraphServiceClient graph, String driveId, String itemId, @Nullable List<String> select) {
        return GraphUtils.getPagedAsync(graph, () -> graph.drives().byDriveId(driveId).items().byDriveItemId(itemId).children().get(request -> {
            if (null != select) {
                assert request.queryParameters != null;
                request.queryParameters.select = select.toArray(new String[0]);
            }
        }), new ArrayList<>(), DriveItemCollectionResponse::createFromDiscriminatorValue);
    }

    public static CompletableFuture<List<DriveItem>> getDriveItems(GraphServiceClient graph, String driveId, String itemId) {
        return getDriveItems(graph, driveId, itemId, null);
    }
}
