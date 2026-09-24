package com.raisetech.tripdiary.dto;

import com.raisetech.tripdiary.domain.Comment;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "コメント1件。mine が true なら自分ので削除できる。")
public class CommentResponse {

  private long id;
  private long postId;
  private long userId;
  private String username;
  private String displayName;
  private String body;
  private String createdAt;
  private boolean mine;

  public static CommentResponse from(Comment comment, long viewerId) {
    CommentResponse response = new CommentResponse();
    response.id = comment.getId();
    response.postId = comment.getPostId();
    response.userId = comment.getUserId();
    response.username = comment.getUsername();
    response.displayName = comment.getDisplayName();
    response.body = comment.getBody();
    response.createdAt = comment.getCreatedAt();
    response.mine = comment.getUserId() != null && comment.getUserId() == viewerId;
    return response;
  }

  public long getId() {
    return id;
  }

  public long getPostId() {
    return postId;
  }

  public long getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getBody() {
    return body;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public boolean isMine() {
    return mine;
  }
}
