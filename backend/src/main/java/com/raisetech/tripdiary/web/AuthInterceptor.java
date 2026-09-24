package com.raisetech.tripdiary.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raisetech.tripdiary.dto.ErrorResponse;
import com.raisetech.tripdiary.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

  public static final String USER_ID_ATTR = "userId";

  private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

  private final JwtService jwt;
  private final ObjectMapper objectMapper;

  public AuthInterceptor(JwtService jwt, ObjectMapper objectMapper) {
    this.jwt = jwt;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    String header = request.getHeader("Authorization");
    String token = "";
    if (header != null && header.startsWith("Bearer ")) {
      token = header.substring(7);
    }
    if (token.isEmpty()) {
      log.warn("未ログイン path={}", request.getRequestURI());
      writeUnauthorized(response, "ログインしてください");
      return false;
    }
    try {
      Claims claims = jwt.parseAccess(token);
      String userId = claims.getSubject();
      request.setAttribute(USER_ID_ATTR, Long.parseLong(userId));
      MDC.put("userId", userId);
      return true;
    } catch (JwtException | IllegalArgumentException ex) {
      log.warn("トークン無効 path={}", request.getRequestURI());
      writeUnauthorized(response, "トークンが無効です。再度ログインしてください");
      return false;
    }
  }

  private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), new ErrorResponse(HttpStatus.UNAUTHORIZED, message));
  }
}
