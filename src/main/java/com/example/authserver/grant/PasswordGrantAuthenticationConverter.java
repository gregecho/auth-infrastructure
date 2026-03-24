package com.example.authserver.grant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.authserver.grant.GrantUtils.extractParameters;
import static com.example.authserver.grant.GrantUtils.throwInvalidRequest;

public class PasswordGrantAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!"password".equals(grantType)) return null;

        MultiValueMap<String, String> params = extractParameters(request);

        String username = params.getFirst(OAuth2ParameterNames.USERNAME);
        if (!StringUtils.hasText(username)) throwInvalidRequest(OAuth2ParameterNames.USERNAME);

        String password = params.getFirst(OAuth2ParameterNames.PASSWORD);
        if (!StringUtils.hasText(password)) throwInvalidRequest(OAuth2ParameterNames.PASSWORD);

        Set<String> scopes = new HashSet<>();
        String scopeParam = params.getFirst(OAuth2ParameterNames.SCOPE);
        if (StringUtils.hasText(scopeParam)) {
            scopes = Arrays.stream(scopeParam.split(" ")).collect(Collectors.toSet());
        }

        Map<String, Object> additional = params.entrySet().stream()
                .filter(e -> !Set.of(OAuth2ParameterNames.GRANT_TYPE, OAuth2ParameterNames.USERNAME,
                        OAuth2ParameterNames.PASSWORD, OAuth2ParameterNames.SCOPE).contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        return new PasswordGrantAuthenticationToken(clientPrincipal, username, password, scopes, additional);
    }
}
