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

public class MagicCodeGrantAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!"magic_code".equals(grantType)) return null;

        MultiValueMap<String, String> params = extractParameters(request);

        String code = params.getFirst("code");
        if (!StringUtils.hasText(code)) throwInvalidRequest("code");

        Set<String> scopes = new HashSet<>();
        String scopeParam = params.getFirst(OAuth2ParameterNames.SCOPE);
        if (StringUtils.hasText(scopeParam)) {
            scopes = Arrays.stream(scopeParam.split(" ")).collect(Collectors.toSet());
        }

        Map<String, Object> additional = params.entrySet().stream()
                .filter(e -> !Set.of(OAuth2ParameterNames.GRANT_TYPE, "code", OAuth2ParameterNames.SCOPE).contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        return new MagicCodeGrantAuthenticationToken(clientPrincipal, code, scopes, additional);
    }
}
