package com.example.authserver.service

import com.example.authserver.model.MongoRegisteredClient
import com.example.authserver.repository.MongoRegisteredClientRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import spock.lang.Specification
import spock.lang.Subject

class MongoRegisteredClientServiceSpec extends Specification {

    MongoRegisteredClientRepository repo = Mock()
    ObjectMapper objectMapper = new ObjectMapper()

    @Subject
    MongoRegisteredClientService service = new MongoRegisteredClientService(repo, objectMapper)

    def "findByClientId returns null when not found"() {
        given:
        repo.findByClientId("unknown") >> Optional.empty()

        expect:
        service.findByClientId("unknown") == null
    }

    def "save persists a RegisteredClient"() {
        given:
        def client = RegisteredClient.withId("id-1")
                .clientId("my-client")
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("read")
                .build()

        when:
        service.save(client)

        then:
        1 * repo.save({ MongoRegisteredClient doc ->
            doc.clientId == "my-client" && doc.scopes == ["read"] as Set
        })
    }

    def "findById returns mapped RegisteredClient"() {
        given:
        def doc = new MongoRegisteredClient(
                id: "id-1",
                clientId: "my-client",
                clientSecret: "{noop}secret",
                clientName: "my-client",
                clientAuthenticationMethods: ["client_secret_basic"] as Set,
                authorizationGrantTypes: ["client_credentials"] as Set,
                redirectUris: [] as Set,
                postLogoutRedirectUris: [] as Set,
                scopes: ["read"] as Set,
                clientSettings: objectMapper.writeValueAsString(ClientSettings.builder().build().settings),
                tokenSettings: objectMapper.writeValueAsString(TokenSettings.builder().build().settings)
        )
        repo.findById("id-1") >> Optional.of(doc)

        when:
        def result = service.findById("id-1")

        then:
        result.clientId == "my-client"
        result.scopes == ["read"] as Set
    }
}
