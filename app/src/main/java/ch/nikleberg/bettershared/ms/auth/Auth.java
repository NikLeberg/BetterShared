package ch.nikleberg.bettershared.ms.auth;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SignInParameters;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import ch.nikleberg.bettershared.R;

public class Auth implements TokenProvider {
    private static final HashMap<Integer, Auth> INSTANCES = new HashMap<>();

    private final Context appContext;
    private final List<String> scopes;

    private ISingleAccountPublicClientApplication app = null;
    private IAccount account = null;
    private IAuthenticationResult accessToken = null;

    private Auth(Context context, List<String> scopes) {
        this.appContext = context;
        this.scopes = scopes;
    }

    public static synchronized Auth getInstance(Context context, List<String> scopes) {
        int key = scopes.hashCode();
        if (INSTANCES.containsKey(key)) {
            return INSTANCES.get(key);
        } else {
            Auth instance = new Auth(context, scopes);
            INSTANCES.put(key, instance);
            return instance;
        }
    }

    public boolean isAuthenticated() {
        return (null != accessToken);
    }

    public CompletableFuture<Void> authenticateSilent() {
        return CompletableFuture.supplyAsync(() -> {
            createApp();
            loadAccount();
            return null;
        }).thenCompose(v -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            final AcquireTokenSilentParameters params = new AcquireTokenSilentParameters.Builder()
                    .forAccount(account)
                    .fromAuthority(account.getAuthority())
                    .withScopes(scopes)
                    .withCallback(new SilentAuthenticationCallback() {
                        @Override
                        public void onSuccess(IAuthenticationResult authenticationResult) {
                            accessToken = authenticationResult;
                            future.complete(null);
                        }

                        @Override
                        public void onError(MsalException exception) {
                            Log.d("Auth", exception.getMessage());
                            future.completeExceptionally(exception);
                        }
                    }).build();
            app.acquireTokenSilentAsync(params);
            return future;
        });
    }

    public CompletableFuture<Void> authenticateInteractive(Activity parentActivity) {
        return CompletableFuture.supplyAsync(() -> {
            createApp();
            return null;
        }).thenCompose(v -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            final SignInParameters params = SignInParameters.builder()
                    .withActivity(parentActivity)
                    .withScopes(scopes)
                    .withCallback(new AuthenticationCallback() {
                        @Override
                        public void onCancel() {
                            future.completeExceptionally(new RuntimeException("User cancelled"));
                        }

                        @Override
                        public void onSuccess(IAuthenticationResult authenticationResult) {
                            accessToken = authenticationResult;
                            future.complete(null);
                        }

                        @Override
                        public void onError(MsalException exception) {
                            Log.d("Auth", exception.getMessage());
                            future.completeExceptionally(exception);
                        }
                    }).build();
            app.signIn(params);
            return future;
        }).thenApplyAsync(v -> {
            loadAccount();
            return null;
        });
    }

    public IAccount getAuthenticatedUser() {
        return account;
    }

    @Override
    public IAuthenticationResult getAccessToken() {
        if (isTokenExpired())
            authenticateSilent().join();
        return accessToken;
    }

    private boolean isTokenExpired() {
        return (null == accessToken) || (accessToken.getExpiresOn().before(new Date()));
    }

    private void createApp() {
        if (null != app) return;

        CompletableFuture<Void> future = new CompletableFuture<>();
        PublicClientApplication.createSingleAccountPublicClientApplication(appContext,
                R.raw.msal_config,
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        app = application;
                        future.complete(null);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Log.d("Auth", exception.getMessage());
                        future.completeExceptionally(exception);
                    }
                });
        future.join();
    }

    private void loadAccount() {
        if (null != account) return;
        assert app != null;

        CompletableFuture<Void> future = new CompletableFuture<>();
        app.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                account = activeAccount;
                if (null != activeAccount) {
                    future.complete(null);
                } else {
                    future.completeExceptionally(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT));
                }
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                // TODO: handle logout?
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Log.d("Auth", exception.getMessage());
                future.completeExceptionally(exception);
            }
        });
        future.join();
    }
}
