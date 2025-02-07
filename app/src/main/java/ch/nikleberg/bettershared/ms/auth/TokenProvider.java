package ch.nikleberg.bettershared.ms.auth;

import com.microsoft.identity.client.IAuthenticationResult;

public interface TokenProvider {
    IAuthenticationResult getAccessToken();
}
