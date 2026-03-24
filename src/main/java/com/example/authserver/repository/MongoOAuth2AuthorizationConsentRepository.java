package com.example.authserver.repository;

import com.example.authserver.model.MongoOAuth2AuthorizationConsent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MongoOAuth2AuthorizationConsentRepository extends MongoRepository<MongoOAuth2AuthorizationConsent, String> {
    Optional<MongoOAuth2AuthorizationConsent> findByRegisteredClientIdAndPrincipalName(
            String registeredClientId, String principalName);
    void deleteByRegisteredClientIdAndPrincipalName(
            String registeredClientId, String principalName);
}
