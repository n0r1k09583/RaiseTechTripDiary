package com.raisetech.tripdiary.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestMdcFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestMdcFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    long started = System.currentTimeMillis();
    MDC.put("traceId", traceId);
    MDC.put("requestId", traceId);
    MDC.put("service", "timeline");
    MDC.put("method", request.getMethod());
    MDC.put("endpoint", request.getRequestURI());
    MDC.put(DurationMdcTurboFilter.STARTED_AT_MS, Long.toString(started));
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.put("httpStatus", Integer.toString(response.getStatus()));
      MDC.put("duration_ms", Long.toString(System.currentTimeMillis() - started));
      if (request.getRequestURI().startsWith("/api")) {
        log.info("リクエスト完了");
      }
      MDC.clear();
    }
  }
}
