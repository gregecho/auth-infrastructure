package com.example.authserver.repository;

import com.example.authserver.model.RsaKeyPair;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RsaKeyPairRepository extends MongoRepository<RsaKeyPair, String> {
    List<RsaKeyPair> findAllByOrderByCreatedAtDesc();
}
