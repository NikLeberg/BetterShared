package ch.nikleberg.bettershared.ms;

import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.authentication.AuthenticationProvider;

import java.util.Map;

public class SimpleAuthProvider implements AuthenticationProvider {

    final private String token;

    public SimpleAuthProvider(String token) {
        this.token = token;
    }

    @Override
    public void authenticateRequest(RequestInformation request, Map<String, Object> additionalAuthenticationContext) {
        request.headers.add("Authorization", "Bearer " + token);
    }
}
