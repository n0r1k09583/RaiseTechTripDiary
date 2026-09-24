package com.raisetech.tripdiary.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raisetech.tripdiary.dto.CommentListResponse;
import com.raisetech.tripdiary.service.CommentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerSliceTest(controllers = CommentController.class)
class CommentControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  CommentService comments;

  @Test
  void 無い投稿のコメントは404() throws Exception {
    when(comments.list(1L, 9L)).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "投稿が見つかりません"));

    mockMvc
        .perform(get("/api/posts/9/comments").requestAttr(AuthInterceptor.USER_ID_ATTR, 1L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("投稿が見つかりません"));
  }

  @Test
  void コメント0件は空配列() throws Exception {
    when(comments.list(1L, 3L)).thenReturn(new CommentListResponse(List.of()));

    mockMvc
        .perform(get("/api/posts/3/comments").requestAttr(AuthInterceptor.USER_ID_ATTR, 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.comments").isEmpty());
  }

  @Test
  void 空白コメント作成は400() throws Exception {
    when(comments.create(eq(1L), eq(3L), anyString()))
        .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "コメントは1〜140文字です"));

    mockMvc
        .perform(
            post("/api/posts/3/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"  \"}")
                .requestAttr(AuthInterceptor.USER_ID_ATTR, 1L))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("コメントは1〜140文字です"));
  }

  @Test
  void 他人のコメント削除は403() throws Exception {
    doThrow(new ApiException(HttpStatus.FORBIDDEN, "自分のコメントだけ削除できます")).when(comments).delete(2L, 8L);

    mockMvc
        .perform(delete("/api/comments/8").requestAttr(AuthInterceptor.USER_ID_ATTR, 2L))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void 無いコメント削除は404() throws Exception {
    doThrow(new ApiException(HttpStatus.NOT_FOUND, "コメントが見つかりません")).when(comments).delete(anyLong(), eq(404L));

    mockMvc
        .perform(delete("/api/comments/404").requestAttr(AuthInterceptor.USER_ID_ATTR, 1L))
        .andExpect(status().isNotFound());
  }
}
