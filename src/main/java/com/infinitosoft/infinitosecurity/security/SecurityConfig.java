package com.infinitosoft.infinitosecurity.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors(Customizer.withDefaults())
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                    .antMatchers("/auth/**", "/roles/**", "/actuator/**", "/backup/**").permitAll()
                    .anyRequest().authenticated()
                .and()
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:*}") String allowedOriginsProp,
            @Value("${cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}") String allowedMethodsProp,
            @Value("${cors.allowed-headers:Authorization,authorization,Content-Type,Accept,X-Requested-With,token}") String allowedHeadersProp,
            @Value("${cors.exposed-headers:Authorization,authorization}") String exposedHeadersProp,
            @Value("${cors.allow-credentials:false}") boolean allowCredentials
    ) {
        CorsConfiguration config = new CorsConfiguration();
        for (String o : allowedOriginsProp.split(",")) config.addAllowedOriginPattern(o.trim());
        for (String m : allowedMethodsProp.split(",")) config.addAllowedMethod(m.trim());
        for (String h : allowedHeadersProp.split(",")) config.addAllowedHeader(h.trim());
        for (String h : exposedHeadersProp.split(",")) config.addExposedHeader(h.trim());
        config.setAllowCredentials(allowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
