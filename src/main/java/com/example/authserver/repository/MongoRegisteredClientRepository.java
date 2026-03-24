package com.example.authserver.repository;

import com.example.authserver.model.MongoRegisteredClient;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MongoRegisteredClientRepository extends MongoRepository<MongoRegisteredClient, String> {
    Optional<MongoRegisteredClient> findByClientId(String clientId);
}
