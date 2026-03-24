package com.example.authserver.service.Impl;

import com.example.authserver.model.RsaKeyPair;
import com.example.authserver.repository.RsaKeyPairRepository;
import com.example.authserver.service.RsaKeyService;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RsaKeyServiceImpl implements RsaKeyService {

    private final RsaKeyPairRepository repo;

    @Override
    public List<RSAKey> loadKeys() {
        return repo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toRsaKey)
                .toList();
    }

    @Override
    public RSAKey rotate() {
        List<RsaKeyPair> existing = repo.findAllByOrderByCreatedAtDesc();
        existing.forEach(k -> k.setActive(false));
        repo.saveAll(existing);
        return toRsaKey(generateAndSave());
    }

    @Override
    public void ensureInitialized() {
        if (repo.count() == 0) generateAndSave();
    }

    private RsaKeyPair generateAndSave() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            java.security.KeyPair kp = gen.generateKeyPair();

            RsaKeyPair doc = new RsaKeyPair();
            doc.setKeyId(UUID.randomUUID().toString());
            doc.setPublicKeyPem(Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));
            doc.setPrivateKeyPem(Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));
            doc.setActive(true);
            doc.setCreatedAt(Instant.now());
            return repo.save(doc);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key", e);
        }
    }

    private RSAKey toRsaKey(RsaKeyPair doc) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPublicKey pub = (RSAPublicKey) kf.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(doc.getPublicKeyPem())));
            RSAPrivateKey priv = (RSAPrivateKey) kf.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(doc.getPrivateKeyPem())));
            return new RSAKey.Builder(pub).privateKey(priv).keyID(doc.getKeyId()).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA key: " + doc.getKeyId(), e);
        }
    }
}
