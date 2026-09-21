package com.pedidos360.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Desactivamos CSRF porque nuestra API será consumida con tokens (sin estado)
            .csrf(csrf -> csrf.disable())
            
            // Configuramos las reglas de acceso exigidas por el encargo
            .authorizeHttpRequests(auth -> auth
                // Proteger los endpoints: se requiere un token válido para acceder
                .requestMatchers("/api/orders/**").authenticated()
                .requestMatchers("/api/catalog/products/**").authenticated()
                
                // Si en el futuro necesitas validar roles específicos (ej. que solo el Admin cree productos),
                // se haría así: .requestMatchers(HttpMethod.POST, "/api/catalog/products").hasAuthority("SCOPE_Admin")
                
                .anyRequest().permitAll()
            )
            
            // Habilitamos que Spring Security intercepte y valide el JWT usando la URL de Entra ID
            // configurada en el application.properties
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            );

        return http.build();
    }
}