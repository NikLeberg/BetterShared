package ch.nikleberg.bettershared.ms.auth;

import android.app.Activity;
import android.content.Context;

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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import ch.nikleberg.bettershared.R;

public class Auth implements TokenProvider {
    private static Auth INSTANCE = null;

    private Context appContext = null;
    private Activity parentActivity = null;
    private List<String> scopes = null;

    private ISingleAccountPublicClientApplication app = null;
    private IAccount account = null;
    private IAuthenticationResult accessToken = null;

    private Auth() {
    }

    public static synchronized Auth getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Auth();
        }
        return INSTANCE;
    }

    public boolean isAuthenticated() {
        return (null != accessToken);
    }

    public boolean canAuthenticateSilent() {
        return (null != appContext && null != scopes);
    }

    public boolean canAuthenticateInteractive() {
        return (null != parentActivity && canAuthenticateSilent());
    }

    public void setSilentContext(@Nullable Context appContext, @Nullable List<String> scopes) {
        this.appContext = appContext;
        this.scopes = scopes;
    }

    public void setInteractiveContext(@Nullable Context appContext, @Nullable List<String> scopes, @Nullable Activity parentActivity) {
        this.appContext = appContext;
        this.scopes = scopes;
        this.parentActivity = parentActivity;
    }

    public CompletableFuture<Void> authenticateSilent() {
        assert canAuthenticateSilent();

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
                            future.completeExceptionally(exception);
                        }
                    }).build();
            app.acquireTokenSilentAsync(params);
            return future;
        });
    }

    public CompletableFuture<Void> authenticateInteractive() {
        assert canAuthenticateInteractive();

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
        authenticateSilent().join();
        return accessToken;
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
                future.completeExceptionally(exception);
            }
        });
        future.join();
    }
}
