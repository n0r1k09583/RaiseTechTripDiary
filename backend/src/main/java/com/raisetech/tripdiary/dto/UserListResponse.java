package com.raisetech.tripdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "ユーザー検索またはフォロー一覧")
public class UserListResponse {

  @Schema(description = "ユーザー。検索はユーザー名昇順")
  private final List<UserSummaryResponse> users;

  public UserListResponse(List<UserSummaryResponse> users) {
    this.users = users;
  }

  public List<UserSummaryResponse> getUsers() {
    return users;
  }
}
