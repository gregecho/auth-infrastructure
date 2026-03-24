package com.example.authserver.service

import com.example.authserver.model.User
import com.example.authserver.repository.UserRepository
import org.springframework.security.core.userdetails.UsernameNotFoundException
import spock.lang.Specification
import spock.lang.Subject

class MongoUserDetailsServiceSpec extends Specification {

    UserRepository userRepository = Mock()

    @Subject
    MongoUserDetailsService service = new MongoUserDetailsService(userRepository)

    def "loadUserByUsername returns UserDetails for existing user"() {
        given:
        def user = new User(username: "alice", password: "{noop}pass", roles: ["USER"] as Set, enabled: true)
        userRepository.findByUsername("alice") >> Optional.of(user)

        when:
        def details = service.loadUserByUsername("alice")

        then:
        details.username == "alice"
        details.authorities*.authority == ["ROLE_USER"]
    }

    def "loadUserByUsername throws when user not found"() {
        given:
        userRepository.findByUsername("ghost") >> Optional.empty()

        when:
        service.loadUserByUsername("ghost")

        then:
        thrown(UsernameNotFoundException)
    }
}
