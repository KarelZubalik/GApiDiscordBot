package org.example.Google.Factories;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Scanner;

public class PhotosLibraryClientFactory {
    private static final java.io.File DATA_STORE_DIR =
            new java.io.File(new File(System.getProperty("user.dir")).getParentFile().getPath(), "credentials");
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final int LOCAL_RECEIVER_PORT = 4040;

    private PhotosLibraryClientFactory() {}

    /** Creates a new {@link PhotosLibraryClient} instance with credentials and scopes. */
    public static PhotosLibraryClient createClient(
            String credentialsPath,
            List<String> selectedScopes)

            throws Exception {

        PhotosLibrarySettings settings =
                PhotosLibrarySettings.newBuilder()
                        .setCredentialsProvider(
                                FixedCredentialsProvider.create(
                                        getUserCredentials(credentialsPath, selectedScopes)))
                        .build();
        return PhotosLibraryClient.initialize(settings);
    }

    private static Credentials getUserCredentials(String credentialsPath, List<String> selectedScopes) throws Exception {
        // Získání názvu operačního systému
        String osName = System.getProperty("os.name").toLowerCase();

        // Kontrola, zda je operační systém Windows nebo Linux
        if (osName.contains("win")) {
            return getUserCredentialsFromBrowser(credentialsPath,selectedScopes);
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("mac")) {
            return getUserCredentialsWithInput(credentialsPath, selectedScopes);
        } else {
            throw new Exception("Není možné určit, na jakém operačním systému běžíte.");
        }
    }

    private static Credentials getUserCredentialsWithInput(String credentialsPath, List<String> selectedScopes)
            throws IOException, GeneralSecurityException {
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(
                        JSON_FACTORY, new InputStreamReader(new FileInputStream(credentialsPath)));
        String clientId = clientSecrets.getDetails().getClientId();
        String clientSecret = clientSecrets.getDetails().getClientSecret();
        String redirectUri = clientSecrets.getDetails().getRedirectUris().get(0);

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        JSON_FACTORY,
                        clientSecrets,
                        selectedScopes)
                        .setDataStoreFactory(new FileDataStoreFactory(DATA_STORE_DIR))
                        .setAccessType("offline")
                        .build();
        String authorizationUrl = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .build();
        System.out.println("Open the following URL in your browser:");
        System.out.println(authorizationUrl);
        System.out.println("Enter the authorization code:");

        Scanner scanner = new Scanner(System.in);
        String code = scanner.nextLine();
        Credential credential = flow.createAndStoreCredential(
                flow.newTokenRequest(code)
                        .setRedirectUri(redirectUri)
                        .execute(),
                "user");

        return UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(credential.getRefreshToken())
                .build();
    }

    private static Credentials getUserCredentialsFromBrowser(String credentialsPath, List<String> selectedScopes)
            throws IOException, GeneralSecurityException {
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(
                        JSON_FACTORY, new InputStreamReader(new FileInputStream(credentialsPath)));
        String clientId = clientSecrets.getDetails().getClientId();
        String clientSecret = clientSecrets.getDetails().getClientSecret();

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        JSON_FACTORY,
                        clientSecrets,
                        selectedScopes)
                        .setDataStoreFactory(new FileDataStoreFactory(DATA_STORE_DIR))
                        .setAccessType("offline")
                        .build();
        LocalServerReceiver receiver =
                new LocalServerReceiver.Builder().setPort(LOCAL_RECEIVER_PORT).setCallbackPath("/gtoken").build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        return UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(credential.getRefreshToken())
                .build();
    }
}

