package com.raisetech.tripdiary.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raisetech.tripdiary.dto.PostListResponse;
import com.raisetech.tripdiary.service.FavoriteService;
import com.raisetech.tripdiary.service.LikeService;
import com.raisetech.tripdiary.service.PostService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerSliceTest(controllers = PostController.class)
class PostControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  PostService posts;

  @MockitoBean
  LikeService likes;

  @MockitoBean
  FavoriteService favorites;

  @Test
  void 投稿0件の一覧は空() throws Exception {
    when(posts.list(eq(1L), eq("all"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(new PostListResponse(List.of(), false));

    mockMvc
        .perform(get("/api/posts").requestAttr(AuthInterceptor.USER_ID_ATTR, 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posts").isEmpty())
        .andExpect(jsonPath("$.hasMore").value(false));
  }

  @Test
  void 無い投稿は404() throws Exception {
    when(posts.get(1L, 99L)).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "投稿が見つかりません"));

    mockMvc
        .perform(get("/api/posts/99").requestAttr(AuthInterceptor.USER_ID_ATTR, 1L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.error").value("投稿が見つかりません"));
  }

  @Test
  void 空本文の作成は400() throws Exception {
    when(posts.create(eq(1L), eq("東京駅"), eq("関東"), eq("visited"), eq("   "), any(), eq("public"), isNull(), isNull()))
        .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "本文は1〜280文字です"));

    mockMvc
        .perform(
            multipart("/api/posts")
                .param("spotName", "東京駅")
                .param("areaTag", "関東")
                .param("body", "   ")
                .requestAttr(AuthInterceptor.USER_ID_ATTR, 1L))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("本文は1〜280文字です"));
  }

  @Test
  void 非対応画像の作成は400() throws Exception {
    when(posts.create(eq(1L), eq("東京駅"), eq("関東"), eq("visited"), eq("本文"), any(), eq("public"), isNull(), isNull()))
        .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "JPEG / PNG / WebP のみです"));

    MockMultipartFile gif = new MockMultipartFile("image", "x.gif", "image/gif", new byte[] {1, 2, 3});
    mockMvc
        .perform(
            multipart("/api/posts")
                .file(gif)
                .param("spotName", "東京駅")
                .param("areaTag", "関東")
                .param("body", "本文")
                .requestAttr(AuthInterceptor.USER_ID_ATTR, 1L))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("JPEG / PNG / WebP のみです"));
  }

  @Test
  void 他人の投稿編集は403() throws Exception {
    when(posts.update(eq(2L), eq(5L), isNull(), isNull(), isNull(), eq("盗む"), any(), isNull(), isNull(), isNull(), eq(true)))
        .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "自分の投稿だけ編集できます"));

    mockMvc
        .perform(
            multipart(HttpMethod.PATCH, "/api/posts/5")
                .param("body", "盗む")
                .requestAttr(AuthInterceptor.USER_ID_ATTR, 2L))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void 他人の投稿削除は403() throws Exception {
    org.mockito.Mockito.doThrow(new ApiException(HttpStatus.FORBIDDEN, "自分の投稿だけ削除できます"))
        .when(posts)
        .delete(2L, 5L);

    mockMvc
        .perform(delete("/api/posts/5").requestAttr(AuthInterceptor.USER_ID_ATTR, 2L))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("自分の投稿だけ削除できます"));
  }

  @Test
  void 無い投稿の削除は404() throws Exception {
    org.mockito.Mockito.doThrow(new ApiException(HttpStatus.NOT_FOUND, "投稿が見つかりません"))
        .when(posts)
        .delete(1L, 404L);

    mockMvc
        .perform(delete("/api/posts/404").requestAttr(AuthInterceptor.USER_ID_ATTR, 1L))
        .andExpect(status().isNotFound());
  }
}
