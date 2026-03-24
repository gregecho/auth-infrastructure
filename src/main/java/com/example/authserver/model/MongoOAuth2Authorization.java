package com.example.authserver.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Data
@Document(collection = "oauth2_authorizations")
public class MongoOAuth2Authorization {
    @Id
    private String id;
    private String registeredClientId;
    private String principalName;
    private String authorizationGrantType;
    private Set<String> authorizedScopes;
    private TokenData authorizationCode;
    private TokenData accessToken;
    private TokenData refreshToken;
    private TokenData oidcIdToken;
    private String attributes;

    @Data
    public static class TokenData {
        private String tokenValue;
        private Instant issuedAt;
        private Instant expiresAt;
        private String claims;
        private boolean invalidated;
    }
}
