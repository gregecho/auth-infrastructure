package com.example.authserver.service;

import com.example.authserver.model.MongoOAuth2Authorization;
import com.example.authserver.model.MongoOAuth2Authorization.TokenData;
import com.example.authserver.repository.MongoOAuth2AuthorizationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MongoOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final MongoOAuth2AuthorizationRepository repo;
    private final RegisteredClientRepository registeredClientRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(OAuth2Authorization authorization) {
        repo.save(toDocument(authorization));
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        repo.deleteById(authorization.getId());
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return repo.findById(id).map(this::toAuthorization).orElse(null);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        Optional<MongoOAuth2Authorization> result;
        if (tokenType == null) {
            result = repo.findByAnyTokenValue(token);
        } else {
            result = switch (tokenType.getValue()) {
                case OAuth2ParameterNames.CODE -> repo.findByAuthorizationCode_TokenValue(token);
                case "access_token" -> repo.findByAccessToken_TokenValue(token);
                case "refresh_token" -> repo.findByRefreshToken_TokenValue(token);
                default -> repo.findByAnyTokenValue(token);
            };
        }
        return result.map(this::toAuthorization).orElse(null);
    }

    private MongoOAuth2Authorization toDocument(OAuth2Authorization auth) {
        try {
            MongoOAuth2Authorization doc = new MongoOAuth2Authorization();
            doc.setId(auth.getId());
            doc.setRegisteredClientId(auth.getRegisteredClientId());
            doc.setPrincipalName(auth.getPrincipalName());
            doc.setAuthorizationGrantType(auth.getAuthorizationGrantType().getValue());
            doc.setAuthorizedScopes(auth.getAuthorizedScopes());
            doc.setAttributes(objectMapper.writeValueAsString(auth.getAttributes()));

            var code = auth.getToken(OAuth2AuthorizationCode.class);
            if (code != null) doc.setAuthorizationCode(toTokenData(code));

            var access = auth.getToken(OAuth2AccessToken.class);
            if (access != null) doc.setAccessToken(toTokenData(access));

            var refresh = auth.getToken(OAuth2RefreshToken.class);
            if (refresh != null) doc.setRefreshToken(toTokenData(refresh));

            var oidc = auth.getToken(OidcIdToken.class);
            if (oidc != null) doc.setOidcIdToken(toTokenData(oidc));

            return doc;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends AbstractOAuth2Token> TokenData toTokenData(OAuth2Authorization.Token<T> token) {
        try {
            TokenData td = new TokenData();
            td.setTokenValue(token.getToken().getTokenValue());
            td.setIssuedAt(token.getToken().getIssuedAt());
            td.setExpiresAt(token.getToken().getExpiresAt());
            td.setInvalidated(token.isInvalidated());
            td.setClaims(objectMapper.writeValueAsString(token.getClaims()));
            return td;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OAuth2Authorization toAuthorization(MongoOAuth2Authorization doc) {
        var registeredClient = registeredClientRepository.findById(doc.getRegisteredClientId());
        if (registeredClient == null) {
            throw new DataRetrievalFailureException("RegisteredClient not found: " + doc.getRegisteredClientId());
        }
        try {
            Map<String, Object> attrs = objectMapper.readValue(doc.getAttributes(), new TypeReference<>() {});

            OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient)
                    .id(doc.getId())
                    .principalName(doc.getPrincipalName())
                    .authorizationGrantType(new AuthorizationGrantType(doc.getAuthorizationGrantType()))
                    .authorizedScopes(doc.getAuthorizedScopes())
                    .attributes(a -> a.putAll(attrs));

            if (doc.getAuthorizationCode() != null) {
                TokenData td = doc.getAuthorizationCode();
                builder.token(new OAuth2AuthorizationCode(td.getTokenValue(), td.getIssuedAt(), td.getExpiresAt()));
            }
            if (doc.getAccessToken() != null) {
                TokenData td = doc.getAccessToken();
                builder.token(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                        td.getTokenValue(), td.getIssuedAt(), td.getExpiresAt()));
            }
            if (doc.getRefreshToken() != null) {
                TokenData td = doc.getRefreshToken();
                builder.token(new OAuth2RefreshToken(td.getTokenValue(), td.getIssuedAt(), td.getExpiresAt()));
            }

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
