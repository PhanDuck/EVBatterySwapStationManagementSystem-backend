package com.example.EVBatterySwapStationManagementSystem.service;

import com.example.EVBatterySwapStationManagementSystem.entity.User;
import com.example.EVBatterySwapStationManagementSystem.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class TokenService {

    @Autowired
    private UserRepository userRepository;

    private final String SECRET_KEY = "HackHoTaoCaiHackHoTaoCaiHackHoTaoCaiHackHoTaoCaiHackHoTaoCaiHackHoTaoCai";

    public SecretKey getSignInKey(){
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generate token với User
    public String generateToken(User user){
        return Jwts.builder()
                .subject(user.getId() + "")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7)) // Sửa dấu * thành +
                .signWith(getSignInKey())
                .compact();
    }

    // Verify token - trả về User
    public User extractToken(String token) {
        String value = extractClaim(token, Claims::getSubject);
        long id = Long.parseLong(value);
        return userRepository.findUserById(id); // Đổi thành findUserById
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims,T> resolver){
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }
}