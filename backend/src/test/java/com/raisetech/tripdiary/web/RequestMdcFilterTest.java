package com.raisetech.tripdiary.web;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestMdcFilterTest {

  RequestMdcFilter filter = new RequestMdcFilter();

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void APIはtraceIdと所要時間を載せてからMDCを消す() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/login");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain =
        (req, res) -> {
          assertThat(MDC.get("traceId")).isNotBlank();
          assertThat(MDC.get("requestId")).isEqualTo(MDC.get("traceId"));
          assertThat(MDC.get("service")).isEqualTo("timeline");
          assertThat(MDC.get("method")).isEqualTo("POST");
          assertThat(MDC.get("endpoint")).isEqualTo("/api/login");
          assertThat(MDC.get(DurationMdcTurboFilter.STARTED_AT_MS)).isNotBlank();
        };

    filter.doFilter(request, response, chain);

    assertThat(MDC.getCopyOfContextMap()).isNull();
  }

  @Test
  void API完了ログに授業のJSONフィールドが載る() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(RequestMdcFilter.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/login");
      MockHttpServletResponse response = new MockHttpServletResponse();
      FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(401);

      filter.doFilter(request, response, chain);

      assertThat(appender.list).isNotEmpty();
      ILoggingEvent event = appender.list.get(appender.list.size() - 1);
      assertThat(event.getFormattedMessage()).isEqualTo("リクエスト完了");
      assertThat(event.getMDCPropertyMap())
          .containsKeys("traceId", "requestId", "service", "method", "endpoint", "httpStatus", "duration_ms");
      assertThat(event.getMDCPropertyMap().get("service")).isEqualTo("timeline");
      assertThat(event.getMDCPropertyMap().get("method")).isEqualTo("POST");
      assertThat(event.getMDCPropertyMap().get("endpoint")).isEqualTo("/api/login");
      assertThat(event.getMDCPropertyMap().get("httpStatus")).isEqualTo("401");
      assertThat(event.getMDCPropertyMap().get("traceId")).isEqualTo(event.getMDCPropertyMap().get("requestId"));
      assertThat(Long.parseLong(event.getMDCPropertyMap().get("duration_ms"))).isGreaterThanOrEqualTo(0);
      assertThat(event.getMDCPropertyMap()).doesNotContainKey("password");
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void API以外は完了ログ用のMDCだけ載せて消す() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain =
        (req, res) -> assertThat(MDC.get("endpoint")).isEqualTo("/v3/api-docs");

    filter.doFilter(request, response, chain);

    assertThat(MDC.getCopyOfContextMap()).isNull();
  }
}
