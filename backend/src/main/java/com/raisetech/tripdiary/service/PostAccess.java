package com.raisetech.tripdiary.service;

import com.raisetech.tripdiary.domain.Post;

public final class PostAccess {

  private PostAccess() {}

  public static boolean isPublic(Post post) {
    String visibility = post.getVisibility();
    return visibility == null || visibility.isBlank() || "public".equals(visibility);
  }

  public static boolean canSee(Post post, long viewerId) {
    return isPublic(post) || (post.getUserId() != null && post.getUserId() == viewerId);
  }
}
