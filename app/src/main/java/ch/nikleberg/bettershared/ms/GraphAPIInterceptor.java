package ch.nikleberg.bettershared.ms;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.Interceptor;
import okhttp3.Response;
import okio.Buffer;

public class GraphAPIInterceptor implements Interceptor {
    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Request: %s %s%n", chain.request().method(), chain.request().url()));
        sb.append(String.format("Request headers:%n"));
        chain.request().headers().toMultimap()
                .forEach((k, v) -> sb.append(String.format("%s: %s%n", k, String.join(", ", v))));
        if (chain.request().body() != null) {
            sb.append(String.format("Request body:%n"));
            final Buffer buffer = new Buffer();
            chain.request().body().writeTo(buffer);
            sb.append(String.format("%s%n", buffer.readString(StandardCharsets.UTF_8)));
        }

        final Response response = chain.proceed(chain.request());

        sb.append(String.format("%n"));
        sb.append(String.format("Response: %s%n", response.code()));
        sb.append(String.format("Response headers:%n"));
        response.headers().toMultimap()
                .forEach((k, v) -> sb.append(String.format("%s: %s%n", k, String.join(", ", v))));
        if (response.body() != null) {
            sb.append(String.format("Response body:%n"));
            sb.append(String.format("%s%n", response.peekBody(Long.MAX_VALUE).string()));
        }

        Log.d("GraphAPIInterceptor", sb.toString());

        return response;
    }
}
