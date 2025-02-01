package ch.nikleberg.bettershared.ms;

import com.azure.core.credential.TokenCredential;
import com.microsoft.graph.serviceclient.GraphServiceClient;

public class Client {
    private static Client instance = null;

    private GraphServiceClient graphClient = null;

    private Client() {

    }

    public static Client getInstance() {
        if (null == instance) {
            instance = new Client();
        }
        return instance;
    }

    public GraphServiceClient getClient(TokenCredential credential, String[] scopes) {
        if (null == graphClient) {
            graphClient = new GraphServiceClient(credential, scopes);
        }
        return graphClient;
    }
}
