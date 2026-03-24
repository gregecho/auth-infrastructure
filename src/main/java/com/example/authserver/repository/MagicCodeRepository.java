package com.example.authserver.repository;

import com.example.authserver.model.MagicCode;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MagicCodeRepository extends MongoRepository<MagicCode, String> {
    Optional<MagicCode> findByCodeAndUsedFalse(String code);
}
