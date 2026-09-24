package com.raisetech.tripdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "投稿へのコメント一覧。古い順。")
public class CommentListResponse {

  @Schema(description = "コメント")
  private final List<CommentResponse> comments;

  public CommentListResponse(List<CommentResponse> comments) {
    this.comments = comments;
  }

  public List<CommentResponse> getComments() {
    return comments;
  }
}
