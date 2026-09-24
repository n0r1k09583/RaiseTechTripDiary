package com.raisetech.tripdiary.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raisetech.tripdiary.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

  @Mock
  JwtService jwt;

  AuthInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new AuthInterceptor(jwt, new ObjectMapper());
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void トークン無しは401のJSON() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean ok = interceptor.preHandle(request, response, new Object());

    assertThat(ok).isFalse();
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHORIZED\"");
    assertThat(response.getContentAsString()).contains("ログインしてください");
  }

  @Test
  void 無効トークンは401のJSON() throws Exception {
    when(jwt.parseAccess("bad")).thenThrow(new JwtException("broken"));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
    request.addHeader("Authorization", "Bearer bad");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean ok = interceptor.preHandle(request, response, new Object());

    assertThat(ok).isFalse();
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("トークンが無効です。再度ログインしてください");
  }

  @Test
  void Bearer以外のヘッダは401() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
    request.addHeader("Authorization", "Token abc");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
    assertThat(response.getContentAsString()).contains("ログインしてください");
  }

  @Test
  void Bearerだけは401() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
    request.addHeader("Authorization", "Bearer ");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
    assertThat(response.getContentAsString()).contains("ログインしてください");
  }

  @Test
  void 有効トークンはuserIdをMDCに載せる() throws Exception {
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn("7");
    when(jwt.parseAccess("good")).thenReturn(claims);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
    request.addHeader("Authorization", "Bearer good");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    assertThat(request.getAttribute(AuthInterceptor.USER_ID_ATTR)).isEqualTo(7L);
    assertThat(MDC.get("userId")).isEqualTo("7");
  }

  @Test
  void OPTIONSはトークン無しで通す() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/posts");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
