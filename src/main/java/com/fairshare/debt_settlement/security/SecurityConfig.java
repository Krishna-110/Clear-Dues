package com.fairshare.debt_settlement.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    // Inject both the Bouncer and the Success Bridge
    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                // 1. Disable CSRF (We use JWTs, so we don't need this)
                .csrf(csrf -> csrf.disable())

                // 2. Configure Endpoint Rules
                .authorizeHttpRequests(auth -> auth
                        // Allow login routes, error pages, and static assets
                        .requestMatchers("/error", "/", "/login**", "/oauth2/**", "/favicon.ico").permitAll()
                        // Lock down all actual data APIs
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )

                // 3. Stateless Sessions (Crucial for APIs interacting with Mobile Apps)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. Enable OAuth2 Login and attach our custom Success Handler.
                //    Force Google's account chooser every time so re-login after logout
                //    always asks which account (instead of silently reusing the session).
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(
                                        accountChooserResolver(clientRegistrationRepository)))
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                // 5. Add our custom JWT filter BEFORE the standard Spring filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Adds prompt=select_account to the Google authorization request so the user is always
    // shown the account picker, even if they're still signed in to Google in the browser.
    private OAuth2AuthorizationRequestResolver accountChooserResolver(
            ClientRegistrationRepository repo) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(customizer ->
                customizer.additionalParameters(params -> params.put("prompt", "select_account")));
        return resolver;
    }
}