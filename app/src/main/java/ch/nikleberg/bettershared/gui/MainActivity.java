package ch.nikleberg.bettershared.gui;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;

import com.google.android.material.snackbar.Snackbar;
import com.microsoft.graph.models.Folder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;

import ch.nikleberg.bettershared.R;
import ch.nikleberg.bettershared.ms.DriveUtils;
import ch.nikleberg.bettershared.ms.auth.Auth;
import ch.nikleberg.bettershared.ms.auth.AuthProvider;

public class MainActivity extends AppCompatActivity implements MenuProvider {
    public final String TAG = MainActivity.class.getSimpleName();

    private final Auth auth = Auth.getInstance();
    private GraphServiceClient graph = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authenticateSilent();
    }

    // *********************************************************************************************
    // ** Authentication
    // *********************************************************************************************

    private void authenticateSilent() {
        auth.setSilentContext(getApplicationContext(), DriveUtils.DRIVE_SCOPES);
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
        auth.setInteractiveContext(getApplicationContext(), DriveUtils.DRIVE_SCOPES, this);
        auth.authenticateInteractive().thenAccept(v -> {
            auth.setInteractiveContext(null, null, null);
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

    private void test() {
        graph = new GraphServiceClient(new AuthProvider(auth));
        DriveUtils.getDrives(graph).thenAccept(drives -> drives.forEach(drive -> {
            Log.d(TAG, "Drive type: " + drive.getDriveType() + ", id: " + drive.getId());
            // personal drive-id: 31867e5cd2a336b
            //     drive-item-id: 31867e5cd2a336b!68561

            DriveUtils.getDriveItem(graph, drive.getId(), "root").thenAccept(item -> {
                Folder folder = item.getFolder();
                if (null != folder) {
                    Log.d(TAG, "Root folder has " + item.getFolder().getChildCount() + " items.");
                }
            });
        }));
    }

    private void test2() {
        DriveUtils.getDrives(graph).thenAccept(drives -> drives.forEach(drive -> {
            Log.i(TAG, "Drive: " + drive.getDriveType() + ", ID: " + drive.getId());
            DriveUtils.getDriveItems(graph, drive.getId()).thenAccept(items -> items.forEach(item -> {
                boolean isFolder = null != item.getFolder();
                Log.i(TAG, "Item: " + item.getName() + ", ID: " + item.getId() + ", Type: " + (isFolder ? "Folder" : "File"));
                if (isFolder) {
                    DriveUtils.getDriveItems(graph, drive.getId(), item.getId()).thenAccept(subItems -> subItems.forEach(subItem -> {
                        boolean isFolder2 = null != subItem.getFolder();
                        Log.i(TAG, "\tFolder: " + item.getName() + ", Item: " + subItem.getName() + ", ID: " + subItem.getId() + ", Type: " + (isFolder2 ? "Folder" : "File"));
                        if (isFolder2) {
                            DriveUtils.getDriveItems(graph, drive.getId(), subItem.getId()).thenAccept(subItems2 -> subItems2.forEach(subItem2 -> {
                                boolean isFolder3 = null != subItem2.getFolder();
                                Log.i(TAG, "\t\tFolder: " + subItem.getName() + ", Item: " + subItem2.getName() + ", ID: " + subItem2.getId() + ", Type: " + (isFolder3 ? "Folder" : "File"));
                            }));
                        }
                    }));
                }
            }));
        }));
    }
}
