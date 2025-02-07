package ch.nikleberg.bettershared.gui;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.microsoft.graph.models.Folder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;

import java.util.ArrayList;

import ch.nikleberg.bettershared.R;
import ch.nikleberg.bettershared.db.Album;
import ch.nikleberg.bettershared.db.AlbumObserver;
import ch.nikleberg.bettershared.ms.DriveUtils;
import ch.nikleberg.bettershared.ms.auth.Auth;
import ch.nikleberg.bettershared.ms.auth.AuthProvider;

public class MainActivity extends AppCompatActivity implements AlbumObserver {
    public final String TAG = MainActivity.class.getSimpleName();

    private final Auth auth = Auth.getInstance();
    private GraphServiceClient graph = null;

    private ArrayList<Album> albums = new ArrayList<>();

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
            test();
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
        String message = null;
        if (ex instanceof MsalClientException) {
            MsalException msalEx = (MsalException) ex;
            message = getString(R.string.auth_error_message, msalEx.getMessage(), msalEx.getErrorCode());
        } else {
            message = getString(R.string.auth_error_message, ex.getMessage(), "unknown");
        }
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.auth_error_button_retry,
                        (dialog, which) -> authenticateInteractive())
                .setNegativeButton(R.string.auth_error_button_cancel,
                        (dialog, which) -> finish())
                .setCancelable(false)
                .create()
                .show();
    }

    // *********************************************************************************************
    // ** Album Recycler
    // *********************************************************************************************

    @Override
    public void onAlbumAdded(Album album) {
        albums.add(album);
    }

    @Override
    public void onAlbumChanged(Album album) {
        albums.replaceAll(a -> a.id == album.id ? album : a);
    }

    @Override
    public void onAlbumRemoved(Album album) {
        albums.removeIf(a -> a.id == album.id);
    }

    private void test() {
        graph = new GraphServiceClient(new AuthProvider(auth));
        DriveUtils.getDrives(graph).thenAccept(drives -> {
            drives.forEach(drive -> {
                Log.d(TAG, "Drive type: " + drive.getDriveType() + ", id: " + drive.getId());
                // personal drive-id: 31867e5cd2a336b

                DriveUtils.getDriveItem(graph, drive.getId(), "root").thenAccept(item -> {
                    Folder folder = item.getFolder();
                    if (null != folder) {
                        Log.d(TAG, "Root folder has " + item.getFolder().getChildCount() + " items.");
                    }
                });
            });
        });
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
