package com.raisetech.tripdiary.web;

import com.raisetech.tripdiary.config.OpenApiConfig;
import com.raisetech.tripdiary.dto.AuthResponse;
import com.raisetech.tripdiary.dto.ErrorResponse;
import com.raisetech.tripdiary.dto.HealthResponse;
import com.raisetech.tripdiary.dto.LoginRequest;
import com.raisetech.tripdiary.dto.RefreshRequest;
import com.raisetech.tripdiary.dto.SignupRequest;
import com.raisetech.tripdiary.dto.UserResponse;
import com.raisetech.tripdiary.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "認証", description = "登録・ログイン・トークン。画面担当はここからセッションを始める。")
public class AuthController {

  private static final String ERROR_JSON = MediaType.APPLICATION_JSON_VALUE;

  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @GetMapping("/health")
  @Operation(summary = "起動確認", description = "8080 が生きているか。ログイン不要。")
  public HealthResponse health() {
    return new HealthResponse();
  }

  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "新規登録", description = "成功するとログインと同じトークンを返す。パスワードはハッシュ保存し、応答に出さない。")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "登録して JWT を返す"),
    @ApiResponse(
        responseCode = "400",
        description = "ユーザー名・表示名・メール・パスワードの形式不正、または確認が一致しない",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "ユーザー名またはメールが既にある",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public AuthResponse signup(@RequestBody SignupRequest request) {
    return auth.signup(request);
  }

  @PostMapping("/login")
  @Operation(summary = "ログイン", description = "メールとパスワード。成功したら accessToken を Bearer に付ける。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "JWT とユーザー"),
    @ApiResponse(
        responseCode = "400",
        description = "メールまたはパスワードが空",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "メールまたはパスワードが違う。画面はパスワード欄を空にする",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public AuthResponse login(@RequestBody LoginRequest request) {
    return auth.login(request);
  }

  @PostMapping("/refresh")
  @Operation(summary = "アクセストークンを更新", description = "refreshToken を1回使い、新しい組を返す。古い refresh は無効。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "新しい JWT"),
    @ApiResponse(
        responseCode = "400",
        description = "refreshToken が無い",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "refreshToken が無効または期限切れ。再ログインが必要",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public AuthResponse refresh(@RequestBody RefreshRequest request) {
    return auth.refresh(request.getRefreshToken());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "ログアウト", description = "refreshToken があれば破棄する。204。画面はトークンを捨ててログインへ。")
  @ApiResponse(responseCode = "204", description = "破棄した。本文なし")
  public void logout(@RequestBody(required = false) RefreshRequest request) {
    auth.logout(request == null ? null : request.getRefreshToken());
  }

  @GetMapping("/me")
  @SecurityRequirement(name = OpenApiConfig.BEARER)
  @Operation(summary = "今のユーザー", description = "Bearer 必須。応答は `{ user }`。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "ログイン中のユーザー"),
    @ApiResponse(
        responseCode = "401",
        description = "トークンが無い、または無効。`ログインしてください` / `トークンが無効です。再度ログインしてください`",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public Map<String, UserResponse> me(HttpServletRequest request) {
    long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
    return Map.of("user", auth.me(userId));
  }
}
