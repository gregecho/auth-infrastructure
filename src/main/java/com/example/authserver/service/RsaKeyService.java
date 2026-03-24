package com.example.authserver.service;

import com.nimbusds.jose.jwk.RSAKey;

import java.util.List;

public interface RsaKeyService {
    List<RSAKey> loadKeys();
    RSAKey rotate();
    void ensureInitialized();
}
