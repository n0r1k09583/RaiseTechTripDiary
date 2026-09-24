package com.raisetech.tripdiary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.raisetech.tripdiary.domain.Follow;
import com.raisetech.tripdiary.domain.Profile;
import com.raisetech.tripdiary.domain.User;
import com.raisetech.tripdiary.mapper.FollowMapper;
import com.raisetech.tripdiary.mapper.UserMapper;
import com.raisetech.tripdiary.web.ApiException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

  @Mock
  FollowMapper follows;

  @Mock
  UserMapper users;

  FollowService service;

  @BeforeEach
  void setUp() {
    service = new FollowService(follows, users);
  }

  @Test
  void 空の検索はSQLを叩かない() {
    assertThat(service.search(1L, "   ").getUsers()).isEmpty();
    assertThat(service.search(1L, "@@@").getUsers()).isEmpty();
  }

  @Test
  void かな検索はローマ字とカタカナも渡す() {
    when(users.search(anyList(), eq(1L))).thenReturn(List.of());
    service.search(1L, "やまだ");
    verify(users)
        .search(
            argThat(
                patterns ->
                    patterns.contains("%やまだ%")
                        && patterns.contains("%ヤマダ%")
                        && patterns.contains("%yamada%")),
            eq(1L));
  }

  @Test
  void アットマーク付きの検索語は外して渡す() {
    when(users.search(List.of("%yamada%"), 1L)).thenReturn(List.of());
    service.search(1L, "  @yamada  ");
    verify(users).search(List.of("%yamada%"), 1L);
  }

  @Test
  void 自分自身はフォローできない() {
    User me = new User();
    me.setId(1L);
    me.setUsername("yamada");
    when(users.findByUsername("yamada")).thenReturn(me);
    assertThatThrownBy(() -> service.follow(1L, "yamada"))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getStatus())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void 無いユーザーは404() {
    when(users.findProfile("nobody", 1L)).thenReturn(null);
    assertThatThrownBy(() -> service.profile(1L, "nobody"))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getStatus())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void 既にフォロー中なら追加しない() {
    User them = new User();
    them.setId(2L);
    them.setUsername("hanako");
    when(users.findByUsername("hanako")).thenReturn(them);
    when(follows.find(1L, 2L)).thenReturn(new Follow());
    Profile profile = new Profile();
    profile.setId(2L);
    profile.setUsername("hanako");
    profile.setDisplayName("佐藤 花子");
    profile.setFollowedByMe(true);
    when(users.findProfile("hanako", 1L)).thenReturn(profile);
    assertThat(service.follow(1L, "hanako").isFollowedByMe()).isTrue();
  }
}
