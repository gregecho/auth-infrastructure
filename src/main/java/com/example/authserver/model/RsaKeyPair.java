package com.example.authserver.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "rsa_keys")
public class RsaKeyPair {
    @Id
    private String keyId;
    private String publicKeyPem;
    private String privateKeyPem;
    private boolean active;
    private Instant createdAt;
}
