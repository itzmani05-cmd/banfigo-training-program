package com.example.banfigo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // Bank staff see every customer's data; CUSTOMER is a self-registered user limited to their own
    // (enforced in the services through CurrentUser)
    private static final String[] STAFF = {"ADMIN", "MAKER", "CHECKER"};
    private static final String CUSTOMER = "CUSTOMER";
    private static final String[] ANY_USER = {"ADMIN", "MAKER", "CHECKER", CUSTOMER};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/health",
                                "/api/info"
                        ).permitAll()

                        // The logged-in customer's own record
                        .requestMatchers("/api/me").hasRole(CUSTOMER)

                        // Bank accounts (customers only get their own; the services filter)
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/accounts",
                                "/api/accounts/**"
                        ).hasAnyRole(ANY_USER)

                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/accounts"
                        ).hasRole("ADMIN")

                        // Transactions
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/accounts/*/transactions"
                        ).hasRole("MAKER")

                        // Transfers: staff, or a customer paying from their own account
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/transfers"
                        ).hasAnyRole("MAKER", CUSTOMER)

                        // Beneficiaries: a customer manages their own list
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/api/beneficiaries/*"
                        ).hasAnyRole("ADMIN", "CHECKER", CUSTOMER)

                        .requestMatchers(
                                "/api/beneficiaries/**"
                        ).hasAnyRole(ANY_USER)

                        // Consents: MAKER raises a request, CHECKER / ADMIN decides on it
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/consents"
                        ).hasRole("MAKER")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/consents/*/approve",
                                "/api/consents/*/reject",
                                "/api/consents/*/revoke"
                        ).hasAnyRole("ADMIN", "CHECKER")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/consents",
                                "/api/consents/**"
                        ).hasAnyRole(ANY_USER)

                        // Customers: the bank's customer list is for staff only
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/customers",
                                "/api/customers/**"
                        ).hasAnyRole(STAFF)

                        .requestMatchers(
                                "/api/customers",
                                "/api/customers/**"
                        ).hasRole("ADMIN")

                        // Spring's error page, so error responses aren't turned into 401s
                        .requestMatchers("/error").permitAll()

                        // Anything not listed above (e.g. the dashboard) needs one of the app's roles
                        .anyRequest().hasAnyRole(ANY_USER)
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {

        return jwt -> {

            Map<String, Object> realmAccess =
                    jwt.getClaimAsMap("realm_access");

            Collection<GrantedAuthority> authorities = new ArrayList<>();

            if (realmAccess != null) {

                Object rolesObject = realmAccess.get("roles");

                if (rolesObject instanceof Collection<?> roles) {

                    for (Object role : roles) {

                        authorities.add(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role.toString()
                                )
                        );
                    }
                }
            }

            return new JwtAuthenticationToken(jwt, authorities);
        };
    }
}