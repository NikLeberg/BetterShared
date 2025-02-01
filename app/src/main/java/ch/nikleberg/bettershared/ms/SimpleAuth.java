package ch.nikleberg.bettershared.ms;

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
import java.util.function.Consumer;
import java.util.function.Supplier;

import ch.nikleberg.bettershared.R;

public class SimpleAuth {
    final public String TAG = SimpleAuth.class.getSimpleName();

    public SimpleAuth(Context context, List<String> scopes, Supplier<CompletableFuture<Activity>> getActivity, Consumer<SimpleAuthProvider> onAuthentication, Consumer<Exception> onError) {
        createApp(context, app -> loadAccount(app,
                account -> authenticateSilent(app, account, scopes, onAuthentication, onError),
                ex -> getActivity.get().thenAccept(activity ->
                        authenticateInteractive(app, activity, scopes, onAuthentication, onError))), onError);
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

    private void authenticateSilent(ISingleAccountPublicClientApplication app, IAccount account, List<String> scopes, Consumer<SimpleAuthProvider> onAuthentication, Consumer<Exception> onError) {
        final AcquireTokenSilentParameters.Builder builder = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.getAuthority())
                .withScopes(scopes)
                .withCallback(new SilentAuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        onAuthentication.accept(new SimpleAuthProvider(authenticationResult.getAccessToken()));
                    }

                    @Override
                    public void onError(MsalException exception) {
                        onError.accept(exception);
                    }
                });
        app.acquireTokenSilentAsync(builder.build());
    }

    private void authenticateInteractive(ISingleAccountPublicClientApplication app, Activity activity, List<String> scopes, Consumer<SimpleAuthProvider> onAuthentication, Consumer<Exception> onError) {
        final SignInParameters.SignInParametersBuilder builder = SignInParameters.builder()
                .withActivity(activity)
                .withScopes(scopes)
                .withCallback(new AuthenticationCallback() {
                    @Override
                    public void onCancel() {
                        onError.accept(new RuntimeException("User cancelled"));
                    }

                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        onAuthentication.accept(new SimpleAuthProvider(authenticationResult.getAccessToken()));
                    }

                    @Override
                    public void onError(MsalException exception) {
                        onError.accept(exception);
                    }
                });
        app.signIn(builder.build());
    }
}