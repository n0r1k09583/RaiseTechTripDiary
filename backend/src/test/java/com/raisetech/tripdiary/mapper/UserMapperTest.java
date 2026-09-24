package com.raisetech.tripdiary.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raisetech.tripdiary.domain.User;
import com.raisetech.tripdiary.domain.UserSummary;
import com.raisetech.tripdiary.support.MapperH2Test;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@MapperH2Test
class UserMapperTest {

  @Autowired
  UserMapper users;

  @Test
  void 無いユーザーはnull() {
    assertThat(users.findByEmail("nobody@example.com")).isNull();
    assertThat(users.findByUsername("nobody")).isNull();
    assertThat(users.findById(9999)).isNull();
  }

  @Test
  void 挿入後にユーザー名で引ける() {
    User user = insert("one@example.com", "one_user", "一人");
    assertThat(users.findByUsername("one_user").getEmail()).isEqualTo("one@example.com");
    assertThat(users.findById(user.getId()).getPasswordDigest()).isEqualTo("hash");
    assertThat(users.findByEmail("ONE@example.com")).isNull();
  }

  @Test
  void ユーザー名の重複はDBが拒否する() {
    insert("a@example.com", "same_name", "A");
    assertThatThrownBy(() -> insert("b@example.com", "same_name", "B"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void メールの重複はDBが拒否する() {
    insert("same@example.com", "user_a", "A");
    assertThatThrownBy(() -> insert("same@example.com", "user_b", "B"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 複数パターンでユーザーを探せる() {
    insert("kana@example.com", "yamada_kana", "山田");
    assertThat(users.search(List.of("%yamada_kana%"), 1L))
        .extracting(UserSummary::getUsername)
        .contains("yamada_kana");
    assertThat(users.search(List.of("%やまだ%", "%ヤマダ%", "%yamada_kana%"), 1L))
        .extracting(UserSummary::getUsername)
        .contains("yamada_kana");
  }

  private User insert(String email, String username, String displayName) {
    User user = new User();
    user.setEmail(email);
    user.setUsername(username);
    user.setDisplayName(displayName);
    user.setPasswordDigest("hash");
    users.insert(user);
    return user;
  }
}
