package com.example.EVBatterySwapStationManagementSystem.config;

import com.example.EVBatterySwapStationManagementSystem.exception.exceptions.AuthenticationException;
import com.example.EVBatterySwapStationManagementSystem.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    @Autowired
    private TokenService tokenService;

    private final List<String> PUBLIC_API = List.of(
            "POST:/api/auth/register",
            "POST:/api/auth/login",
            "GET:/api/station/**"
    );

    public boolean isPublicAPI(String uri, String method) {
        AntPathMatcher matcher = new AntPathMatcher();

        return PUBLIC_API.stream().anyMatch(pattern -> {
            String[] parts = pattern.split(":", 2);
            if (parts.length != 2) return false;

            String allowedMethod = parts[0];
            String allowedUri = parts[1];

            return method.equals(allowedMethod) && matcher.match(allowedUri, uri);
        });
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        if(isPublicAPI(uri, method)){
            filterChain.doFilter(request, response);
        } else {
            String token = getToken(request);
            if(token == null) {
                resolver.resolveException(request, response, null, new AuthenticationException("Empty token!"));
                return;
            }
            // TODO: Thêm logic verify token và set authentication
            filterChain.doFilter(request, response);
        }
    }

    private String getToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7);
    }
}