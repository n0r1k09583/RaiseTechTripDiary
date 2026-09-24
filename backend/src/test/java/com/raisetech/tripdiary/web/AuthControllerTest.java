package com.raisetech.tripdiary.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.raisetech.tripdiary.dto.AuthResponse;
import com.raisetech.tripdiary.dto.UserResponse;
import com.raisetech.tripdiary.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerSliceTest(controllers = AuthController.class)
class AuthControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  AuthService auth;

  @Test
  void ログイン失敗はエラー形を返す() throws Exception {
    when(auth.login(any())).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "メールアドレスまたはパスワードが違います"));

    mockMvc
        .perform(
            post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"yamada@example.com\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.error").value("メールアドレスまたはパスワードが違います"));
  }

  @Test
  void 空ログインはバリデーションになる() throws Exception {
    when(auth.login(any())).thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "メールアドレスとパスワードを入力してください"));

    mockMvc
        .perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"\",\"password\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION"));
  }

  @Test
  void 重複ユーザー名は409() throws Exception {
    when(auth.signup(any())).thenThrow(new ApiException(HttpStatus.CONFLICT, "このユーザー名は使われています"));

    mockMvc
        .perform(
            post("/api/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"yamada","displayName":"山田","email":"other@example.com","password":"password123","confirm":"password123"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void 無効リフレッシュは401() throws Exception {
    when(auth.refresh(anyString()))
        .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "リフレッシュトークンが無効です。再度ログインしてください"));

    mockMvc
        .perform(
            post("/api/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"dead\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("リフレッシュトークンが無効です。再度ログインしてください"));
  }

  @Test
  void 壊れたJSONはログインできない() throws Exception {
    mockMvc
        .perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("リクエストの形式が正しくありません"));
    verify(auth, never()).login(any());
  }

  @Test
  void GETのログインは405() throws Exception {
    mockMvc
        .perform(get("/api/login"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  void meはリクエストのユーザーIDを使う() throws Exception {
    UserResponse user = new UserResponse();
    when(auth.me(7L)).thenReturn(user);

    mockMvc
        .perform(get("/api/me").requestAttr(AuthInterceptor.USER_ID_ATTR, 7L))
        .andExpect(status().isOk());
    verify(auth).me(7L);
  }

  @Test
  void 存在しないユーザーのmeは401() throws Exception {
    when(auth.me(anyLong())).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "ユーザーが見つかりません"));

    mockMvc
        .perform(get("/api/me").requestAttr(AuthInterceptor.USER_ID_ATTR, 99L))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("ユーザーが見つかりません"));
  }

  @Test
  void ログイン成功にパスワードを出さない() throws Exception {
    UserResponse user = new UserResponse();
    when(auth.login(any())).thenReturn(new AuthResponse("access", "refresh", user));

    mockMvc
        .perform(
            post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"yamada@example.com\",\"password\":\"password123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.user.passwordDigest").doesNotExist());
  }
}
