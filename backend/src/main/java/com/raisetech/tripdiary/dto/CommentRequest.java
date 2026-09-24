package com.raisetech.tripdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "コメント作成。1〜140文字。")
public class CommentRequest {

  @Schema(description = "本文 1〜140文字", example = "拝見しました")
  private String body;

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }
}
