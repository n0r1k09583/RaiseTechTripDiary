package com.raisetech.tripdiary.dto;

import com.raisetech.tripdiary.domain.Profile;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "プロフィール。email とパスワードは出さない。")
public class ProfileResponse {

  private long id;
  private String username;
  private String displayName;
  private int followingCount;
  private int followerCount;
  private boolean followedByMe;
  private boolean mine;

  public static ProfileResponse from(Profile profile, long viewerId) {
    ProfileResponse response = new ProfileResponse();
    response.id = profile.getId();
    response.username = profile.getUsername();
    response.displayName = profile.getDisplayName();
    response.followingCount = profile.getFollowingCount();
    response.followerCount = profile.getFollowerCount();
    response.followedByMe = profile.isFollowedByMe();
    response.mine = profile.getId() != null && profile.getId() == viewerId;
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

  public int getFollowingCount() {
    return followingCount;
  }

  public int getFollowerCount() {
    return followerCount;
  }

  public boolean isFollowedByMe() {
    return followedByMe;
  }

  public boolean isMine() {
    return mine;
  }
}
