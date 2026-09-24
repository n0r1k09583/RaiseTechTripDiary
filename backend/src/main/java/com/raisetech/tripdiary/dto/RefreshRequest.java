package com.raisetech.tripdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "リフレッシュトークンの回し。ログアウトでも同じ形を受け取る。")
public class RefreshRequest {

  @Schema(description = "ログイン時に受け取った refreshToken")
  private String refreshToken;

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
