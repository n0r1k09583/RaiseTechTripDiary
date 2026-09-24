package com.raisetech.tripdiary.service;

import com.raisetech.tripdiary.domain.RefreshToken;
import com.raisetech.tripdiary.domain.User;
import com.raisetech.tripdiary.dto.AuthResponse;
import com.raisetech.tripdiary.dto.LoginRequest;
import com.raisetech.tripdiary.dto.SignupRequest;
import com.raisetech.tripdiary.dto.UserResponse;
import com.raisetech.tripdiary.mapper.RefreshTokenMapper;
import com.raisetech.tripdiary.mapper.UserMapper;
import com.raisetech.tripdiary.web.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{3,20}$");
  private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserMapper users;
  private final RefreshTokenMapper refreshTokens;
  private final JwtService jwt;
  private final PasswordEncoder passwords = new BCryptPasswordEncoder();
  private final long refreshExpirationMs;

  public AuthService(
      UserMapper users,
      RefreshTokenMapper refreshTokens,
      JwtService jwt,
      @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.jwt = jwt;
    this.refreshExpirationMs = refreshExpirationMs;
  }

  @Transactional
  public AuthResponse signup(SignupRequest request) {
    String username = trim(request.getUsername()).toLowerCase();
    String displayName = trim(request.getDisplayName());
    String email = trim(request.getEmail());
    String password = request.getPassword() == null ? "" : request.getPassword();
    String confirm = firstNonBlank(request.getConfirm(), request.getPasswordConfirm(), password);

    if (!USERNAME.matcher(username).matches()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "ユーザー名は3〜20文字の半角英小文字・数字・_です");
    }
    if (displayName.isEmpty() || displayName.length() > 20) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "表示名は1〜20文字です");
    }
    if (!EMAIL.matcher(email).matches()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "メール形式で入力してください");
    }
    if (password.length() < 8) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "パスワードは8文字以上です");
    }
    if (!password.equals(confirm)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "パスワードが一致しません");
    }
    if (users.findByUsername(username) != null) {
      throw new ApiException(HttpStatus.CONFLICT, "このユーザー名は使われています");
    }
    if (users.findByEmail(email) != null) {
      throw new ApiException(HttpStatus.CONFLICT, "このメールは登録済みです");
    }

    User user = new User();
    user.setEmail(email);
    user.setUsername(username);
    user.setDisplayName(displayName);
    user.setPasswordDigest(passwords.encode(password));
    users.insert(user);
    log.info("新規登録 userId={} username={}", user.getId(), username);
    return issueTokens(users.findById(user.getId()));
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    String email = trim(request.getEmail());
    String password = request.getPassword() == null ? "" : request.getPassword();
    if (email.isEmpty() || password.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "メールアドレスとパスワードを入力してください");
    }
    User user = users.findByEmail(email);
    if (user == null || !passwords.matches(password, user.getPasswordDigest())) {
      log.warn("ログイン失敗 email={}", email);
      throw new ApiException(HttpStatus.UNAUTHORIZED, "メールアドレスまたはパスワードが違います");
    }
    log.info("ログイン成功 userId={} username={}", user.getId(), user.getUsername());
    return issueTokens(user);
  }

  @Transactional
  public AuthResponse refresh(String refreshToken) {
    String raw = trim(refreshToken);
    if (raw.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "リフレッシュトークンを指定してください");
    }
    long now = System.currentTimeMillis();
    RefreshToken stored = refreshTokens.findValidByHash(sha256Hex(raw), now);
    if (stored == null) {
      log.warn("リフレッシュトークンが無効");
      throw new ApiException(HttpStatus.UNAUTHORIZED, "リフレッシュトークンが無効です。再度ログインしてください");
    }
    refreshTokens.deleteById(stored.getId());
    User user = users.findById(stored.getUserId());
    if (user == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "ユーザーが見つかりません");
    }
    log.info("トークン更新 userId={}", user.getId());
    return issueTokens(user);
  }

  public void logout(String refreshToken) {
    String raw = trim(refreshToken);
    if (raw.isEmpty()) {
      return;
    }
    refreshTokens.deleteByHash(sha256Hex(raw));
    log.info("ログアウト");
  }

  public UserResponse me(long userId) {
    User user = users.findById(userId);
    if (user == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "ユーザーが見つかりません");
    }
    return UserResponse.from(user);
  }

  public void seedIfEmpty() {
    ensureDemo("yamada@example.com", "yamada", "山田");
    ensureDemo("hanako@example.com", "hanako", "佐藤 花子");
    ensureDemo("ichiro@example.com", "ichiro", "鈴木 一郎");
  }

  private AuthResponse issueTokens(User user) {
    long now = System.currentTimeMillis();
    String refreshRaw = newRefreshToken();
    RefreshToken row = new RefreshToken();
    row.setUserId(user.getId());
    row.setTokenHash(sha256Hex(refreshRaw));
    row.setExpiresAt(now + refreshExpirationMs);
    row.setCreatedAt(now);
    refreshTokens.insert(row);
    return new AuthResponse(jwt.issueAccess(user), refreshRaw, UserResponse.from(user));
  }

  private void ensureDemo(String email, String username, String displayName) {
    if (users.findByUsername(username) != null || users.findByEmail(email) != null) {
      return;
    }
    insertDemo(email, username, displayName);
  }

  private void insertDemo(String email, String username, String displayName) {
    User user = new User();
    user.setEmail(email);
    user.setUsername(username);
    user.setDisplayName(displayName);
    user.setPasswordDigest(passwords.encode("password123"));
    users.insert(user);
  }

  private static String newRefreshToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private static String sha256Hex(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isEmpty()) {
        return value;
      }
    }
    return "";
  }
}
