package com.raisetech.tripdiary.service;

import com.raisetech.tripdiary.domain.Comment;
import com.raisetech.tripdiary.domain.Post;
import com.raisetech.tripdiary.domain.User;
import com.raisetech.tripdiary.dto.CommentListResponse;
import com.raisetech.tripdiary.dto.CommentResponse;
import com.raisetech.tripdiary.mapper.CommentMapper;
import com.raisetech.tripdiary.mapper.PostMapper;
import com.raisetech.tripdiary.mapper.UserMapper;
import com.raisetech.tripdiary.web.ApiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

  private static final Logger log = LoggerFactory.getLogger(CommentService.class);
  private static final int BODY_MAX = 140;

  private final CommentMapper comments;
  private final PostMapper posts;
  private final UserMapper users;

  public CommentService(CommentMapper comments, PostMapper posts, UserMapper users) {
    this.comments = comments;
    this.posts = posts;
    this.users = users;
  }

  public CommentListResponse list(long viewerId, long postId) {
    requirePost(postId);
    List<CommentResponse> body = comments.listByPostId(postId).stream()
        .map(comment -> CommentResponse.from(comment, viewerId))
        .toList();
    return new CommentListResponse(body);
  }

  @Transactional
  public CommentResponse create(long userId, long postId, String body) {
    requirePost(postId);
    Comment comment = new Comment();
    comment.setPostId(postId);
    comment.setUserId(userId);
    comment.setBody(requireBody(body));
    comments.insert(comment);
    log.info("コメントを作成 userId={} postId={} commentId={}", userId, postId, comment.getId());
    return CommentResponse.from(require(comment.getId()), userId);
  }

  @Transactional
  public void delete(long userId, long id) {
    Comment existing = require(id);
    if (existing.getUserId() == null || existing.getUserId() != userId) {
      log.warn("コメントの権限なし userId={} commentId={}", userId, id);
      throw new ApiException(HttpStatus.FORBIDDEN, "自分のコメントだけ削除できます");
    }
    comments.deleteById(id);
    log.info("コメントを削除 userId={} commentId={}", userId, id);
  }

  public void seedIfEmpty() {
    if (comments.count() > 0 || posts.count() == 0) {
      return;
    }
    List<Post> rows = posts.list(1, "all", null, 20, null, null, null, null);
    User hanako = users.findByUsername("hanako");
    User ichiro = users.findByUsername("ichiro");
    if (rows.isEmpty() || hanako == null || ichiro == null) {
      return;
    }
    insertSeed(rows.get(0).getId(), hanako.getId(), "行き方を教えてください");
    if (rows.size() > 1) {
      insertSeed(rows.get(1).getId(), ichiro.getId(), "周辺のごはん、どこがおすすめ？");
    }
    insertSeed(rows.get(0).getId(), ichiro.getId(), "夜に行った方がいいですか？");
  }

  private void insertSeed(long postId, long userId, String body) {
    Comment comment = new Comment();
    comment.setPostId(postId);
    comment.setUserId(userId);
    comment.setBody(body);
    comments.insert(comment);
  }

  private Post requirePost(long postId) {
    Post post = posts.findById(postId);
    if (post == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "投稿が見つかりません");
    }
    return post;
  }

  private Comment require(long id) {
    Comment comment = comments.findById(id);
    if (comment == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "コメントが見つかりません");
    }
    return comment;
  }

  private static String requireBody(String body) {
    String text = body == null ? "" : body.trim();
    if (text.isEmpty() || text.length() > BODY_MAX) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "コメントは1〜140文字です");
    }
    return text;
  }
}
