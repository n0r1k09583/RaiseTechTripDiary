package com.raisetech.tripdiary.web;

import com.raisetech.tripdiary.config.OpenApiConfig;
import com.raisetech.tripdiary.dto.ErrorResponse;
import com.raisetech.tripdiary.dto.PostListResponse;
import com.raisetech.tripdiary.dto.PostResponse;
import com.raisetech.tripdiary.service.FavoriteService;
import com.raisetech.tripdiary.service.LikeService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "投稿", description = "旅の記録の一覧・作成・編集・削除。場所名・エリア・感想・任意の写真1枚。")
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
public class PostController {

  private static final String ERROR_JSON = MediaType.APPLICATION_JSON_VALUE;

  private final PostService posts;
  private final LikeService likes;
  private final FavoriteService favorites;

  public PostController(PostService posts, LikeService likes, FavoriteService favorites) {
    this.posts = posts;
    this.likes = likes;
    this.favorites = favorites;
  }

  @GetMapping
  @Operation(
      summary = "タイムライン",
      description =
          """
          新しい順。続き（無限スクロール）は beforeCreatedAt + beforeId。
          新しい差分は afterCreatedAt + afterId。
          tab=following は自分とフォロー中。tab=visited は自分の訪問済み。tab=want は行きたい（いいねした投稿）。
          tab=photos は写真つき。area でエリアタグ絞り込み。フォロー先・件数はサブクエリ1回（N+1 にしない）。
          """)
  @ApiResponse(
      responseCode = "200",
      description = "新しい順の一覧",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = PostListResponse.class)))
  public PostListResponse list(
      HttpServletRequest request,
      @Parameter(description = "all=全投稿。following=自分とフォロー中。visited=自分の訪問済み。want=行きたい。photos=写真つき")
          @RequestParam(defaultValue = "all")
          String tab,
      @Parameter(description = "北海道〜海外。空なら全エリア") @RequestParam(required = false) String area,
      @Parameter(description = "件数。省略時20、最大50") @RequestParam(required = false) Integer limit,
      @Parameter(description = "これより古い投稿を取る（続き）") @RequestParam(required = false)
          String beforeCreatedAt,
      @Parameter(description = "同じ時刻のときのタイブレーク") @RequestParam(required = false) Long beforeId,
      @Parameter(description = "これより新しい投稿を取る（静かな取り直し）") @RequestParam(required = false)
          String afterCreatedAt,
      @Parameter(description = "同じ時刻のときのタイブレーク") @RequestParam(required = false) Long afterId) {
    long userId = userId(request);
    return posts.list(userId, tab, area, limit, beforeCreatedAt, beforeId, afterCreatedAt, afterId);
  }

  @GetMapping("/{id}")
  @Operation(summary = "投稿1件", description = "編集画面・詳細用。無いと 404。")
  @ApiResponse(
      responseCode = "404",
      description = "投稿が無い",
      content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  public PostResponse get(
      HttpServletRequest request, @Parameter(description = "投稿ID") @PathVariable long id) {
    return posts.get(userId(request), id);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "投稿を作成",
      description =
          """
          multipart/form-data。spotName・areaTag 必須。visitStatus は visited か want。
          感想（body）か写真（image）のどちらかが必要。写真があれば感想は空でもよい。
          画像は JPEG / PNG / WebP、5MB。実体は uploads/、DB にはファイル名。
          応答の imageUrl は /uploads/ファイル名。S3 に出すときはこの URL だけ差し替える。
          """)
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "作成した投稿。画面は先頭へすぐ出す"),
    @ApiResponse(
        responseCode = "400",
        description = "本文不正、または画像が JPEG/PNG/WebP でない",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "413",
        description = "画像が5MB超。`画像は5MBまでです`",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public PostResponse create(
      HttpServletRequest request,
      @Parameter(description = "場所名 1〜40文字", required = true) @RequestParam String spotName,
      @Parameter(description = "エリアタグ", required = true) @RequestParam String areaTag,
      @Parameter(description = "visited=訪問済み。want=行きたい") @RequestParam(defaultValue = "visited")
          String visitStatus,
      @Parameter(description = "感想。写真があれば空でも可。最大280文字") @RequestParam(required = false, defaultValue = "")
          String body,
      @Parameter(description = "任意。JPEG / PNG / WebP、5MBまで") @RequestParam(required = false)
          MultipartFile image,
      @Parameter(description = "public=みんなの記録に出す。private=本人だけ") @RequestParam(defaultValue = "public")
          String visibility,
      @Parameter(description = "緯度。経度とセット。空なら未設定") @RequestParam(required = false) String latitude,
      @Parameter(description = "経度。緯度とセット。空なら未設定") @RequestParam(required = false) String longitude) {
    return posts.create(
        userId(request), spotName, areaTag, visitStatus, body, image, visibility, latitude, longitude);
  }

  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "自分の投稿を編集", description = "他人の投稿は 403。無いと 404。multipart。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "更新後の投稿"),
    @ApiResponse(
        responseCode = "400",
        description = "本文不正、または画像形式不正",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "他人の投稿は編集できない",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "投稿が無い",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "413",
        description = "画像が5MB超",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public PostResponse update(
      HttpServletRequest request,
      @Parameter(description = "投稿ID") @PathVariable long id,
      @Parameter(description = "場所名 1〜40文字") @RequestParam(required = false) String spotName,
      @Parameter(description = "エリアタグ") @RequestParam(required = false) String areaTag,
      @Parameter(description = "visited か want") @RequestParam(required = false) String visitStatus,
      @Parameter(description = "感想。写真があれば空でも可。最大280文字") @RequestParam(required = false, defaultValue = "")
          String body,
      @Parameter(description = "任意。省略時は既存画像のまま") @RequestParam(required = false)
          MultipartFile image,
      @Parameter(description = "public か private。省略時は現状のまま") @RequestParam(required = false)
          String visibility,
      @Parameter(description = "緯度。空文字で位置を消す") @RequestParam(required = false) String latitude,
      @Parameter(description = "経度。空文字で位置を消す") @RequestParam(required = false) String longitude) {
    return posts.update(
        userId(request), id, spotName, areaTag, visitStatus, body, image, visibility, latitude, longitude, true);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "自分の投稿を削除", description = "他人は 403。204。画像ファイルも消す。")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "削除した。本文なし"),
    @ApiResponse(
        responseCode = "403",
        description = "他人の投稿は削除できない",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "投稿が無い",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public void delete(
      HttpServletRequest request, @Parameter(description = "投稿ID") @PathVariable long id) {
    posts.delete(userId(request), id);
  }

  @PostMapping("/{id}/likes")
  @Operation(summary = "行きたいをトグル", description = "未登録なら付ける。済みなら外す。行きたいリスト（tab=want）に使う。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "トグル後の投稿（likeCount と likedByMe）"),
    @ApiResponse(
        responseCode = "404",
        description = "投稿が無い",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public PostResponse toggleLike(
      HttpServletRequest request, @Parameter(description = "投稿ID") @PathVariable long id) {
    return likes.toggle(userId(request), id);
  }

  @PostMapping("/{id}/favorites")
  @Operation(summary = "お気に入りをトグル", description = "本人の一覧だけに出る。他人のお気に入りは見えない。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "トグル後の投稿（favoritedByMe）"),
    @ApiResponse(
        responseCode = "404",
        description = "投稿が無い、または非公開で本人以外",
        content = @Content(mediaType = ERROR_JSON, schema = @Schema(implementation = ErrorResponse.class)))
  })
  public PostResponse toggleFavorite(
      HttpServletRequest request, @Parameter(description = "投稿ID") @PathVariable long id) {
    return favorites.toggle(userId(request), id);
  }

  private static long userId(HttpServletRequest request) {
    return (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
  }
}
