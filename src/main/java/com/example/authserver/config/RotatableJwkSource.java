package com.example.authserver.config;

import com.example.authserver.service.RsaKeyService;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class RotatableJwkSource implements JWKSource<SecurityContext> {

    private final RsaKeyService rsaKeyService;

    @Override
    public List<JWK> get(JWKSelector selector, SecurityContext context) throws KeySourceException {
        List<JWK> keys = rsaKeyService.loadKeys().stream().map(k -> (JWK) k).toList();
        List<JWK> matched = selector.select(new com.nimbusds.jose.jwk.JWKSet(keys));
        return matched.isEmpty() ? keys.subList(0, 1) : matched; // fallback to newest
    }
}
