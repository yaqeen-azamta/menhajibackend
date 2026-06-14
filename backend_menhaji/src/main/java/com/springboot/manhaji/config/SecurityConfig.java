package com.springboot.manhaji.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

        .requestMatchers("/api/auth/**").permitAll()

        .requestMatchers(
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-ui.html"
        ).permitAll()

        .requestMatchers("/uploads/**").permitAll()

        .requestMatchers("/uploads/audio/**").authenticated()

        // Parents can take adaptive quizzes on behalf of a linked child.
        // Ownership of the selected child is enforced in AdaptiveQuizService.
        .requestMatchers("/api/quiz/adaptive/**").hasAnyRole("STUDENT", "PARENT", "ADMIN")

        .requestMatchers("/api/reports/**").hasAnyRole("STUDENT", "PARENT", "ADMIN")

        // ✅ QUESTIONS
        .requestMatchers("/api/questions/**").permitAll()

        // Student answers require auth — the endpoint reads the student id from the JWT.
        .requestMatchers("/api/student-answers/**").authenticated()

        .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()

        .requestMatchers("/app", "/app/**").permitAll()

        .requestMatchers("/assets/**", "/icons/**", "/canvaskit/**").permitAll()

        .requestMatchers("/api/admin/**").hasRole("ADMIN")

        .requestMatchers("/api/teacher/**").hasAnyRole("TEACHER", "ADMIN")

        .requestMatchers("/api/parent/**").hasAnyRole("PARENT", "ADMIN")
.requestMatchers("/api/student/**")
.hasAnyRole("STUDENT", "PARENT", "ADMIN")

        .requestMatchers("/api/progress/**").hasAnyRole("PARENT", "STUDENT", "ADMIN")

        .anyRequest().authenticated()
)

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
