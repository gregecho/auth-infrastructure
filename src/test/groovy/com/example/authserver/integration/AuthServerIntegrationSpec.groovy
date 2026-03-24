package com.example.authserver.integration

import com.example.authserver.repository.MongoRegisteredClientRepository
import com.example.authserver.repository.UserRepository
import com.example.authserver.service.MongoRegisteredClientService
import com.example.authserver.service.MongoUserDetailsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import spock.lang.Specification

@SpringBootTest
class AuthServerIntegrationSpec extends Specification {

    @Autowired
    MongoRegisteredClientRepository registeredClientRepository

    @Autowired
    UserRepository userRepository

    @Autowired
    MongoRegisteredClientService registeredClientService

    @Autowired
    MongoUserDetailsService userDetailsService

    def cleanup() {
        registeredClientRepository.deleteAll()
        userRepository.deleteAll()
    }

    def "save and retrieve RegisteredClient from MongoDB"() {
        given:
        def client = RegisteredClient.withId("integ-1")
                .clientId("integ-client")
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("read")
                .build()

        when:
        registeredClientService.save(client)
        def found = registeredClientService.findByClientId("integ-client")

        then:
        found != null
        found.clientId == "integ-client"
        found.scopes == ["read"] as Set
    }

    def "DataInitializer seeds admin user and test-client on startup"() {
        expect:
        userRepository.findByUsername("admin").isPresent()
        registeredClientService.findByClientId("test-client") != null
    }
}
