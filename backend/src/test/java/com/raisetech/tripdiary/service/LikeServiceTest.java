package com.raisetech.tripdiary.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.raisetech.tripdiary.domain.Like;
import com.raisetech.tripdiary.domain.Post;
import com.raisetech.tripdiary.mapper.LikeMapper;
import com.raisetech.tripdiary.mapper.PostMapper;
import com.raisetech.tripdiary.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

  @Mock
  LikeMapper likes;

  @Mock
  PostMapper posts;

  LikeService service;

  @BeforeEach
  void setUp() {
    service = new LikeService(likes, posts);
  }

  @Test
  void 無い投稿はいいねできない() {
    when(posts.findById(9L)).thenReturn(null);
    assertThatThrownBy(() -> service.toggle(1L, 9L))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getStatus())
        .isEqualTo(HttpStatus.NOT_FOUND);
    verify(likes, never()).insert(any());
  }

  @Test
  void 未いいねなら付け済みなら外す() {
        Post post = new Post();
        post.setId(5L);
        post.setUserId(1L);
        when(posts.findById(5L)).thenReturn(post);
        when(likes.find(5L, 1L)).thenReturn(null);
        when(posts.findForViewer(5L, 1L)).thenReturn(post);
    service.toggle(1L, 5L);
    verify(likes).insert(any(Like.class));

    when(likes.find(5L, 1L)).thenReturn(new Like());
    service.toggle(1L, 5L);
    verify(likes).delete(5L, 1L);
  }
}
