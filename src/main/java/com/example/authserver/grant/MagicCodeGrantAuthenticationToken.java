package com.example.authserver.grant;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;
import java.util.Set;

public class MagicCodeGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    public static final AuthorizationGrantType MAGIC_CODE = new AuthorizationGrantType("magic_code");

    private final String code;
    private final Set<String> scopes;

    public MagicCodeGrantAuthenticationToken(Authentication clientPrincipal,
                                             String code,
                                             Set<String> scopes,
                                             Map<String, Object> additionalParameters) {
        super(MAGIC_CODE, clientPrincipal, additionalParameters);
        this.code = code;
        this.scopes = scopes;
    }

    public String getCode() { return code; }
    public Set<String> getScopes() { return scopes; }
}
