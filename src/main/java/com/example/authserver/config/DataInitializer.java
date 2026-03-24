package com.example.authserver.config;

import com.example.authserver.model.User;
import com.example.authserver.repository.UserRepository;
import com.example.authserver.service.MongoRegisteredClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MongoRegisteredClientService registeredClientService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initUser();
        initClient();
    }

    private void initUser() {
        if (userRepository.findByUsername("admin").isPresent()) return;

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode("admin123"));
        user.setRoles(Set.of("ADMIN", "USER"));
        userRepository.save(user);
        log.info("Created test user: admin / admin123");
    }

    private void initClient() {
        if (registeredClientService.findByClientId("test-client") != null) return;

        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("test-client")
                .clientSecret(passwordEncoder.encode("test-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://localhost:8080/login/oauth2/code/test-client")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("read")
                .scope("write")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .build();

        registeredClientService.save(client);
        log.info("Created test client: test-client / test-secret");
    }
}
