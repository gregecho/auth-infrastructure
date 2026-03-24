package com.example.authserver.service.Impl;

import com.example.authserver.model.MagicCode;
import com.example.authserver.repository.MagicCodeRepository;
import com.example.authserver.service.MagicCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class MagicCodeServiceImpl implements MagicCodeService {

    private final MagicCodeRepository repo;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String issue(String username) {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        MagicCode mc = new MagicCode();
        mc.setCode(code);
        mc.setUsername(username);
        mc.setExpiresAt(Instant.now().plusSeconds(900));
        repo.save(mc);
        return code;
    }

    @Override
    public String consume(String code) {
        MagicCode mc = repo.findByCodeAndUsedFalse(code)
                .orElseThrow(() -> new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT));
        if (Instant.now().isAfter(mc.getExpiresAt())) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }
        mc.setUsed(true);
        repo.save(mc);
        return mc.getUsername();
    }
}
