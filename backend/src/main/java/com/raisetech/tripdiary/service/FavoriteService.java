package com.raisetech.tripdiary.service;

import com.raisetech.tripdiary.domain.Favorite;
import com.raisetech.tripdiary.domain.Post;
import com.raisetech.tripdiary.dto.PostResponse;
import com.raisetech.tripdiary.mapper.FavoriteMapper;
import com.raisetech.tripdiary.mapper.PostMapper;
import com.raisetech.tripdiary.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {

  private static final Logger log = LoggerFactory.getLogger(FavoriteService.class);

  private final FavoriteMapper favorites;
  private final PostMapper posts;

  public FavoriteService(FavoriteMapper favorites, PostMapper posts) {
    this.favorites = favorites;
    this.posts = posts;
  }

  @Transactional
  public PostResponse toggle(long userId, long postId) {
    Post post = posts.findById(postId);
    if (post == null || !PostAccess.canSee(post, userId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "投稿が見つかりません");
    }
    Favorite existing = favorites.find(postId, userId);
    if (existing == null) {
      Favorite row = new Favorite();
      row.setPostId(postId);
      row.setUserId(userId);
      favorites.insert(row);
      log.info("お気に入り userId={} postId={}", userId, postId);
    } else {
      favorites.delete(postId, userId);
      log.info("お気に入り解除 userId={} postId={}", userId, postId);
    }
    return PostResponse.from(posts.findForViewer(postId, userId), userId);
  }
}
