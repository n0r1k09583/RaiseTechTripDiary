package com.raisetech.tripdiary.web;

import com.raisetech.tripdiary.config.OpenApiConfig;
import com.raisetech.tripdiary.dto.CommentListResponse;
import com.raisetech.tripdiary.dto.CommentRequest;
import com.raisetech.tripdiary.dto.CommentResponse;
import com.raisetech.tripdiary.dto.ErrorResponse;
import com.raisetech.tripdiary.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "コメント", description = "投稿へのコメント。件数は投稿一覧の同じ SELECT のサブクエリ（N+1 にしない）。")
@SecurityRequirement(name = OpenApiConfig.BEARER)
@ApiResponses({
  @ApiResponse(
      responseCode = "401",
      description = "未ログイン、またはトークン無効",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)))
})
public class CommentController {

  private static final String ERROR_JSON = MediaType.APPLICATION_JSON_VALUE;

  private final CommentService comments;

  public CommentController(CommentService comments) {
    this.comments = comments;
  }

  @GetMapping("/api/posts/{postId}/comments")
  @Operation(summary = "コメント一覧", description = "古い順。投稿が無いと 404。")
  @ApiResponse(
      responseCode = "404",
      description = "投稿が無い",
      content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  public CommentListResponse list(
      HttpServletRequest request, @Parameter(description = "投稿ID") @PathVariable long postId) {
    return comments.list(userId(request), postId);
  }

  @PostMapping("/api/posts/{postId}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "コメントを作成", description = "JSON `{ body }`。1〜140文字。自分の投稿にも他人の投稿にも可。")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "作成したコメント"),
    @ApiResponse(
        responseCode = "400",
        description = "空、または141文字以上",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "投稿が無い",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public CommentResponse create(
      HttpServletRequest request,
      @Parameter(description = "投稿ID") @PathVariable long postId,
      @RequestBody CommentRequest body) {
    return comments.create(userId(request), postId, body == null ? null : body.getBody());
  }

  @DeleteMapping("/api/comments/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "自分のコメントを削除", description = "他人は 403。204。件数も減る。")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "削除した。本文なし"),
    @ApiResponse(
        responseCode = "403",
        description = "他人のコメントは削除できない",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "コメントが無い",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public void delete(
      HttpServletRequest request, @Parameter(description = "コメントID") @PathVariable long id) {
    comments.delete(userId(request), id);
  }

  private static long userId(HttpServletRequest request) {
    return (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
  }
}
