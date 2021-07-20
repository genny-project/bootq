package life.genny.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;


import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import io.vertx.core.json.JsonObject;
import org.apache.http.impl.client.HttpClientBuilder;


public class KeycloakUtils {
    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    public static int ACCESS_TOKEN_EXPIRY_LIMIT_SECONDS = 60;


    // Decode the keycloak token string and send back in Json Format
    public static JSONObject getDecodedToken(final String bearerToken) {
        JSONObject jsonObj = null;
        String decodedJson = null;
        try {
            final String[] jwtToken = bearerToken.split("\\.");
            final org.apache.commons.codec.binary.Base64 decoder = new Base64(true);
            final byte[] decodedClaims = decoder.decode(jwtToken[1]);
            decodedJson = new String(decodedClaims);
            jsonObj = new JSONObject(decodedJson);
        } catch (final JSONException e1) {
            log.info("bearerToken=" + bearerToken + "  decodedJson=" + decodedJson + ":" + e1.getMessage());
        }
        return jsonObj;
    }


    public static JsonObject getToken(String keycloakUrl, String realm, String clientId, String secret, String username, String password, String refreshToken) throws IOException {

        HashMap<String, String> postDataParams = new HashMap<>();
        postDataParams.put("Content-Type", "application/x-www-form-urlencoded");
        /* if we have a refresh token */
        if (refreshToken != null) {

            /* we decode it */
            JSONObject decodedServiceToken = KeycloakUtils.getDecodedToken(refreshToken);

            /* we get the expiry timestamp */
            long expiryTime = decodedServiceToken.getLong("exp");

            /* we get the current time */
            long nowTime = LocalDateTime.now().atZone(TimeZone.getDefault().toZoneId()).toEpochSecond();

            /* we calculate the differencr */
            long duration = expiryTime - nowTime;

			/* if the difference is negative it means the expiry time is less than the nowTime
				if the difference < 180000, it means the token will expire in 3 hours
			*/
            if (duration <= ACCESS_TOKEN_EXPIRY_LIMIT_SECONDS) {

                /* if the refresh token is about to expire, we must re-generate a new one */
                refreshToken = null;
            }
        }
        /* if we don't have a refresh token, we generate a new token using username and password */
        if (refreshToken == null) {
            postDataParams.put("username", username);
            postDataParams.put("password", password);
            log.info("using username");
            postDataParams.put(OAuth2Constants.GRANT_TYPE, "password");
        } else {
            postDataParams.put("refresh_token", refreshToken);
            postDataParams.put(OAuth2Constants.GRANT_TYPE, "refresh_token");
            log.info("using refresh token");
            log.info(refreshToken);
        }

        postDataParams.put(OAuth2Constants.CLIENT_ID, clientId);
        if (secret != null) {
            postDataParams.put(OAuth2Constants.CLIENT_SECRET, secret);
        }


        String requestURL = keycloakUrl + "/auth/realms/" + realm + "/protocol/openid-connect/token";

        String str = QwandaUtils.performPostCall(requestURL,
                postDataParams);


        JsonObject json = new JsonObject(str);
        return json;
    }

    public static JsonObject getToken(String keycloakUrl, String realm, String clientId, String secret,
                                      String username, String password) throws IOException {
        return KeycloakUtils.getToken(keycloakUrl, realm, clientId, secret, username, password, null);
    }

    public static String getAccessToken(String keycloakUrl, String realm, String clientId, String secret, String username,
                                        String password) throws IOException {

        try {

            JsonObject content = KeycloakUtils.getToken(keycloakUrl, realm, clientId, secret, username, password);
            if (content != null) {
                return content.getString("access_token");
            }

            return null;
        } catch (Exception e) {
            log.error("Cannot get Token for USername " + username + " for realm " + realm + " on " + keycloakUrl + " and clientId " + clientId);
        }
        return null;
    }

    public static Integer getKeycloakUserCount(String keycloakUrl, String realm, String servicePassword) {
        Integer count = -1;
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String accessToken = getAccessToken(keycloakUrl, realm, "admin-cli", null, "service", servicePassword);
            HttpGet getUserCount = new HttpGet(keycloakUrl + "/auth/admin/realms/" + realm + "/users/count");
            getUserCount.addHeader("Authorization", "Bearer " + accessToken);
            HttpResponse response = client.execute(getUserCount);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Get keycloak user response code:" + response.getStatusLine().getStatusCode());
            }
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            count = JsonSerialization.readValue(is, Integer.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }


    public static HashMap<String, String> getUsersByRealm(String keycloakUrl, String realm, String servicePassword) {
        HashMap<String, String> userCodeUUIDMapping = new HashMap<>();
        List<LinkedHashMap> results = new ArrayList<>();

        Integer count = getKeycloakUserCount(keycloakUrl, realm, servicePassword);
        log.info("Total keycloak user:" + count);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String accessToken = getAccessToken(keycloakUrl, realm, "admin-cli", null, "service", servicePassword);

            HttpGet get = new HttpGet(keycloakUrl + "/auth/admin/realms/" + realm + "/users?first=0&max=" + count);
            get.addHeader("Authorization", "Bearer " + accessToken);
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Get keycloak user response code:" + response.getStatusLine().getStatusCode());
            }
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            results = JsonSerialization.readValue(is, (new ArrayList<UserRepresentation>()).getClass());
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (LinkedHashMap userMap : results) {
            String username = (String) userMap.get("username");
            String email = (String) userMap.get("email");
            String code = QwandaUtils.getNormalisedUsername("PER_" + username);
            String id = (String) userMap.get("id");
            String uuid = "PER_" + id.toUpperCase();
            if (userCodeUUIDMapping.containsKey(code)) {
                log.error(String.format("Duplicate user in keycloak, user code:%s, user name:%s, email:%s.",
                        code, username, email));
            } else {
                userCodeUUIDMapping.put(code, uuid);
            }
        }
        log.info("Get " + results.size() + " keycloak users");
        return userCodeUUIDMapping;
    }

    public static String getKeycloakUUIDByUserCode(String code, HashMap<String, String> userCodeUUIDMapping) {
        String keycloakUUID = null;
        if (userCodeUUIDMapping.containsKey(code)) {
            keycloakUUID = userCodeUUIDMapping.get(code);
            log.debug(String.format("DEBUG:Find user baseentity code:%s, update to keycloak uuid:%s",
                    code, keycloakUUID));
        } else {
            keycloakUUID = code;
            log.debug(String.format("DEBUG:Can not find user baseentity code:%s, set keycloak uuid:%s",
                    code, keycloakUUID));
        }
        return keycloakUUID;
    }
}
