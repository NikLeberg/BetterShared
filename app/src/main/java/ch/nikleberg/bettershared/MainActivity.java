package ch.nikleberg.bettershared;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import ch.nikleberg.bettershared.ms.SimpleAuth;
import ch.nikleberg.bettershared.ms.SimpleAuthProvider;

public class MainActivity extends AppCompatActivity {
    final public String TAG = MainActivity.class.getSimpleName();

    private SimpleAuthProvider tokenProvider = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        final SimpleAuth sa = new SimpleAuth(getApplicationContext(), List.of("User.Read"),
                this::getActivity, this::onAuthenticated, e -> Log.e(TAG, e.getMessage(), e));
    }

    private CompletableFuture<Activity> getActivity() {
        return CompletableFuture.supplyAsync(() -> this);
    }

    private void onAuthenticated(SimpleAuthProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
        makeGraphApiCall();
    }

    private void makeGraphApiCall() {
        final GraphServiceClient client = new GraphServiceClient(tokenProvider);
        new Thread(() -> {
            User me = client.me().get();
            Log.i(TAG, "Hello " + me.getDisplayName() + ", your ID is " + me.getId());
        }).start();
    }
}