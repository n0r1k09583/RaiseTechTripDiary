package com.raisetech.tripdiary.web;

import com.raisetech.tripdiary.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
    if (ex.getStatus().is5xxServerError()) {
      log.error("APIエラー status={} message={}", ex.getStatus().value(), ex.getMessage(), ex);
    } else {
      log.warn("APIエラー status={} message={}", ex.getStatus().value(), ex.getMessage());
    }
    return json(ex.getStatus(), ex.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleBadJson(HttpMessageNotReadableException ex) {
    return json(HttpStatus.BAD_REQUEST, "リクエストの形式が正しくありません");
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissing(MissingServletRequestParameterException ex) {
    if ("body".equals(ex.getParameterName())) {
      return json(HttpStatus.BAD_REQUEST, "本文は1〜280文字です");
    }
    return json(HttpStatus.BAD_REQUEST, "必要な項目が足りません");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleType(MethodArgumentTypeMismatchException ex) {
    return json(HttpStatus.BAD_REQUEST, "リクエストの形式が正しくありません");
  }

  @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
  public ResponseEntity<ErrorResponse> handleTooLarge(Exception ex) {
    return json(HttpStatus.PAYLOAD_TOO_LARGE, "画像は5MBまでです");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMedia(HttpMediaTypeNotSupportedException ex) {
    return json(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "対応していない形式です");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex) {
    return json(HttpStatus.METHOD_NOT_ALLOWED, "この操作はできません");
  }

  @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
  public ResponseEntity<ErrorResponse> handleMissingRoute(Exception ex) {
    return json(HttpStatus.NOT_FOUND, "指定されたURLが見つかりません");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
    log.error("未処理のエラー", ex);
    return json(HttpStatus.INTERNAL_SERVER_ERROR, "サーバーで問題が起きました");
  }

  private static ResponseEntity<ErrorResponse> json(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(new ErrorResponse(status, message));
  }
}
