package com.fairshare.debt_settlement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@AllArgsConstructor// Tells Spring to manage this class as a bean
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;



    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Look for the "Authorization" header in the incoming HTTP request
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. If there is no header, or it doesn't start with "Bearer ",
        // ignore it and pass the request down the chain (it might be a public endpoint like /login)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the JWT (Remove the first 7 characters: "Bearer ")
        jwt = authHeader.substring(7);

        // 4-8. Parsing/validating the token can throw (expired, malformed, tampered - every user
        // eventually sends an expired token) and loadUserByUsername throws if the token's email no
        // longer maps to an account. This runs inside the servlet filter chain, BEFORE
        // DispatcherServlet/@RestControllerAdvice ever sees the request, so GlobalExceptionHandler
        // can never catch it here - an uncaught exception would escape as a raw container error
        // instead of a clean 401. Treat any failure as "just not authenticated" and move on; Spring
        // Security's normal authorization step then returns a proper 401/403 for a protected endpoint.
        try {
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (RuntimeException e) {
            // Expired/malformed/tampered token, or the account no longer exists - request continues
            // unauthenticated; Spring Security handles the resulting 401/403 normally.
            SecurityContextHolder.clearContext();
        }

        // 9. Continue the filter chain so the request can hit the Controller
        filterChain.doFilter(request, response);
    }
}