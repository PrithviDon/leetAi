package com.leetai.config;

import com.leetai.security.CustomOAuth2UserService;
import com.leetai.security.JwtAuthFilter;
import com.leetai.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                           OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                           JwtAuthFilter jwtAuthFilter) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless for the JWT-protected API; the OAuth2 login dance
            // itself still uses a transient session internally (required by
            // Spring Security to carry state/nonce during the redirect).
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // Public reads — anyone can browse published problems.
                .requestMatchers(HttpMethod.GET, "/api/problems", "/api/problems/*").permitAll()
                // Admin-only writes and the draft-inclusive admin listing.
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/problems").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/problems/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/problems/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/problems/*/**").hasRole("ADMIN")
                // Submitting a solution, and viewing your own submission
                // history, both require being logged in (any role).
                .requestMatchers(HttpMethod.POST, "/api/problems/*/submit").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/problems/*/submissions").authenticated()
                .requestMatchers("/api/auth/me").authenticated()
                // OAuth2 login endpoints must stay open — that's the login flow itself.
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2LoginSuccessHandler)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
