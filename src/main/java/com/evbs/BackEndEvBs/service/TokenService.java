package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.repository.AuthenticationRepository;
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

    private final String SECRET_KEY = "HackHoTaoCaiHackHoTaoCaiHackHoTaoCaiHackHoTaoCaiHackHoTaoCaiHackHoTaoCai";

    @Autowired
    AuthenticationRepository authenticationRepository;

    public SecretKey getSignInKey(){
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    //generate token
    public String generateToken(User user){
        return Jwts.builder()
                .subject(user.getId() + "")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000*60*60*24*7))
                .signWith(getSignInKey())
                .compact();


    }

    //verify

    public User extractToken(String token) {
        String value = extractClaim(token, Claims::getSubject);
        long id = Long.parseLong(value);
        return authenticationRepository.findUserById(id);
    }

    public Claims extractAllClaims(String token) {
        return  Jwts.parser().
                verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    public <T> T extractClaim(String token, Function<Claims,T> resolver){
        Claims claims = extractAllClaims(token);
        return  resolver.apply(claims);
    }

}
