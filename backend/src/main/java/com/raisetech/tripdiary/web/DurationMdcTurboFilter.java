package com.raisetech.tripdiary.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.MDC;
import org.slf4j.Marker;

/**
 * リクエスト開始時刻から経過ミリ秒を {@code duration_ms} に載せる。 ERROR 行にも所要時間が付く（授業の JSON 例と同じ）。
 */
public class DurationMdcTurboFilter extends TurboFilter {

  static final String STARTED_AT_MS = "startedAtMs";

  @Override
  public FilterReply decide(
      Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
    String started = MDC.get(STARTED_AT_MS);
    if (started != null && !started.isBlank()) {
      MDC.put("duration_ms", Long.toString(System.currentTimeMillis() - Long.parseLong(started)));
    }
    return FilterReply.NEUTRAL;
  }
}
