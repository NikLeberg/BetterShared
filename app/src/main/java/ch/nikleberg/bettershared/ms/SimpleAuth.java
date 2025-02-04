package ch.nikleberg.bettershared.ms;

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
import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.authentication.AuthenticationProvider;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ch.nikleberg.bettershared.R;

public class SimpleAuth {
    public final String TAG = SimpleAuth.class.getSimpleName();

    public SimpleAuth(Context appContext, List<String> scopes, Supplier<CompletableFuture<Activity>> getActivity, Consumer<Provider> onAuthenticated, Consumer<Exception> onError) {
        // 1. Create an MSAL application.
        // 2. Load user account.
        // 3. If account could be loaded, authenticate silently.
        // 4. Else, authenticate interactively.
        // TODO: Shall we do the interactive auth when silent auth fails?
        createApp(appContext, app -> loadAccount(app,
                account -> authenticateSilent(app, account, scopes, onAuthenticated, onError),
                ex -> getActivity.get().thenAccept(activity ->
                        authenticateInteractive(app, activity, scopes, onAuthenticated, onError))), onError);
    }

    private void createApp(Context context, Consumer<ISingleAccountPublicClientApplication> onCreated, Consumer<Exception> onError) {
        PublicClientApplication.createSingleAccountPublicClientApplication(context,
                R.raw.msal_config,
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        onCreated.accept(application);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        onError.accept(exception);
                    }
                });
    }

    private void loadAccount(ISingleAccountPublicClientApplication app, Consumer<IAccount> onAccountLoaded, Consumer<Exception> onError) {
        app.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                if (null != activeAccount) {
                    onAccountLoaded.accept(activeAccount);
                } else {
                    onError.accept(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT));
                }
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                // TODO: handle logout?
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                onError.accept(exception);
            }
        });
    }

    private void authenticateSilent(ISingleAccountPublicClientApplication app, IAccount account, List<String> scopes, Consumer<Provider> onAuthenticated, Consumer<Exception> onError) {
        final AcquireTokenSilentParameters params = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.getAuthority())
                .withScopes(scopes)
                .withCallback(new SilentAuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        onAuthenticated.accept(new Provider(authenticationResult.getAccessToken()));
                    }

                    @Override
                    public void onError(MsalException exception) {
                        onError.accept(exception);
                    }
                }).build();
        app.acquireTokenSilentAsync(params);
    }

    private void authenticateInteractive(ISingleAccountPublicClientApplication app, Activity activity, List<String> scopes, Consumer<Provider> onAuthenticated, Consumer<Exception> onError) {
        final SignInParameters params = SignInParameters.builder()
                .withActivity(activity)
                .withScopes(scopes)
                .withCallback(new AuthenticationCallback() {
                    @Override
                    public void onCancel() {
                        onError.accept(new RuntimeException("User cancelled"));
                    }

                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        onAuthenticated.accept(new Provider(authenticationResult.getAccessToken()));
                    }

                    @Override
                    public void onError(MsalException exception) {
                        onError.accept(exception);
                    }
                }).build();
        app.signIn(params);
    }

    public static class Provider implements AuthenticationProvider {
        final private String token;

        public Provider(String token) {
            this.token = token;
        }

        @Override
        public void authenticateRequest(RequestInformation request, Map<String, Object> additionalAuthenticationContext) {
            try {
                if (null != request.getUri()) Log.d("SimpleAuth.Provider", request.getUri().toString());
            } catch (URISyntaxException ignore) {
            }
            request.headers.add("Authorization", "Bearer " + token);
        }
    }
}