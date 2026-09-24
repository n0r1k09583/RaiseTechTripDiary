package com.raisetech.tripdiary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raisetech.tripdiary.domain.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  @Test
  void 期限切れアクセストークンは拒否する() throws Exception {
    JwtService jwt = new JwtService("unit-test-secret-value", 1);
    User user = new User();
    user.setId(1L);
    user.setUsername("yamada");
    String token = jwt.issueAccess(user);
    Thread.sleep(20);
    assertThatThrownBy(() -> jwt.parseAccess(token)).isInstanceOf(ExpiredJwtException.class);
  }

  @Test
  void 壊れたJWTは拒否する() {
    JwtService jwt = new JwtService("unit-test-secret-value", 60_000);
    assertThatThrownBy(() -> jwt.parseAccess("not-a-jwt")).isInstanceOf(JwtException.class);
  }

  @Test
  void refreshのtypはアクセスとして使えない() throws Exception {
    JwtService jwt = new JwtService("unit-test-secret-value", 60_000);
    byte[] key =
        MessageDigest.getInstance("SHA-256").digest("unit-test-secret-value".getBytes(StandardCharsets.UTF_8));
    String refreshLike =
        Jwts.builder().subject("1").claim("typ", "refresh").signWith(Keys.hmacShaKeyFor(key)).compact();
    assertThatThrownBy(() -> jwt.parseAccess(refreshLike)).isInstanceOf(JwtException.class);
  }

  @Test
  void 発行したアクセスはsubjectを持つ() {
    JwtService jwt = new JwtService("unit-test-secret-value", 60_000);
    User user = new User();
    user.setId(42L);
    user.setUsername("yamada");
    assertThat(jwt.parseAccess(jwt.issueAccess(user)).getSubject()).isEqualTo("42");
  }
}
