package com.refit.app.global.config;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final String issuer;
    private final long accessExpMillis;
    private final long refreshExpMillis;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-exp-min}") long accessExpMin,
            @Value("${jwt.refresh-exp-days}") long refreshExpDays
    ) {

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secretBytes);

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
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
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
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token);
    }

    public boolean isRefreshToken(Jws<Claims> jws) {
        Object typ = jws.getBody().get("typ");
        return "refresh".equals(typ);
    }

    public Long getUserId(Jws<Claims> jws) {
        return Long.valueOf(jws.getBody().getSubject());
    }

    public Date getExpiration(Jws<Claims> jws) {
        return jws.getBody().getExpiration();
    }

}
