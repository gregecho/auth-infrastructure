package com.example.authserver.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Data
@Document(collection = "oauth2_authorization_consents")
public class MongoOAuth2AuthorizationConsent {
    @Id
    private String id; // "{registeredClientId}:{principalName}"
    private String registeredClientId;
    private String principalName;
    private Set<String> authorities;
}
