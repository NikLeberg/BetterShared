package ch.nikleberg.bettershared.ms;

import android.util.Log;

import androidx.annotation.NonNull;

import com.microsoft.graph.core.tasks.PageIterator;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.serialization.AdditionalDataHolder;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.ParsableFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class GraphUtils {
    private static final String TAG = GraphUtils.class.getSimpleName();

    private GraphUtils() {
    }

    public static <TEntity extends Parsable> CompletableFuture<TEntity> getAsync(
            @NonNull Supplier<TEntity> supplier
    ) {
        return CompletableFuture.supplyAsync(supplier);
    }

    public static <TEntity extends Parsable, TCollectionPage extends Parsable & AdditionalDataHolder> List<TEntity> getPaged(
            @NonNull GraphServiceClient gc,
            TCollectionPage pageResponse,
            @NonNull List<TEntity> list,
            @NonNull ParsableFactory<TCollectionPage> collectionPageFactory
    ) {
        if (null == pageResponse) {
            return list;
        }
        try {
            PageIterator<TEntity, TCollectionPage> pageIterator =
                    new PageIterator.Builder<TEntity, TCollectionPage>()
                            .client(gc)
                            .collectionPage(pageResponse)
                            .collectionPageFactory(collectionPageFactory)
                            .processPageItemCallback((item) -> {
                                list.add(item);
                                return true;
                            }).build();
            pageIterator.iterate();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
        return list;
    }

    public static <TEntity extends Parsable, TCollectionPage extends Parsable & AdditionalDataHolder> CompletableFuture<List<TEntity>> getPagedAsync(
            @NonNull GraphServiceClient gc,
            CompletableFuture<TCollectionPage> pageResponse,
            @NonNull List<TEntity> list,
            @NonNull ParsableFactory<TCollectionPage> collectionPageFactory
    ) {
        return pageResponse.thenCompose(page -> CompletableFuture.supplyAsync(() -> getPaged(gc, page, list, collectionPageFactory)));
    }

    public static <TEntity extends Parsable, TCollectionPage extends Parsable & AdditionalDataHolder> CompletableFuture<List<TEntity>> getPagedAsync(
            @NonNull GraphServiceClient gc,
            @NonNull Supplier<TCollectionPage> pageSupplier,
            @NonNull List<TEntity> list,
            @NonNull ParsableFactory<TCollectionPage> collectionPageFactory
    ) {
        return getPagedAsync(gc, CompletableFuture.supplyAsync(pageSupplier), list, collectionPageFactory);
    }
}
