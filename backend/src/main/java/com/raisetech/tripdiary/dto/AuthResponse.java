package com.raisetech.tripdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登録・ログイン・リフレッシュの成功応答。パスワードは出さない。")
public class AuthResponse {

  @Schema(description = "アクセス JWT。有効 15分。Authorization: Bearer に付ける")
  private String accessToken;

  @Schema(description = "リフレッシュ JWT。有効 7日。DB にハッシュ保存")
  private String refreshToken;

  @Schema(description = "今のユーザー")
  private UserResponse user;

  public AuthResponse(String accessToken, String refreshToken, UserResponse user) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.user = user;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  @Schema(description = "以前の { token } 形式との互換。中身は accessToken")
  public String getToken() {
    return accessToken;
  }

  public UserResponse getUser() {
    return user;
  }
}
