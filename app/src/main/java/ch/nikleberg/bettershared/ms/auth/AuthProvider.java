package ch.nikleberg.bettershared.ms.auth;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.authentication.AuthenticationProvider;

import java.util.Date;
import java.util.Map;

public class AuthProvider implements AuthenticationProvider {
    private IAuthenticationResult accessToken = null;
    private TokenProvider provider;

    public AuthProvider(TokenProvider provider) {
        this.provider = provider;
    }

    @Override
    public void authenticateRequest(@NonNull RequestInformation request, Map<String, Object> additionalAuthContext) {
        if (isTokenExpired()) {
            refreshToken();
        }
        request.headers.add("Authorization", "Bearer " + getToken());
    }

    private boolean isTokenExpired() {
        return (null == accessToken) || (accessToken.getExpiresOn().before(new Date()));
    }

    private String getToken() {
        assert accessToken != null;
        return accessToken.getAccessToken();
    }

    private void refreshToken() {
        assert provider != null;
        accessToken = provider.getAccessToken();
    }
}
