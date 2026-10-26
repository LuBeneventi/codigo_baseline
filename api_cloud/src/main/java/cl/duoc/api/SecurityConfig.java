package cl.duoc.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${spring.security.enabled:true}")
    private boolean securityEnabled;

    @Value("${security.jwt.secret:changeme}")
    private String jwtSecret;

    @Bean
    // BASELINE-VULN [SEC-REQ-008]: BFF sin ningun limitador de peticiones (rate limiting)
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS
            ));

        if (!securityEnabled) {
            // Modo Desarrollo / Pruebas Rápidas: permite todas las peticiones sin exigir Bearer token
            http.authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        } else {
            http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
                    .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth ->
                    oauth.jwt(Customizer.withDefaults())
                );
        }

        return http.build();
    }

    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
        byte[] bytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(bytes, "HmacSHA256");
        return org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // BASELINE-VULN [SEC-REQ-013]: configuracion CORS permisiva con comodin wildcard (*) y ausencia de Content-Security-Policy
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000", "*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-User-Role"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}