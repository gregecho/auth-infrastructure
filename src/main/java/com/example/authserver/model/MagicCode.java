package com.example.authserver.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "magic_codes")
public class MagicCode {
    @Id
    private String id;
    @Indexed(unique = true)
    private String code;
    private String username;
    private Instant expiresAt;
    private boolean used = false;
}
