package com.raisetech.tripdiary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.raisetech.tripdiary.domain.Comment;
import com.raisetech.tripdiary.domain.Post;
import com.raisetech.tripdiary.mapper.CommentMapper;
import com.raisetech.tripdiary.mapper.PostMapper;
import com.raisetech.tripdiary.mapper.UserMapper;
import com.raisetech.tripdiary.web.ApiException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock
  CommentMapper comments;

  @Mock
  PostMapper posts;

  @Mock
  UserMapper users;

  CommentService service;

  @BeforeEach
  void setUp() {
    service = new CommentService(comments, posts, users);
  }

  @Test
  void 無い投稿のコメント一覧は404() {
    when(posts.findById(9L)).thenReturn(null);
    assertApi(HttpStatus.NOT_FOUND, "投稿が見つかりません", () -> service.list(1L, 9L));
  }

  @Test
  void 無い投稿にはコメントできない() {
    when(posts.findById(9L)).thenReturn(null);
    assertApi(HttpStatus.NOT_FOUND, "投稿が見つかりません", () -> service.create(1L, 9L, "本文"));
    verify(comments, never()).insert(any());
  }

  @Test
  void 空白コメントは投稿できない() {
    when(posts.findById(1L)).thenReturn(new Post());
    assertApi(HttpStatus.BAD_REQUEST, "コメントは1〜140文字です", () -> service.create(1L, 1L, "  "));
  }

  @Test
  void コメント1文字は投稿できる() {
    when(posts.findById(1L)).thenReturn(new Post());
    when(comments.insert(any(Comment.class)))
        .thenAnswer(
            inv -> {
              inv.getArgument(0, Comment.class).setId(2L);
              return 1;
            });
    Comment stored = new Comment();
    stored.setId(2L);
    stored.setPostId(1L);
    stored.setUserId(1L);
    stored.setBody("あ");
    stored.setUsername("yamada");
    stored.setDisplayName("山田");
    when(comments.findById(2L)).thenReturn(stored);
    assertThat(service.create(1L, 1L, "あ").getBody()).isEqualTo("あ");
  }

  @Test
  void コメント140文字は投稿できる() {
    when(posts.findById(1L)).thenReturn(new Post());
    when(comments.insert(any(Comment.class)))
        .thenAnswer(
            inv -> {
              inv.getArgument(0, Comment.class).setId(2L);
              return 1;
            });
    Comment stored = new Comment();
    stored.setId(2L);
    stored.setPostId(1L);
    stored.setUserId(1L);
    stored.setBody("あ".repeat(140));
    stored.setUsername("yamada");
    stored.setDisplayName("山田");
    when(comments.findById(2L)).thenReturn(stored);
    assertThat(service.create(1L, 1L, "あ".repeat(140)).getBody()).hasSize(140);
  }

  @Test
  void コメント141文字は投稿できない() {
    when(posts.findById(1L)).thenReturn(new Post());
    assertApi(HttpStatus.BAD_REQUEST, "コメントは1〜140文字です", () -> service.create(1L, 1L, "あ".repeat(141)));
  }

  @Test
  void 所有者IDがnullならコメント削除できない() {
    Comment comment = new Comment();
    comment.setId(4L);
    comment.setUserId(null);
    when(comments.findById(4L)).thenReturn(comment);
    assertApi(HttpStatus.FORBIDDEN, "自分のコメントだけ削除できます", () -> service.delete(1L, 4L));
  }

  @Test
  void 他人のコメントは削除できない() {
    Comment comment = new Comment();
    comment.setId(4L);
    comment.setUserId(1L);
    when(comments.findById(4L)).thenReturn(comment);
    assertApi(HttpStatus.FORBIDDEN, "自分のコメントだけ削除できます", () -> service.delete(2L, 4L));
    verify(comments, never()).deleteById(4L);
  }

  @Test
  void 無いコメントは削除できない() {
    when(comments.findById(404L)).thenReturn(null);
    assertApi(HttpStatus.NOT_FOUND, "コメントが見つかりません", () -> service.delete(1L, 404L));
  }

  @Test
  void コメント0件の一覧は空で返す() {
    when(posts.findById(1L)).thenReturn(new Post());
    when(comments.listByPostId(1L)).thenReturn(List.of());
    assertThat(service.list(1L, 1L).getComments()).isEmpty();
  }

  private static void assertApi(HttpStatus status, String message, Runnable action) {
    assertThatThrownBy(action::run)
        .isInstanceOf(ApiException.class)
        .satisfies(
            ex -> {
              ApiException api = (ApiException) ex;
              assertThat(api.getStatus()).isEqualTo(status);
              assertThat(api.getMessage()).isEqualTo(message);
            });
  }
}
