package com.raisetech.tripdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "ログイン。メールとパスワード。")
public class LoginRequest {

  @Schema(description = "登録済みメール", example = "yamada@example.com")
  private String email;

  @Schema(description = "パスワード（8文字以上）", example = "password123", format = "password")
  private String password;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
