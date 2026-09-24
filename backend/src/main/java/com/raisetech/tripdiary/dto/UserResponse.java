package com.raisetech.tripdiary.dto;

import com.raisetech.tripdiary.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "公開してよいユーザー情報。password_digest は出さない。")
public class UserResponse {

  private long id;
  private String email;
  private String username;
  private String displayName;

  public static UserResponse from(User user) {
    UserResponse response = new UserResponse();
    response.id = user.getId();
    response.email = user.getEmail();
    response.username = user.getUsername();
    response.displayName = user.getDisplayName();
    return response;
  }

  public long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getUsername() {
    return username;
  }

  public String getDisplayName() {
    return displayName;
  }
}
