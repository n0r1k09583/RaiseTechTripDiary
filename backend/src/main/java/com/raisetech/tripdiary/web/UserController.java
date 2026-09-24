package com.raisetech.tripdiary.web;

import com.raisetech.tripdiary.config.OpenApiConfig;
import com.raisetech.tripdiary.dto.ErrorResponse;
import com.raisetech.tripdiary.dto.PostListResponse;
import com.raisetech.tripdiary.dto.ProfileResponse;
import com.raisetech.tripdiary.dto.UserListResponse;
import com.raisetech.tripdiary.service.FollowService;
import com.raisetech.tripdiary.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "ユーザー", description = "検索・プロフィール・フォロー。件数はサブクエリ（N+1 にしない）。")
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
public class UserController {

  private static final String ERROR_JSON = MediaType.APPLICATION_JSON_VALUE;

  private final FollowService follows;
  private final PostService posts;

  public UserController(FollowService follows, PostService posts) {
    this.follows = follows;
    this.posts = posts;
  }

  @GetMapping
  @Operation(summary = "ユーザー検索", description = "ユーザー名・表示名の部分一致。ひらがなとカタカナは同じ読みとして探す。空文字は SQL を叩かず空配列。LIKE の % と _ は無視する。")
  public UserListResponse search(
      HttpServletRequest request,
      @Parameter(description = "ユーザー名の一部") @RequestParam(defaultValue = "") String q) {
    return follows.search(userId(request), q);
  }

  @GetMapping("/{username}")
  @Operation(summary = "プロフィール", description = "フォロー数・フォロワー数・自分がフォローしているか。email は出さない。")
  @ApiResponse(
      responseCode = "404",
      description = "ユーザーが無い",
      content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  public ProfileResponse profile(
      HttpServletRequest request, @Parameter(description = "ユーザー名") @PathVariable String username) {
    return follows.profile(userId(request), username);
  }

  @GetMapping("/{username}/posts")
  @Operation(summary = "その人の投稿", description = "新しい順。ページングはタイムラインと同じ。")
  @ApiResponse(
      responseCode = "404",
      description = "ユーザーが無い",
      content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  public PostListResponse listPosts(
      HttpServletRequest request,
      @PathVariable String username,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) String beforeCreatedAt,
      @RequestParam(required = false) Long beforeId,
      @RequestParam(required = false) String afterCreatedAt,
      @RequestParam(required = false) Long afterId) {
    return posts.listByUsername(
        userId(request), username, limit, beforeCreatedAt, beforeId, afterCreatedAt, afterId);
  }

  @GetMapping("/{username}/followees")
  @Operation(summary = "フォロー中一覧", description = "その人がフォローしている人。1クエリ。")
  @ApiResponse(
      responseCode = "404",
      description = "ユーザーが無い",
      content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  public UserListResponse followees(HttpServletRequest request, @PathVariable String username) {
    return follows.followees(userId(request), username);
  }

  @GetMapping("/{username}/followers")
  @Operation(summary = "フォロワー一覧", description = "その人をフォローしている人。1クエリ。")
  @ApiResponse(
      responseCode = "404",
      description = "ユーザーが無い",
      content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  public UserListResponse followers(HttpServletRequest request, @PathVariable String username) {
    return follows.followers(userId(request), username);
  }

  @PostMapping("/{username}/follow")
  @Operation(summary = "フォローする", description = "自分は不可。既にしていればそのまま返す。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "更新後のプロフィール"),
    @ApiResponse(
        responseCode = "400",
        description = "自分自身",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "ユーザーが無い",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ProfileResponse follow(HttpServletRequest request, @PathVariable String username) {
    return follows.follow(userId(request), username);
  }

  @DeleteMapping("/{username}/follow")
  @Operation(summary = "フォローを外す", description = "未フォローでもプロフィールを返す。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "更新後のプロフィール"),
    @ApiResponse(
        responseCode = "400",
        description = "自分自身",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "ユーザーが無い",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ProfileResponse unfollow(HttpServletRequest request, @PathVariable String username) {
    return follows.unfollow(userId(request), username);
  }

  private static long userId(HttpServletRequest request) {
    return (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
  }
}
