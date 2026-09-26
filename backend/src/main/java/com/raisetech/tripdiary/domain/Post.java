package com.raisetech.tripdiary.domain;

public class Post {

  private Long id;
  private Long userId;
  private String spotName;
  private String areaTag;
  private String visitStatus;
  private String body;
  private String imagePath;
  private Double latitude;
  private Double longitude;
  private String visibility;
  private String createdAt;
  private String updatedAt;
  private String username;
  private String displayName;
  private int commentCount;
  private int likeCount;
  private boolean likedByMe;
  private boolean favoritedByMe;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getSpotName() {
    return spotName;
  }

  public void setSpotName(String spotName) {
    this.spotName = spotName;
  }

  public String getAreaTag() {
    return areaTag;
  }

  public void setAreaTag(String areaTag) {
    this.areaTag = areaTag;
  }

  public String getVisitStatus() {
    return visitStatus;
  }

  public void setVisitStatus(String visitStatus) {
    this.visitStatus = visitStatus;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getImagePath() {
    return imagePath;
  }

  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
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

  public int getCommentCount() {
    return commentCount;
  }

  public void setCommentCount(int commentCount) {
    this.commentCount = commentCount;
  }

  public int getLikeCount() {
    return likeCount;
  }

  public void setLikeCount(int likeCount) {
    this.likeCount = likeCount;
  }

  public boolean isLikedByMe() {
    return likedByMe;
  }

  public void setLikedByMe(boolean likedByMe) {
    this.likedByMe = likedByMe;
  }

  public boolean isFavoritedByMe() {
    return favoritedByMe;
  }

  public void setFavoritedByMe(boolean favoritedByMe) {
    this.favoritedByMe = favoritedByMe;
  }
}
