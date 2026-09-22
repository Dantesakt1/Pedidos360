package com.pedidos360.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Habilitamos CORS indicando que use el Bean creado más abajo
            .cors(Customizer.withDefaults())
            
            // Desactivamos CSRF porque nuestra API será consumida con tokens (sin estado)
            .csrf(csrf -> csrf.disable())
            
            // Configuramos las reglas de acceso exigidas por el encargo
            .authorizeHttpRequests(auth -> auth
                // Proteger los endpoints: se requiere un token válido para acceder
             //   .requestMatchers("/api/orders/**").authenticated()
               // .requestMatchers("/api/catalog/products/**").authenticated()
                
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

    // 2. Creamos la configuración CORS para permitir que Angular se conecte
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permite la conexión desde el puerto por defecto de Angular
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        // Permite los métodos HTTP necesarios para tu CRUD
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Permite el envío de todas las cabeceras (importante para enviar el Authorization: Bearer token)
        configuration.setAllowedHeaders(List.of("*"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica esta regla a todos los endpoints de tu API
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}