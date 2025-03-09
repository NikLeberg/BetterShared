package ch.nikleberg.bettershared.ms;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.microsoft.graph.core.requests.GraphClientFactory;
import com.microsoft.graph.core.tasks.PageIterator;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import com.microsoft.kiota.serialization.AdditionalDataHolder;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.ParsableFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class GraphUtils {
    private static final String TAG = GraphUtils.class.getSimpleName();

    private GraphUtils() {
    }

    public static <TEntity extends Parsable> CompletableFuture<TEntity> getAsync(
            @NonNull Supplier<TEntity> supplier
    ) {
        return CompletableFuture.supplyAsync(supplier);
    }

    public static <TEntity extends Parsable, TCollectionPage extends Parsable & AdditionalDataHolder> Pair<List<TEntity>, String> getPaged(
            @NonNull GraphServiceClient gc,
            TCollectionPage pageResponse,
            @NonNull List<TEntity> list,
            @NonNull ParsableFactory<TCollectionPage> collectionPageFactory
    ) {
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
            String deltaToken = null;
            String deltaLink = pageIterator.getDeltaLink();
            if (null != deltaLink) {
                HttpUrl deltaUrl = HttpUrl.parse(deltaLink);
                deltaToken = (null != deltaUrl) ? deltaUrl.queryParameter("token") : null;
            }
            return Pair.create(list, deltaToken);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
            return Pair.create(list, "");
        }
    }

    public static <TEntity extends Parsable, TCollectionPage extends Parsable & AdditionalDataHolder> CompletableFuture<List<TEntity>> getPagedAsync(
            @NonNull GraphServiceClient gc,
            CompletableFuture<TCollectionPage> pageResponse,
            @NonNull List<TEntity> list,
            @NonNull ParsableFactory<TCollectionPage> collectionPageFactory
    ) {
        return pageResponse.thenCompose(page -> CompletableFuture.supplyAsync(() -> getPaged(gc, page, list, collectionPageFactory).first));
    }

    public static <TEntity extends Parsable, TCollectionPage extends Parsable & AdditionalDataHolder> CompletableFuture<List<TEntity>> getPagedAsync(
            @NonNull GraphServiceClient gc,
            @NonNull Supplier<TCollectionPage> pageSupplier,
            @NonNull List<TEntity> list,
            @NonNull ParsableFactory<TCollectionPage> collectionPageFactory
    ) {
        return getPagedAsync(gc, CompletableFuture.supplyAsync(pageSupplier), list, collectionPageFactory);
    }

    public static <TEntity extends Parsable, TCollectionPage extends Parsable & AdditionalDataHolder> Pair<List<TEntity>, String> getPagedWithDelta(
            @NonNull GraphServiceClient gc,
            TCollectionPage pageResponse,
            @NonNull List<TEntity> list,
            @NonNull ParsableFactory<TCollectionPage> collectionPageFactory
    ) {
        return getPaged(gc, pageResponse, list, collectionPageFactory);
    }

    public static class Factory {
        public static GraphServiceClient getDebugServiceClient(AuthenticationProvider authProvider) {
            final OkHttpClient okHttpClient = GraphClientFactory.create(GraphServiceClient.getGraphClientOptions())
                    .addInterceptor(new GraphAPIInterceptor())
                    .build();
            return new GraphServiceClient(authProvider, okHttpClient);
        }
    }
}
