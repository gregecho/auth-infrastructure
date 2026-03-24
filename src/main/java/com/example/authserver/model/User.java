package com.example.authserver.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String password;
    private Set<String> roles;
    private boolean enabled = true;
}
