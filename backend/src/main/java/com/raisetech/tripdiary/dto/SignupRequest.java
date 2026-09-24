package com.raisetech.tripdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "新規登録。成功するとログインと同じトークンを返す。")
public class SignupRequest {

  @Schema(description = "3〜20文字の半角英小文字・数字・_", example = "yamada")
  private String username;

  @Schema(description = "表示名 1〜20文字", example = "山田")
  private String displayName;

  @Schema(description = "メール", example = "yamada@example.com")
  private String email;

  @Schema(description = "8文字以上", example = "password123", format = "password")
  private String password;

  @Schema(description = "パスワード確認。password と同じ値")
  private String confirm;

  @Schema(description = "confirm の別名。どちらかあればよい", hidden = true)
  private String passwordConfirm;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

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

  public String getConfirm() {
    return confirm;
  }

  public void setConfirm(String confirm) {
    this.confirm = confirm;
  }

  public String getPasswordConfirm() {
    return passwordConfirm;
  }

  public void setPasswordConfirm(String passwordConfirm) {
    this.passwordConfirm = passwordConfirm;
  }
}
