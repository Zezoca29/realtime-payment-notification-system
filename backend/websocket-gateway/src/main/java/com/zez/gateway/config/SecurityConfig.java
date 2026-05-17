package com.zez.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the WebSocket Gateway.
 *
 * <p>Authentication strategy:
 * <ul>
 *   <li>{@code /internal/**} — protected by {@link com.zez.gateway.security.InternalApiKeyFilter}
 *       (service-to-service API key checked before this chain runs)</li>
 *   <li>{@code /ws/**} and {@code /topic/**} — open for browser WebSocket connections
 *       (JWT handshake auth is on the V2 roadmap)</li>
 *   <li>{@code /actuator/**} — restricted to localhost / internal network via network policy</li>
 * </ul>
 *
 * <p>CSRF is disabled because all non-browser clients use bearer credentials or API keys,
 * and the WebSocket endpoint relies on origin-based SockJS validation.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/actuator/**").denyAll()  // block external metrics scraping
                    .anyRequest().permitAll()
            );
        return http.build();
    }
}
