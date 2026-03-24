package com.example.authserver.service;

import com.example.authserver.model.MongoOAuth2AuthorizationConsent;
import com.example.authserver.repository.MongoOAuth2AuthorizationConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MongoOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private final MongoOAuth2AuthorizationConsentRepository repo;

    @Override
    public void save(OAuth2AuthorizationConsent consent) {
        repo.save(toDocument(consent));
    }

    @Override
    public void remove(OAuth2AuthorizationConsent consent) {
        repo.deleteByRegisteredClientIdAndPrincipalName(
                consent.getRegisteredClientId(), consent.getPrincipalName());
    }

    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        return repo.findByRegisteredClientIdAndPrincipalName(registeredClientId, principalName)
                .map(this::toConsent).orElse(null);
    }

    private MongoOAuth2AuthorizationConsent toDocument(OAuth2AuthorizationConsent consent) {
        MongoOAuth2AuthorizationConsent doc = new MongoOAuth2AuthorizationConsent();
        doc.setId(consent.getRegisteredClientId() + ":" + consent.getPrincipalName());
        doc.setRegisteredClientId(consent.getRegisteredClientId());
        doc.setPrincipalName(consent.getPrincipalName());
        doc.setAuthorities(consent.getAuthorities().stream()
                .map(a -> a.getAuthority()).collect(Collectors.toSet()));
        return doc;
    }

    private OAuth2AuthorizationConsent toConsent(MongoOAuth2AuthorizationConsent doc) {
        OAuth2AuthorizationConsent.Builder builder = OAuth2AuthorizationConsent
                .withId(doc.getRegisteredClientId(), doc.getPrincipalName());
        doc.getAuthorities().forEach(a -> builder.authority(new SimpleGrantedAuthority(a)));
        return builder.build();
    }
}
