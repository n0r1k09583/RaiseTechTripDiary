package com.raisetech.tripdiary.service;

import com.raisetech.tripdiary.domain.Post;
import com.raisetech.tripdiary.domain.User;
import com.raisetech.tripdiary.dto.PostListResponse;
import com.raisetech.tripdiary.dto.PostResponse;
import com.raisetech.tripdiary.mapper.CommentMapper;
import com.raisetech.tripdiary.mapper.FavoriteMapper;
import com.raisetech.tripdiary.mapper.PostMapper;
import com.raisetech.tripdiary.mapper.UserMapper;
import com.raisetech.tripdiary.web.ApiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PostService {

  private static final Logger log = LoggerFactory.getLogger(PostService.class);
  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 50;
  private static final int BODY_MAX = 280;
  private static final int SPOT_MAX = 40;

  private final PostMapper posts;
  private final UserMapper users;
  private final CommentMapper comments;
  private final FavoriteMapper favorites;
  private final ImageStore images;

  public PostService(
      PostMapper posts, UserMapper users, CommentMapper comments, ImageStore images, FavoriteMapper favorites) {
    this.posts = posts;
    this.users = users;
    this.comments = comments;
    this.images = images;
    this.favorites = favorites;
  }

  public PostListResponse list(
      long viewerId,
      String tab,
      Integer limit,
      String beforeCreatedAt,
      Long beforeId,
      String afterCreatedAt,
      Long afterId) {
    return list(viewerId, tab, null, limit, beforeCreatedAt, beforeId, afterCreatedAt, afterId);
  }

  public PostListResponse list(
      long viewerId,
      String tab,
      String area,
      Integer limit,
      String beforeCreatedAt,
      Long beforeId,
      String afterCreatedAt,
      Long afterId) {
    String normalizedTab = normalizeTab(tab);
    String areaFilter = blankToNull(area);
    int size = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
    boolean pagingOlder = beforeCreatedAt != null && beforeId != null;
    boolean pagingNewer = afterCreatedAt != null && afterId != null;
    int fetch = pagingNewer ? size : size + 1;
    List<Post> rows = posts.list(
        viewerId,
        normalizedTab,
        areaFilter,
        fetch,
        pagingOlder ? beforeCreatedAt : null,
        pagingOlder ? beforeId : null,
        pagingNewer ? afterCreatedAt : null,
        pagingNewer ? afterId : null);
    boolean hasMore = !pagingNewer && rows.size() > size;
    if (hasMore) {
      rows = rows.subList(0, size);
    }
    List<PostResponse> body = rows.stream().map(post -> PostResponse.from(post, viewerId)).toList();
    return new PostListResponse(body, hasMore);
  }

  public PostResponse get(long viewerId, long id) {
    Post post = posts.findForViewer(id, viewerId);
    if (post == null || !PostAccess.canSee(post, viewerId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "投稿が見つかりません");
    }
    return PostResponse.from(post, viewerId);
  }

  public PostListResponse listByUsername(
      long viewerId,
      String username,
      Integer limit,
      String beforeCreatedAt,
      Long beforeId,
      String afterCreatedAt,
      Long afterId) {
    User author = users.findByUsername(username);
    if (author == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません");
    }
    int size = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
    boolean pagingOlder = beforeCreatedAt != null && beforeId != null;
    boolean pagingNewer = afterCreatedAt != null && afterId != null;
    int fetch = pagingNewer ? size : size + 1;
    List<Post> rows = posts.listByAuthor(
        viewerId,
        author.getId(),
        fetch,
        pagingOlder ? beforeCreatedAt : null,
        pagingOlder ? beforeId : null,
        pagingNewer ? afterCreatedAt : null,
        pagingNewer ? afterId : null);
    boolean hasMore = !pagingNewer && rows.size() > size;
    if (hasMore) {
      rows = rows.subList(0, size);
    }
    List<PostResponse> body = rows.stream().map(post -> PostResponse.from(post, viewerId)).toList();
    return new PostListResponse(body, hasMore);
  }

  @Transactional
  public PostResponse create(long userId, String body, MultipartFile image) {
    return create(userId, "未設定の場所", "関東", "visited", body, image);
  }

  @Transactional
  public PostResponse create(
      long userId,
      String spotName,
      String areaTag,
      String visitStatus,
      String body,
      MultipartFile image) {
    return create(userId, spotName, areaTag, visitStatus, body, image, null, null, null);
  }

  @Transactional
  public PostResponse create(
      long userId,
      String spotName,
      String areaTag,
      String visitStatus,
      String body,
      MultipartFile image,
      String visibility,
      String latitude,
      String longitude) {
    boolean hasImage = image != null && !image.isEmpty();
    String text = requireBody(body, hasImage);
    Double lat = parseCoord(latitude, -90, 90, "緯度は-90から90です");
    Double lng = parseCoord(longitude, -180, 180, "経度は-180から180です");
    requirePair(lat, lng);
    Post post = new Post();
    post.setUserId(userId);
    post.setSpotName(requireSpot(spotName));
    post.setAreaTag(requireArea(areaTag));
    post.setVisitStatus(requireStatus(visitStatus));
    post.setBody(text);
    post.setImagePath(images.save(image));
    post.setVisibility(requireVisibility(visibility));
    post.setLatitude(lat);
    post.setLongitude(lng);
    posts.insert(post);
    log.info("投稿を作成 userId={} postId={}", userId, post.getId());
    return PostResponse.from(posts.findForViewer(post.getId(), userId), userId);
  }

  @Transactional
  public PostResponse update(long userId, long id, String body, MultipartFile image) {
    return update(userId, id, null, null, null, body, image);
  }

  @Transactional
  public PostResponse update(
      long userId,
      long id,
      String spotName,
      String areaTag,
      String visitStatus,
      String body,
      MultipartFile image) {
    Post existing = requireOwned(id, userId, "自分の投稿だけ編集できます");
    boolean keepImage = existing.getImagePath() != null && !existing.getImagePath().isBlank();
    boolean hasImage = (image != null && !image.isEmpty()) || keepImage;
    existing.setBody(requireBody(body, hasImage));
    if (spotName != null && !spotName.isBlank()) {
      existing.setSpotName(requireSpot(spotName));
    }
    if (areaTag != null && !areaTag.isBlank()) {
      existing.setAreaTag(requireArea(areaTag));
    }
    if (visitStatus != null && !visitStatus.isBlank()) {
      existing.setVisitStatus(requireStatus(visitStatus));
    }
    if (image != null && !image.isEmpty()) {
      String previous = existing.getImagePath();
      existing.setImagePath(images.save(image));
      images.delete(previous);
    }
    posts.update(existing);
    return PostResponse.from(posts.findForViewer(id, userId), userId);
  }

  @Transactional
  public PostResponse update(
      long userId,
      long id,
      String spotName,
      String areaTag,
      String visitStatus,
      String body,
      MultipartFile image,
      String visibility,
      String latitude,
      String longitude,
      boolean replacePlace) {
    PostResponse updated = update(userId, id, spotName, areaTag, visitStatus, body, image);
    if (!replacePlace) {
      return updated;
    }
    Post existing = requireOwned(id, userId, "自分の投稿だけ編集できます");
    if (visibility != null && !visibility.isBlank()) {
      existing.setVisibility(requireVisibility(visibility));
    }
    if (latitude != null || longitude != null) {
      Double lat = parseCoord(latitude, -90, 90, "緯度は-90から90です");
      Double lng = parseCoord(longitude, -180, 180, "経度は-180から180です");
      requirePair(lat, lng);
      existing.setLatitude(lat);
      existing.setLongitude(lng);
    }
    posts.update(existing);
    return PostResponse.from(posts.findForViewer(id, userId), userId);
  }

  @Transactional
  public void delete(long userId, long id) {
    Post existing = requireOwned(id, userId, "自分の投稿だけ削除できます");
    comments.deleteByPostId(id);
    favorites.deleteByPostId(id);
    posts.deleteById(id);
    images.delete(existing.getImagePath());
    log.info("投稿を削除 userId={} postId={}", userId, id);
  }

  public void seedIfEmpty() {
    if (posts.count() == 0) {
      insertSeed("hanako", "函館山", "北海道", "visited", "夜景がきれいで、もう一度行きたい", "2026-08-17 21:00:00");
      insertSeed("ichiro", "金閣寺", "近畿", "visited", "金箔が思ったよりまぶしかった", "2026-08-17 20:45:00");
      insertSeed("yamada", "恩納村の海", "沖縄", "visited", "水が透き通っていて泳いだ", "2026-08-17 22:10:00");
    }
    attachSeedPhotos();
  }

  private void insertSeed(
      String username, String spotName, String areaTag, String visitStatus, String body, String createdAt) {
    User user = users.findByUsername(username);
    if (user == null) {
      return;
    }
    Post post = new Post();
    post.setUserId(user.getId());
    post.setSpotName(spotName);
    post.setAreaTag(areaTag);
    post.setVisitStatus(visitStatus);
    post.setBody(body);
    post.setCreatedAt(createdAt);
    posts.insert(post);
  }

  private void attachSeedPhotos() {
    attachSeedPhoto("函館山", "hakodate.jpg");
    attachSeedPhoto("金閣寺", "kinkaku.jpg");
    attachSeedPhoto("恩納村の海", "onna.jpg");
  }

  private void attachSeedPhoto(String spotName, String resourceName) {
    String saved = images.installClasspath("seed-photos/" + resourceName, resourceName);
    if (saved == null) {
      return;
    }
    posts.setImagePathIfBlank(spotName, saved);
  }

  private Post require(long id) {
    Post post = posts.findById(id);
    if (post == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "投稿が見つかりません");
    }
    return post;
  }

  private Post requireOwned(long id, long userId, String message) {
    Post post = require(id);
    if (post.getUserId() == null || post.getUserId() != userId) {
      log.warn("投稿の権限なし userId={} postId={}", userId, id);
      throw new ApiException(HttpStatus.FORBIDDEN, message);
    }
    return post;
  }

  private static String requireBody(String body, boolean hasImage) {
    String text = body == null ? "" : body.trim();
    if (text.length() > BODY_MAX) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "本文は1〜280文字です");
    }
    if (text.isEmpty() && !hasImage) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "本文は1〜280文字です");
    }
    return text;
  }

  private static String requireSpot(String spotName) {
    String text = spotName == null ? "" : spotName.trim();
    if (text.isEmpty() || text.length() > SPOT_MAX) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "場所名は1〜40文字です");
    }
    return text;
  }

  private static String requireArea(String areaTag) {
    String text = areaTag == null ? "" : areaTag.trim();
    if (!Areas.contains(text)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "エリアを選んでください");
    }
    return text;
  }

  private static String requireStatus(String visitStatus) {
    if ("visited".equals(visitStatus) || "want".equals(visitStatus)) {
      return visitStatus;
    }
    throw new ApiException(HttpStatus.BAD_REQUEST, "訪問済みか行きたいを選んでください");
  }

  private static String normalizeTab(String tab) {
    if ("following".equals(tab)
        || "visited".equals(tab)
        || "want".equals(tab)
        || "photos".equals(tab)
        || "favorites".equals(tab)) {
      return tab;
    }
    return "all";
  }

  private static String requireVisibility(String visibility) {
    if (visibility == null || visibility.isBlank() || "public".equals(visibility)) {
      return "public";
    }
    if ("private".equals(visibility)) {
      return "private";
    }
    throw new ApiException(HttpStatus.BAD_REQUEST, "公開か非公開を選んでください");
  }

  private static Double parseCoord(String raw, double min, double max, String message) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      double value = Double.parseDouble(raw.trim());
      if (Double.isNaN(value) || value < min || value > max) {
        throw new ApiException(HttpStatus.BAD_REQUEST, message);
      }
      return value;
    } catch (NumberFormatException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, message);
    }
  }

  private static void requirePair(Double latitude, Double longitude) {
    if ((latitude == null) != (longitude == null)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "緯度と経度はセットで指定してください");
    }
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
