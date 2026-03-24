package com.example.authserver.repository;

import com.example.authserver.model.MongoOAuth2Authorization;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface MongoOAuth2AuthorizationRepository extends MongoRepository<MongoOAuth2Authorization, String> {
    Optional<MongoOAuth2Authorization> findByAuthorizationCode_TokenValue(String token);
    Optional<MongoOAuth2Authorization> findByAccessToken_TokenValue(String token);
    Optional<MongoOAuth2Authorization> findByRefreshToken_TokenValue(String token);
    Optional<MongoOAuth2Authorization> findByOidcIdToken_TokenValue(String token);

    @Query("{ $or: [ {'authorizationCode.tokenValue': ?0}, {'accessToken.tokenValue': ?0}, " +
           "{'refreshToken.tokenValue': ?0}, {'oidcIdToken.tokenValue': ?0} ] }")
    Optional<MongoOAuth2Authorization> findByAnyTokenValue(String token);
}
