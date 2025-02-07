package ch.nikleberg.bettershared.ms;

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

    private DriveUtils() {
    }

    public static CompletableFuture<List<Drive>> getDrives(GraphServiceClient graph) {
        return GraphUtils.getPagedAsync(graph, () -> graph.drives().get(request -> {
            assert request.queryParameters != null;
            //request.queryParameters.select = new String[]{"id", "name", "driveType"};
            request.queryParameters.top = 4;
        }), new ArrayList<>(), DriveCollectionResponse::createFromDiscriminatorValue);
    }

    public static CompletableFuture<DriveItem> getDriveItem(GraphServiceClient graph, String driveId, String itemId) {
        return GraphUtils.getAsync(() -> graph.drives().byDriveId(driveId).items().byDriveItemId(itemId).get());
    }

    public static CompletableFuture<List<DriveItem>> getDriveItems(GraphServiceClient graph, String driveId, String itemId) {
        return GraphUtils.getPagedAsync(graph, () -> graph.drives().byDriveId(driveId).items().byDriveItemId(itemId).children().get(request -> {
            assert request.queryParameters != null;
            //request.queryParameters.select = new String[]{"id", "name", "file", "folder"};
            request.queryParameters.top = 4;
        }), new ArrayList<>(), DriveItemCollectionResponse::createFromDiscriminatorValue);
    }

    public static CompletableFuture<List<DriveItem>> getDriveItems(GraphServiceClient graph, String driveId) {
        return getDriveItems(graph, driveId, "root");
    }
}
