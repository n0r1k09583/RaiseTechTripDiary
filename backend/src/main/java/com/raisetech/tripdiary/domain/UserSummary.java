package com.raisetech.tripdiary.domain;

public class UserSummary {

  private Long id;
  private String username;
  private String displayName;
  private boolean followedByMe;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public boolean isFollowedByMe() {
    return followedByMe;
  }

  public void setFollowedByMe(boolean followedByMe) {
    this.followedByMe = followedByMe;
  }
}
