package com.raisetech.tripdiary.service;

import com.raisetech.tripdiary.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  static final String ACCESS_TYPE = "access";

  private final SecretKey key;
  private final long accessExpirationMs;

  public JwtService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-expiration-ms}") long accessExpirationMs) {
    this.key = Keys.hmacShaKeyFor(sha256(secret));
    this.accessExpirationMs = accessExpirationMs;
  }

  public String issueAccess(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .claim("typ", ACCESS_TYPE)
        .claim("username", user.getUsername())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(accessExpirationMs)))
        .signWith(key)
        .compact();
  }

  public Claims parseAccess(String token) {
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    if (!ACCESS_TYPE.equals(claims.get("typ", String.class))) {
      throw new JwtException("not an access token");
    }
    return claims;
  }

  private static byte[] sha256(String secret) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
