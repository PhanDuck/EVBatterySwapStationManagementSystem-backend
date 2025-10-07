package com.evbs.BackEndEvBs.config;


import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.service.TokenService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Component
public class Filter extends OncePerRequestFilter {

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    @Autowired
    TokenService tokenService;

    private final List<String> PUBLIC_API = List.of(
            "POST:/api/register",
            "POST:/api/login",
            "GET:/swagger-ui/**",
            "GET:/v3/api-docs/**",
            "GET:/swagger-resources/**",
            "GET:/api/station/**"
    );

    public boolean isPublicAPI(String uri, String method) {
        AntPathMatcher matcher = new AntPathMatcher();



        return PUBLIC_API.stream().anyMatch(pattern -> {
            String[] parts = pattern.split(":", 2);
            if (parts.length != 2) return false;

            String allowedMethod = parts[0];
            String allowedUri = parts[1];

            return matcher.match(allowedUri, uri);
        });
    }



    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("filter running...");
        String uri = request.getRequestURI();
        String method = request.getMethod();


        if(isPublicAPI(uri, method)){
            //api phulic
            //=>access
            filterChain.doFilter(request, response);
        }else {
            //api theo role
            //check xem co quyeen truy cap khoong
            String token = getToken(request);

            if (token == null){
                resolver.resolveException(request, response, null, new AuthenticationException("Empty token!"));
            }

            User user = null;
            try {
                user = tokenService.extractToken(token);
            } catch (ExpiredJwtException expiredJwtException) {
                expiredJwtException.printStackTrace();
                resolver.resolveException(request, response, null, new AuthenticationException("Expired token!"));
            } catch (MalformedJwtException malformedJwtException) {
                resolver.resolveException(request, response, null, new AuthenticationException("invalid token!"));

            }

            UsernamePasswordAuthenticationToken
                    authenToken =
                    new UsernamePasswordAuthenticationToken(user, token, user.getAuthorities());
            authenToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenToken);
            filterChain.doFilter(request, response);

        }

    }

    public String getToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.substring(7);
    }

}
