package ch.nikleberg.bettershared;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import ch.nikleberg.bettershared.ms.Auth;

public class MainActivity extends AppCompatActivity {

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

        final String[] scopes = new String[]{"User.Read"};

        final Auth auth = new Auth(this, challenge -> Log.d("MainActivity", challenge.getMessage()));
        final GraphServiceClient client = new GraphServiceClient(auth, scopes);

        new Thread(() -> {
            User me = client.me().get();
            System.out.printf("Hello %s, your ID is %s%n", me.getDisplayName(), me.getId());
        }).start();
    }
}