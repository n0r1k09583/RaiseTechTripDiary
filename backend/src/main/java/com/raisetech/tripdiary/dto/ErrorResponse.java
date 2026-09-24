package com.raisetech.tripdiary.dto;

import com.raisetech.tripdiary.web.ErrorCodes;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

@Schema(
    name = "ErrorResponse",
    description = "エラー時の JSON。画面は error を出す。code は機械向け。status は HTTP ステータスと揃える。")
public class ErrorResponse {

  private final int status;
  private final String code;
  private final String error;

  public ErrorResponse(String error) {
    this(HttpStatus.BAD_REQUEST, error);
  }

  public ErrorResponse(HttpStatus status, String error) {
    HttpStatus resolved = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    this.status = resolved.value();
    this.code = ErrorCodes.from(resolved);
    this.error = error == null || error.isBlank() ? fallback(resolved) : error;
  }

  @Schema(description = "HTTP ステータス", example = "401")
  public int getStatus() {
    return status;
  }

  @Schema(description = "機械向けコード（UNAUTHORIZED, VALIDATION, FORBIDDEN など）", example = "UNAUTHORIZED")
  public String getCode() {
    return code;
  }

  @Schema(description = "画面に出す日本語", example = "ログインしてください")
  public String getError() {
    return error;
  }

  private static String fallback(HttpStatus status) {
    return switch (status) {
      case UNAUTHORIZED -> "ログインしてください";
      case FORBIDDEN -> "この操作はできません";
      case NOT_FOUND -> "指定されたデータが見つかりません";
      case METHOD_NOT_ALLOWED -> "この操作はできません";
      case PAYLOAD_TOO_LARGE -> "画像は5MBまでです";
      case CONFLICT -> "すでに登録されています";
      default -> status.is4xxClientError() ? "リクエストの形式が正しくありません" : "サーバーで問題が起きました";
    };
  }
}
