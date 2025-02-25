package ch.nikleberg.bettershared.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;

import ch.nikleberg.bettershared.R;
import ch.nikleberg.bettershared.ms.DriveUtils;
import ch.nikleberg.bettershared.ms.auth.Auth;

public class MainActivity extends AppCompatActivity {
    public final String TAG = MainActivity.class.getSimpleName();

    private Auth auth = null;

    public static Intent getLaunchIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = Auth.getInstance(getApplicationContext(), DriveUtils.DRIVE_SCOPES);
        authenticateSilent();
    }

    // *********************************************************************************************
    // ** Authentication
    // *********************************************************************************************

    private void authenticateSilent() {
        auth.authenticateSilent().thenAccept(v -> {
            showLoggedInAsSnackbar();
            // DEBUG START
//            graph = new GraphServiceClient(new AuthProvider(auth));
//            test2();
            // DEBUG END
        }).exceptionally(ex -> {
            showLogInRequestSnackbar();
            return null;
        });
    }

    private void authenticateInteractive() {
        auth.authenticateInteractive(this).thenAccept(v -> {
            authenticateSilent();
        }).exceptionally(ex -> {
            showAuthenticationFailureDialog(ex);
            return null;
        });
    }

    private void showLoggedInAsSnackbar() {
        IAccount account = auth.getAuthenticatedUser();
        String message = getString(R.string.auth_login_success_message, account.getUsername());
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.auth_logout_action, c -> Log.d(TAG, "Logout requested!"))
                .show();
    }

    private void showLogInRequestSnackbar() {
        Snackbar.make(findViewById(android.R.id.content), R.string.auth_login_request_message, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.auth_login_action, c -> authenticateInteractive())
                .show();
    }

    private void showAuthenticationFailureDialog(Throwable ex) {
        String message;
        if (ex instanceof MsalClientException) {
            MsalException msalEx = (MsalException) ex;
            message = getString(R.string.auth_error_message, msalEx.getMessage(), msalEx.getErrorCode());
        } else {
            message = getString(R.string.auth_error_message, ex.getMessage(), "unknown");
        }
        // TODO: Dialog is not showing.
        new AlertDialog.Builder(getApplicationContext())
                .setMessage(message)
                .setPositiveButton(R.string.auth_error_button_retry,
                        (dialog, which) -> authenticateInteractive())
                .setNegativeButton(R.string.auth_error_button_cancel,
                        (dialog, which) -> finish())
                .setCancelable(false)
                .create()
                .show();
    }
}
