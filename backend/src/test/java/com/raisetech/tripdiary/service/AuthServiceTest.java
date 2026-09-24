package com.raisetech.tripdiary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.raisetech.tripdiary.domain.RefreshToken;
import com.raisetech.tripdiary.domain.User;
import com.raisetech.tripdiary.dto.LoginRequest;
import com.raisetech.tripdiary.dto.SignupRequest;
import com.raisetech.tripdiary.mapper.RefreshTokenMapper;
import com.raisetech.tripdiary.mapper.UserMapper;
import com.raisetech.tripdiary.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  UserMapper users;

  @Mock
  RefreshTokenMapper refreshTokens;

  JwtService jwt;
  AuthService auth;

  @BeforeEach
  void setUp() {
    jwt = new JwtService("unit-test-secret-value", 60_000);
    auth = new AuthService(users, refreshTokens, jwt, 86_400_000);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ab", "abcdefghijklmnopqrstu", "ab-c", "ab c"})
  void ユーザー名が仕様外なら登録できない(String username) {
    SignupRequest request = signup(username, "表示", "a@example.com", "password123", "password123");
    assertApi(HttpStatus.BAD_REQUEST, "ユーザー名は3〜20文字の半角英小文字・数字・_です", () -> auth.signup(request));
    verify(users, never()).insert(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "abcdefghijklmnopqrst", "user_1"})
  void ユーザー名の境界は登録できる(String username) {
    stubInsert(username, "a@example.com", "表示");
    SignupRequest request = signup(username, "表示", "a@example.com", "password123", "password123");
    assertThat(auth.signup(request).getUser().getUsername()).isEqualTo(username);
  }

  @Test
  void ユーザー名は小文字化してから判定する() {
    stubInsert("yamada", "a@example.com", "表示");
    SignupRequest request = signup("Yamada", "表示", "a@example.com", "password123", "password123");
    auth.signup(request);
    verify(users).findByUsername("yamada");
  }

  @Test
  void 表示名1文字は登録できる() {
    stubInsert("newuser", "a@example.com", "あ");
    assertThat(auth.signup(signup("newuser", "あ", "a@example.com", "password123", "password123"))
            .getUser()
            .getDisplayName())
        .isEqualTo("あ");
  }

  @Test
  void 表示名20文字は登録できる() {
    String name = "あ".repeat(20);
    stubInsert("newuser", "a@example.com", name);
    assertThat(auth.signup(signup("newuser", name, "a@example.com", "password123", "password123"))
            .getUser()
            .getDisplayName())
        .hasSize(20);
  }

  @Test
  void 表示名21文字は登録できない() {
    assertApi(
        HttpStatus.BAD_REQUEST,
        "表示名は1〜20文字です",
        () -> auth.signup(signup("newuser", "あ".repeat(21), "a@example.com", "password123", "password123")));
  }

  @Test
  void パスワード8文字は登録できる() {
    stubInsert("newuser", "a@example.com", "表示");
    assertThat(auth.signup(signup("newuser", "表示", "a@example.com", "12345678", "12345678")).getAccessToken())
        .isNotBlank();
  }

  @Test
  void confirmが空ならpasswordConfirmを使う() {
    stubInsert("newuser", "a@example.com", "表示");
    SignupRequest request = signup("newuser", "表示", "a@example.com", "password123", "");
    request.setPasswordConfirm("password123");
    assertThat(auth.signup(request).getRefreshToken()).isNotBlank();
  }

  @Test
  void パスワード7文字は登録できない() {
    SignupRequest request = signup("newuser", "表示", "a@example.com", "1234567", "1234567");
    assertApi(HttpStatus.BAD_REQUEST, "パスワードは8文字以上です", () -> auth.signup(request));
  }

  @Test
  void パスワード不一致は登録できない() {
    SignupRequest request = signup("newuser", "表示", "a@example.com", "password123", "other123");
    assertApi(HttpStatus.BAD_REQUEST, "パスワードが一致しません", () -> auth.signup(request));
  }

  @Test
  void メール形式不正は登録できない() {
    SignupRequest request = signup("newuser", "表示", "not-mail", "password123", "password123");
    assertApi(HttpStatus.BAD_REQUEST, "メール形式で入力してください", () -> auth.signup(request));
  }

  @Test
  void 空白のみの表示名は登録できない() {
    SignupRequest request = signup("newuser", "  ", "a@example.com", "password123", "password123");
    assertApi(HttpStatus.BAD_REQUEST, "表示名は1〜20文字です", () -> auth.signup(request));
  }

  @Test
  void 重複ユーザー名は登録できない() {
    when(users.findByUsername("newuser")).thenReturn(new User());
    SignupRequest request = signup("newuser", "表示", "a@example.com", "password123", "password123");
    assertApi(HttpStatus.CONFLICT, "このユーザー名は使われています", () -> auth.signup(request));
  }

  @Test
  void 重複メールは登録できない() {
    when(users.findByUsername("newuser")).thenReturn(null);
    when(users.findByEmail("a@example.com")).thenReturn(new User());
    SignupRequest request = signup("newuser", "表示", "a@example.com", "password123", "password123");
    assertApi(HttpStatus.CONFLICT, "このメールは登録済みです", () -> auth.signup(request));
  }

  @Test
  void メールまたはパスワード空はログインできない() {
    LoginRequest request = new LoginRequest();
    request.setEmail(" ");
    request.setPassword("");
    assertApi(HttpStatus.BAD_REQUEST, "メールアドレスとパスワードを入力してください", () -> auth.login(request));
  }

  @Test
  void 存在しないユーザーはログインできない() {
    when(users.findByEmail("none@example.com")).thenReturn(null);
    LoginRequest request = new LoginRequest();
    request.setEmail("none@example.com");
    request.setPassword("password123");
    assertApi(HttpStatus.UNAUTHORIZED, "メールアドレスまたはパスワードが違います", () -> auth.login(request));
    verify(refreshTokens, never()).insert(any());
  }

  @Test
  void パスワード違いはログインできない() {
    User user = new User();
    user.setId(1L);
    user.setEmail("a@example.com");
    user.setUsername("yamada");
    user.setDisplayName("山田");
    user.setPasswordDigest(new BCryptPasswordEncoder().encode("password123"));
    when(users.findByEmail("a@example.com")).thenReturn(user);

    LoginRequest request = new LoginRequest();
    request.setEmail("a@example.com");
    request.setPassword("nope-nope");
    assertApi(HttpStatus.UNAUTHORIZED, "メールアドレスまたはパスワードが違います", () -> auth.login(request));
  }

  @Test
  void ログイン失敗のログにパスワードを出さない() {
    Logger logger = (Logger) LoggerFactory.getLogger(AuthService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      when(users.findByEmail("none@example.com")).thenReturn(null);
      LoginRequest request = new LoginRequest();
      request.setEmail("none@example.com");
      request.setPassword("secret-pass-xyz");
      assertApi(HttpStatus.UNAUTHORIZED, "メールアドレスまたはパスワードが違います", () -> auth.login(request));
      String joined =
          appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (a, b) -> a + "\n" + b);
      assertThat(joined).contains("ログイン失敗");
      assertThat(joined).doesNotContain("secret-pass-xyz");
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void 空のリフレッシュは拒否する() {
    assertApi(HttpStatus.BAD_REQUEST, "リフレッシュトークンを指定してください", () -> auth.refresh("  "));
  }

  @Test
  void 未知のリフレッシュは拒否する() {
    when(refreshTokens.findValidByHash(anyString(), anyLong())).thenReturn(null);
    assertApi(HttpStatus.UNAUTHORIZED, "リフレッシュトークンが無効です。再度ログインしてください", () -> auth.refresh("token"));
  }

  @Test
  void ユーザー欠落のリフレッシュは拒否する() {
    RefreshToken stored = new RefreshToken();
    stored.setId(3L);
    stored.setUserId(99L);
    when(refreshTokens.findValidByHash(anyString(), anyLong())).thenReturn(stored);
    when(users.findById(99L)).thenReturn(null);

    assertApi(HttpStatus.UNAUTHORIZED, "ユーザーが見つかりません", () -> auth.refresh("token"));
    verify(refreshTokens).deleteById(3L);
  }

  @Test
  void 存在しないユーザーのmeは拒否する() {
    when(users.findById(8L)).thenReturn(null);
    assertApi(HttpStatus.UNAUTHORIZED, "ユーザーが見つかりません", () -> auth.me(8L));
  }

  @Test
  void 空ログアウトは何もしない() {
    auth.logout(" ");
    verify(refreshTokens, never()).deleteByHash(anyString());
  }

  private void stubInsert(String username, String email, String displayName) {
    when(users.findByUsername(anyString())).thenReturn(null);
    when(users.findByEmail(anyString())).thenReturn(null);
    when(users.insert(any(User.class)))
        .thenAnswer(
            inv -> {
              User created = inv.getArgument(0);
              created.setId(1L);
              return 1;
            });
    when(users.findById(1L))
        .thenAnswer(
            inv -> {
              User stored = new User();
              stored.setId(1L);
              stored.setUsername(username);
              stored.setEmail(email);
              stored.setDisplayName(displayName);
              return stored;
            });
  }

  private static SignupRequest signup(
      String username, String displayName, String email, String password, String confirm) {
    SignupRequest request = new SignupRequest();
    request.setUsername(username);
    request.setDisplayName(displayName);
    request.setEmail(email);
    request.setPassword(password);
    request.setConfirm(confirm);
    return request;
  }

  private static void assertApi(HttpStatus status, String message, Runnable action) {
    assertThatThrownBy(action::run)
        .isInstanceOf(ApiException.class)
        .satisfies(
            ex -> {
              ApiException api = (ApiException) ex;
              assertThat(api.getStatus()).isEqualTo(status);
              assertThat(api.getMessage()).isEqualTo(message);
            });
  }
}
