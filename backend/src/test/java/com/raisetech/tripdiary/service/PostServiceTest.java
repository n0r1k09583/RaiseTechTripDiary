package com.raisetech.tripdiary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

  @Mock
  PostMapper posts;

  @Mock
  UserMapper users;

  @Mock
  CommentMapper comments;

  @Mock
  ImageStore images;

  PostService service;

  @BeforeEach
  void setUp() {
    service = new PostService(posts, users, comments, images);
  }

  @Test
  void 無い投稿は取得できない() {
    when(posts.findForViewer(9L, 1L)).thenReturn(null);
    assertApi(HttpStatus.NOT_FOUND, "投稿が見つかりません", () -> service.get(1L, 9L));
  }

  @Test
  void 空白本文は投稿できない() {
    assertApi(HttpStatus.BAD_REQUEST, "本文は1〜280文字です", () -> service.create(1L, "  ", null));
    verify(posts, never()).insert(any());
  }

  @Test
  void 本文281文字は投稿できない() {
    String body = "あ".repeat(281);
    assertApi(HttpStatus.BAD_REQUEST, "本文は1〜280文字です", () -> service.create(1L, body, null));
  }

  @Test
  void 他人の投稿は編集できない() {
    Post post = owned(5L, 1L);
    when(posts.findById(5L)).thenReturn(post);
    assertApi(HttpStatus.FORBIDDEN, "自分の投稿だけ編集できます", () -> service.update(2L, 5L, "本文", null));
    verify(posts, never()).update(any());
  }

  @Test
  void 他人の投稿は削除できない() {
    when(posts.findById(5L)).thenReturn(owned(5L, 1L));
    assertApi(HttpStatus.FORBIDDEN, "自分の投稿だけ削除できます", () -> service.delete(2L, 5L));
    verify(posts, never()).deleteById(5L);
  }

  @Test
  void 無い投稿は削除できない() {
    when(posts.findById(404L)).thenReturn(null);
    assertApi(HttpStatus.NOT_FOUND, "投稿が見つかりません", () -> service.delete(1L, 404L));
  }

  @Test
  void 非対応画像は投稿できない() {
    MockMultipartFile gif = new MockMultipartFile("image", "x.gif", "image/gif", new byte[] {1});
    when(images.save(gif)).thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "JPEG / PNG / WebP のみです"));
    assertApi(HttpStatus.BAD_REQUEST, "JPEG / PNG / WebP のみです", () -> service.create(1L, "本文です", gif));
    verify(posts, never()).insert(any());
  }

  @Test
  void 余分な1件があればhasMoreになる() {
    when(posts.list(1L, "all", null, 2, null, null, null, null)).thenReturn(List.of(owned(1L, 1L), owned(2L, 1L)));
    var response = service.list(1L, "all", 1, null, null, null, null);
    assertThat(response.isHasMore()).isTrue();
    assertThat(response.getPosts()).hasSize(1);
  }

  @Test
  void 本文1文字と280文字は投稿できる() {
    when(images.save(null)).thenReturn(null);
    when(posts.insert(any(Post.class)))
        .thenAnswer(
            inv -> {
              inv.getArgument(0, Post.class).setId(8L);
              return 1;
            });
    Post stored = owned(8L, 1L);
    stored.setBody("あ");
    when(posts.findForViewer(8L, 1L)).thenReturn(stored);
    assertThat(service.create(1L, "あ", null).getBody()).isEqualTo("あ");

    stored.setBody("あ".repeat(280));
    assertThat(service.create(1L, "あ".repeat(280), null).getBody()).hasSize(280);
  }

  @Test
  void 写真があれば空本文で投稿できる() {
    MockMultipartFile jpeg = new MockMultipartFile("image", "x.jpg", "image/jpeg", new byte[] {1});
    when(images.save(jpeg)).thenReturn("x.jpg");
    when(posts.insert(any(Post.class)))
        .thenAnswer(
            inv -> {
              inv.getArgument(0, Post.class).setId(11L);
              return 1;
            });
    Post stored = owned(11L, 1L);
    stored.setBody("");
    stored.setImagePath("x.jpg");
    when(posts.findForViewer(11L, 1L)).thenReturn(stored);
    assertThat(service.create(1L, "", jpeg).getBody()).isEmpty();
    verify(posts).insert(any(Post.class));
  }

  @Test
  void 所有者IDがnullなら編集できない() {
    Post post = owned(5L, 1L);
    post.setUserId(null);
    when(posts.findById(5L)).thenReturn(post);
    assertApi(HttpStatus.FORBIDDEN, "自分の投稿だけ編集できます", () -> service.update(1L, 5L, "本文", null));
  }

  @Test
  void 空画像では画像を差し替えない() {
    Post post = owned(5L, 1L);
    when(posts.findById(5L)).thenReturn(post);
    when(posts.findForViewer(5L, 1L)).thenReturn(post);
    MockMultipartFile empty = new MockMultipartFile("image", "x.jpg", "image/jpeg", new byte[0]);
    service.update(1L, 5L, "直した", empty);
    verify(images, never()).save(empty);
    verify(posts).update(post);
  }

  @Test
  void limitの0は1に51は50に丸める() {
    when(posts.list(eq(1L), eq("all"), isNull(), eq(2), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of());
    assertThat(service.list(1L, "all", 0, null, null, null, null).getPosts()).isEmpty();
    when(posts.list(eq(1L), eq("following"), isNull(), eq(51), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of());
    assertThat(service.list(1L, "following", 51, null, null, null, null).isHasMore()).isFalse();
  }

  @Test
  void beforeが片方だけならページングしない() {
    when(posts.list(eq(1L), eq("all"), isNull(), eq(21), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of());
    service.list(1L, "all", null, "2026-09-01 10:00:00", null, null, null);
    verify(posts).list(1L, "all", null, 21, null, null, null, null);
  }

  @Test
  void 新しい差分ではhasMoreを付けない() {
    when(posts.list(eq(1L), eq("all"), isNull(), eq(20), isNull(), isNull(), eq("t"), eq(3L)))
        .thenReturn(List.of(owned(9L, 1L)));
    var response = service.list(1L, "all", 20, null, null, "t", 3L);
    assertThat(response.isHasMore()).isFalse();
    assertThat(response.getPosts()).hasSize(1);
  }

  @Test
  void 未知タブはallに落とす() {
    when(posts.list(1L, "all", null, 21, null, null, null, null)).thenReturn(List.of());
    var response = service.list(1L, "unknown", null, null, null, null, null);
    assertThat(response.getPosts()).isEmpty();
    assertThat(response.isHasMore()).isFalse();
  }

  private static Post owned(long id, long userId) {
    Post post = new Post();
    post.setId(id);
    post.setUserId(userId);
    post.setBody("本文");
    post.setUsername("yamada");
    post.setDisplayName("山田");
    return post;
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
