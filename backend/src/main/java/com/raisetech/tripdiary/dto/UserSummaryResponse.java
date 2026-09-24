package com.raisetech.tripdiary.dto;

import com.raisetech.tripdiary.domain.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "検索・フォロー一覧の1行。自分の行は mine=true でフォローボタンを出さない。")
public class UserSummaryResponse {

  private long id;
  private String username;
  private String displayName;
  private boolean followedByMe;
  private boolean mine;

  public static UserSummaryResponse from(UserSummary user, long viewerId) {
    UserSummaryResponse response = new UserSummaryResponse();
    response.id = user.getId();
    response.username = user.getUsername();
    response.displayName = user.getDisplayName();
    response.followedByMe = user.isFollowedByMe();
    response.mine = user.getId() != null && user.getId() == viewerId;
    return response;
  }

  public long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public boolean isFollowedByMe() {
    return followedByMe;
  }

  public boolean isMine() {
    return mine;
  }
}
