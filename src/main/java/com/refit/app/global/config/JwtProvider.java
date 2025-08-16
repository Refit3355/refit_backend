package com.refit.app.global.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private final byte[] secretKey;
    private final String issuer;
    private final long accessExpMillis;
    private final long refreshExpMillis;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-exp-min}") long accessExpMin,
            @Value("${jwt.refresh-exp-days}") long refreshExpDays
    ) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.issuer = issuer;
        this.accessExpMillis = accessExpMin * 60_000L;
        this.refreshExpMillis = refreshExpDays * 24L * 60L * 60L * 1000L;
    }

    public String createAccessToken(Long userId, String email, String nickname) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpMillis);

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("nickname", nickname);
        claims.put("typ", "access");

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(String.valueOf(userId))
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(Keys.hmacShaKeyFor(secretKey), SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExpMillis);

        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "refresh");

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(String.valueOf(userId))
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(Keys.hmacShaKeyFor(secretKey), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isRefreshToken(String token) {
        Object typ = parseClaims(token).get("typ");
        return "refresh".equals(typ);
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

}
