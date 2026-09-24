package com.raisetech.tripdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "タイムライン一覧。新しい順。続きは beforeCreatedAt + beforeId。")
public class PostListResponse {

  @Schema(description = "投稿。先頭が新しい")
  private final List<PostResponse> posts;

  @Schema(description = "さらに古い投稿があるか。true なら無限スクロールで追加取得する")
  private final boolean hasMore;

  public PostListResponse(List<PostResponse> posts, boolean hasMore) {
    this.posts = posts;
    this.hasMore = hasMore;
  }

  public List<PostResponse> getPosts() {
    return posts;
  }

  public boolean isHasMore() {
    return hasMore;
  }
}
