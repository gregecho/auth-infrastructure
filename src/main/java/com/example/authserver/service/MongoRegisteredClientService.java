package com.example.authserver.service;

import com.example.authserver.model.MongoRegisteredClient;
import com.example.authserver.repository.MongoRegisteredClientRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MongoRegisteredClientService implements RegisteredClientRepository {

    private final MongoRegisteredClientRepository repo;
    private final ObjectMapper objectMapper;

    @Override
    public void save(RegisteredClient client) {
        repo.save(toDocument(client));
    }

    @Override
    public RegisteredClient findById(String id) {
        return repo.findById(id).map(this::toRegisteredClient).orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return repo.findByClientId(clientId).map(this::toRegisteredClient).orElse(null);
    }

    private MongoRegisteredClient toDocument(RegisteredClient c) {
        try {
            MongoRegisteredClient doc = new MongoRegisteredClient();
            doc.setId(c.getId());
            doc.setClientId(c.getClientId());
            doc.setClientIdIssuedAt(c.getClientIdIssuedAt());
            doc.setClientSecret(c.getClientSecret());
            doc.setClientSecretExpiresAt(c.getClientSecretExpiresAt());
            doc.setClientName(c.getClientName());
            doc.setClientAuthenticationMethods(c.getClientAuthenticationMethods().stream()
                    .map(ClientAuthenticationMethod::getValue).collect(Collectors.toSet()));
            doc.setAuthorizationGrantTypes(c.getAuthorizationGrantTypes().stream()
                    .map(AuthorizationGrantType::getValue).collect(Collectors.toSet()));
            doc.setRedirectUris(c.getRedirectUris());
            doc.setPostLogoutRedirectUris(c.getPostLogoutRedirectUris());
            doc.setScopes(c.getScopes());
            doc.setClientSettings(objectMapper.writeValueAsString(c.getClientSettings().getSettings()));
            doc.setTokenSettings(objectMapper.writeValueAsString(c.getTokenSettings().getSettings()));
            return doc;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RegisteredClient toRegisteredClient(MongoRegisteredClient doc) {
        try {
            Map<String, Object> clientSettingsMap = objectMapper.readValue(
                    doc.getClientSettings(), new TypeReference<>() {});
            Map<String, Object> tokenSettingsMap = objectMapper.readValue(
                    doc.getTokenSettings(), new TypeReference<>() {});

            // OAuth2TokenFormat is stored as {"value":"..."} — restore the typed object
            Object tokenFormat = tokenSettingsMap.get("settings.token.access-token-format");
            if (tokenFormat instanceof Map<?, ?> formatMap) {
                tokenSettingsMap.put("settings.token.access-token-format",
                        new OAuth2TokenFormat((String) formatMap.get("value")));
            }

            // Duration values are serialized as ISO-8601 strings — restore typed objects
            Set<String> durationKeys = Set.of(
                    "settings.token.access-token-time-to-live",
                    "settings.token.refresh-token-time-to-live",
                    "settings.token.authorization-code-time-to-live",
                    "settings.token.device-code-time-to-live"
            );
            for (String key : durationKeys) {
                Object val = tokenSettingsMap.get(key);
                if (val instanceof String s) {
                    tokenSettingsMap.put(key, Duration.parse(s));
                }
            }

            RegisteredClient.Builder builder = RegisteredClient.withId(doc.getId())
                    .clientId(doc.getClientId())
                    .clientIdIssuedAt(doc.getClientIdIssuedAt())
                    .clientSecret(doc.getClientSecret())
                    .clientSecretExpiresAt(doc.getClientSecretExpiresAt())
                    .clientName(doc.getClientName())
                    .redirectUris(uris -> uris.addAll(doc.getRedirectUris()))
                    .postLogoutRedirectUris(uris -> uris.addAll(doc.getPostLogoutRedirectUris()))
                    .scopes(scopes -> scopes.addAll(doc.getScopes()))
                    .clientSettings(ClientSettings.withSettings(clientSettingsMap).build())
                    .tokenSettings(TokenSettings.withSettings(tokenSettingsMap).build());

            doc.getClientAuthenticationMethods().forEach(m ->
                    builder.clientAuthenticationMethod(new ClientAuthenticationMethod(m)));
            doc.getAuthorizationGrantTypes().forEach(g ->
                    builder.authorizationGrantType(new AuthorizationGrantType(g)));

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
