package com.raisetech.tripdiary.domain;

public class Profile {

  private Long id;
  private String username;
  private String displayName;
  private int followingCount;
  private int followerCount;
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

  public int getFollowingCount() {
    return followingCount;
  }

  public void setFollowingCount(int followingCount) {
    this.followingCount = followingCount;
  }

  public int getFollowerCount() {
    return followerCount;
  }

  public void setFollowerCount(int followerCount) {
    this.followerCount = followerCount;
  }

  public boolean isFollowedByMe() {
    return followedByMe;
  }

  public void setFollowedByMe(boolean followedByMe) {
    this.followedByMe = followedByMe;
  }
}
