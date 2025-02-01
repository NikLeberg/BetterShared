package ch.nikleberg.bettershared.ms;

import android.content.Context;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.AuthenticationRecord;
import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.azure.identity.DeviceCodeInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;

public class Auth implements TokenCredential {
    final private static String TAG = Auth.class.toString();

    final private static String CLIENT_ID = "9cef345a-3cd5-4240-9de5-76a851bafe0b";
    final private static String TENANT_ID = "common"; // business + private ms accounts

    final private static String AUTH_RECORD_PATH = "authRecord.bin";

    final private File authenticationRecordFile;
    final private Consumer<DeviceCodeInfo> authenticationChallengeConsumer;

    public Auth(Context context, Consumer<DeviceCodeInfo> challengeConsumer) {
        authenticationRecordFile = new File(context.getCacheDir(), AUTH_RECORD_PATH);
        authenticationChallengeConsumer = challengeConsumer;
    }

    @Override
    public Mono<AccessToken> getToken(TokenRequestContext request) {
        return runSilentAuth(request)
                .onErrorResume(e -> Mono.from(runInteractiveAuth(request)).flatMap(record -> {
                    try {
                        return record.serializeAsync(new FileOutputStream(authenticationRecordFile));
                    } catch (FileNotFoundException ex) {
                        return Mono.empty();
                    }
                }).flatMap(result -> runSilentAuth(request)));
    }

    private DeviceCodeCredentialBuilder getCredentialBuilder() {
        return new DeviceCodeCredentialBuilder()
                .clientId(CLIENT_ID)
                .tenantId(TENANT_ID);
    }

    private void addAuthenticationRecordToBuilderIfExisting(DeviceCodeCredentialBuilder builder) {
        if (authenticationRecordFile.exists()) {
            AuthenticationRecord authRec = null;
            try {
                authRec = AuthenticationRecord.deserialize(new FileInputStream(authenticationRecordFile));
            } catch (FileNotFoundException e) {
                throw new RuntimeException("nuh hu? this shall not happen");
            }
            if (null != authRec) {
                builder.authenticationRecord(authRec);
            }
        }
    }

    private Mono<AccessToken> runSilentAuth(TokenRequestContext request) {
        final DeviceCodeCredentialBuilder builder = getCredentialBuilder()
                .disableAutomaticAuthentication();
        addAuthenticationRecordToBuilderIfExisting(builder);
        DeviceCodeCredential credential = builder.build();
        return credential.getToken(request);
    }

    private Mono<AuthenticationRecord> runInteractiveAuth(TokenRequestContext request) {
        final DeviceCodeCredentialBuilder builder = getCredentialBuilder()
                .challengeConsumer(authenticationChallengeConsumer);
        DeviceCodeCredential credential = builder.build();
        return credential.authenticate(request);
    }
}
