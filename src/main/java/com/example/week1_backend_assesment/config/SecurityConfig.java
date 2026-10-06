package com.example.week1_backend_assesment.config;

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

                        // Bank accounts
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/accounts",
                                "/api/accounts/**"
                        ).authenticated()

                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/accounts"
                        ).hasRole("ADMIN")

                        // Transactions
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/accounts/*/transactions"
                        ).hasRole("MAKER")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/accounts/*/transactions"
                        ).authenticated()

                        // Transfers
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/transfers"
                        ).hasRole("MAKER")

                        // Beneficiaries
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/api/beneficiaries/*"
                        ).hasAnyRole("ADMIN", "CHECKER")

                        .requestMatchers(
                                "/api/beneficiaries/**"
                        ).authenticated()

                        // Customers
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/customers",
                                "/api/customers/**"
                        ).authenticated()

                        .requestMatchers(
                                "/api/customers",
                                "/api/customers/**"
                        ).hasRole("ADMIN")

                        // Spring's error page, so error responses aren't turned into 401s
                        .requestMatchers("/error").permitAll()

                        // Anything not listed above needs a logged-in user
                        .anyRequest().authenticated()
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