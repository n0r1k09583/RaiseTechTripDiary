package com.raisetech.tripdiary.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.raisetech.tripdiary.domain.RefreshToken;
import com.raisetech.tripdiary.domain.User;
import com.raisetech.tripdiary.support.MapperH2Test;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@MapperH2Test
class RefreshTokenMapperTest {

  @Autowired
  RefreshTokenMapper tokens;

  @Autowired
  UserMapper users;

  @Test
  void 期限切れリフレッシュは無効() {
    User user = user();
    RefreshToken row = new RefreshToken();
    row.setUserId(user.getId());
    row.setTokenHash("abc");
    row.setExpiresAt(1_000L);
    row.setCreatedAt(1L);
    tokens.insert(row);

    assertThat(tokens.findValidByHash("abc", 2_000L)).isNull();
    assertThat(tokens.findValidByHash("abc", 500L)).isNotNull();
    assertThat(tokens.findValidByHash("missing", 500L)).isNull();
  }

  @Test
  void ハッシュ削除はその行だけ() {
    User user = user();
    insert(user.getId(), "keep");
    insert(user.getId(), "drop");
    tokens.deleteByHash("drop");
    assertThat(tokens.findValidByHash("keep", 0L)).isNotNull();
    assertThat(tokens.findValidByHash("drop", 0L)).isNull();
  }

  private User user() {
    User user = new User();
    user.setEmail("rt@example.com");
    user.setUsername("rt_user");
    user.setDisplayName("RT");
    user.setPasswordDigest("hash");
    users.insert(user);
    return user;
  }

  private void insert(long userId, String hash) {
    RefreshToken row = new RefreshToken();
    row.setUserId(userId);
    row.setTokenHash(hash);
    row.setExpiresAt(9_999_999_999L);
    row.setCreatedAt(1L);
    tokens.insert(row);
  }
}
