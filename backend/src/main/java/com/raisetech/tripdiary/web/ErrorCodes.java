package com.raisetech.tripdiary.web;

import org.springframework.http.HttpStatus;

public final class ErrorCodes {

  private ErrorCodes() {}

  public static String from(HttpStatus status) {
    if (status == null) {
      return "INTERNAL";
    }
    return switch (status) {
      case BAD_REQUEST -> "VALIDATION";
      case UNAUTHORIZED -> "UNAUTHORIZED";
      case FORBIDDEN -> "FORBIDDEN";
      case NOT_FOUND -> "NOT_FOUND";
      case METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED";
      case CONFLICT -> "CONFLICT";
      case PAYLOAD_TOO_LARGE -> "PAYLOAD_TOO_LARGE";
      case UNSUPPORTED_MEDIA_TYPE -> "UNSUPPORTED_MEDIA";
      case INTERNAL_SERVER_ERROR -> "INTERNAL";
      default -> status.is4xxClientError() ? "CLIENT_ERROR" : "INTERNAL";
    };
  }
}
