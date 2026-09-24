package com.raisetech.tripdiary.domain;

public class Follow {

  private Long id;
  private Long followerId;
  private Long followeeId;
  private String createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getFollowerId() {
    return followerId;
  }

  public void setFollowerId(Long followerId) {
    this.followerId = followerId;
  }

  public Long getFolloweeId() {
    return followeeId;
  }

  public void setFolloweeId(Long followeeId) {
    this.followeeId = followeeId;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }
}
