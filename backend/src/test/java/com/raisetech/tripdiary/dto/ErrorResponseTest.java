package com.raisetech.tripdiary.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.raisetech.tripdiary.web.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorResponseTest {

  @Test
  void unauthorizedHasStableShape() {
    ErrorResponse body = new ErrorResponse(HttpStatus.UNAUTHORIZED, "ログインしてください");
    assertThat(body.getStatus()).isEqualTo(401);
    assertThat(body.getCode()).isEqualTo("UNAUTHORIZED");
    assertThat(body.getError()).isEqualTo("ログインしてください");
    assertThat(ErrorCodes.from(HttpStatus.UNAUTHORIZED)).isEqualTo("UNAUTHORIZED");
  }

  @Test
  void blankMessageFallsBack() {
    ErrorResponse body = new ErrorResponse(HttpStatus.NOT_FOUND, "  ");
    assertThat(body.getError()).isEqualTo("指定されたデータが見つかりません");
    assertThat(body.getCode()).isEqualTo("NOT_FOUND");
  }

  @Test
  void payloadTooLargeMessage() {
    ErrorResponse body = new ErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, null);
    assertThat(body.getError()).isEqualTo("画像は5MBまでです");
    assertThat(body.getCode()).isEqualTo("PAYLOAD_TOO_LARGE");
  }
}
