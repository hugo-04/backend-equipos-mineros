package com.mineria.equipos.auth.security;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.mineria.equipos.shared.exception.ErrorResponseWriter;

import lombok.RequiredArgsConstructor;

/**
 * Reglas de seguridad: autenticación stateless con JWT. Públicos: login, health y Swagger.
 * Todo lo demás exige token, y borrar equipos exige además el rol ADMIN.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final long HSTS_SEGUNDOS = 31_536_000L;
    private static final String CSP_API = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";
    private static final String PERMISSIONS_POLICY = "geolocation=(), camera=(), microphone=(), payment=(), usb=()";
    private static final RequestMatcher RUTAS_SWAGGER = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui.html"),
            PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"));

    private final JwtService jwtService;
    private final SecurityErrorHandlers securityErrorHandlers;
    private final LoginRateLimiter loginRateLimiter;
    private final ErrorResponseWriter escritor;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/equipos/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .headers(cabeceras -> cabeceras
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true).maxAgeInSeconds(HSTS_SEGUNDOS)
                                // Siempre: detrás de un proxy TLS la petición interna llega por http
                                .requestMatcher(AnyRequestMatcher.INSTANCE))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                        .frameOptions(FrameOptionsConfig::deny)
                        // CSP de API pura: no carga nada y no se puede embeber. Swagger UI (solo en desarrollo) la necesita libre
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                new NegatedRequestMatcher(RUTAS_SWAGGER),
                                new StaticHeadersWriter("Content-Security-Policy", CSP_API)))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", PERMISSIONS_POLICY)))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(securityErrorHandlers)
                        .accessDeniedHandler(securityErrorHandlers))
                // Los filtros se crean aquí y no como @Bean: así Spring Boot no los registra dos veces
                .addFilterBefore(new LoginRateLimitFilter(loginRateLimiter, escritor), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
