package com.example.authserver.service;

public interface MagicCodeService {
    String issue(String username);
    String consume(String code);
}
