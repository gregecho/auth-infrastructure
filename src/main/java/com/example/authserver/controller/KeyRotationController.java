package com.example.authserver.controller;

import com.example.authserver.service.RsaKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/keys")
@RequiredArgsConstructor
public class KeyRotationController {

    private final RsaKeyService rsaKeyService;

    @PostMapping("/rotate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> rotate() {
        String newKeyId = rsaKeyService.rotate().getKeyID();
        return ResponseEntity.ok(Map.of("newKeyId", newKeyId));
    }
}
