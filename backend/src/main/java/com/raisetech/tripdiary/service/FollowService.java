package com.raisetech.tripdiary.service;

import com.raisetech.tripdiary.domain.Follow;
import com.raisetech.tripdiary.domain.Profile;
import com.raisetech.tripdiary.domain.User;
import com.raisetech.tripdiary.dto.ProfileResponse;
import com.raisetech.tripdiary.dto.UserListResponse;
import com.raisetech.tripdiary.dto.UserSummaryResponse;
import com.raisetech.tripdiary.mapper.FollowMapper;
import com.raisetech.tripdiary.mapper.UserMapper;
import com.raisetech.tripdiary.web.ApiException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

  private static final Logger log = LoggerFactory.getLogger(FollowService.class);

  private final FollowMapper follows;
  private final UserMapper users;

  public FollowService(FollowMapper follows, UserMapper users) {
    this.follows = follows;
    this.users = users;
  }

  public ProfileResponse profile(long viewerId, String username) {
    return ProfileResponse.from(requireProfile(username, viewerId), viewerId);
  }

  public UserListResponse search(long viewerId, String q) {
    List<String> patterns = searchPatterns(q);
    if (patterns.isEmpty()) {
      return new UserListResponse(List.of());
    }
    return new UserListResponse(
        users.search(patterns, viewerId).stream()
            .map(user -> UserSummaryResponse.from(user, viewerId))
            .toList());
  }

  public UserListResponse followees(long viewerId, String username) {
    Profile profile = requireProfile(username, viewerId);
    return new UserListResponse(
        follows.listFollowees(profile.getId(), viewerId).stream()
            .map(user -> UserSummaryResponse.from(user, viewerId))
            .toList());
  }

  public UserListResponse followers(long viewerId, String username) {
    Profile profile = requireProfile(username, viewerId);
    return new UserListResponse(
        follows.listFollowers(profile.getId(), viewerId).stream()
            .map(user -> UserSummaryResponse.from(user, viewerId))
            .toList());
  }

  @Transactional
  public ProfileResponse follow(long viewerId, String username) {
    User target = requireUser(username);
    if (target.getId() == viewerId) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "自分自身はフォローできません");
    }
    if (follows.find(viewerId, target.getId()) == null) {
      Follow row = new Follow();
      row.setFollowerId(viewerId);
      row.setFolloweeId(target.getId());
      follows.insert(row);
      log.info("フォロー viewerId={} followee={}", viewerId, username);
    }
    return ProfileResponse.from(requireProfile(username, viewerId), viewerId);
  }

  @Transactional
  public ProfileResponse unfollow(long viewerId, String username) {
    User target = requireUser(username);
    if (target.getId() == viewerId) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "自分自身はフォローできません");
    }
    follows.delete(viewerId, target.getId());
    log.info("フォロー解除 viewerId={} followee={}", viewerId, username);
    return ProfileResponse.from(requireProfile(username, viewerId), viewerId);
  }

  public void seedIfEmpty() {
    if (follows.count() > 0) {
      return;
    }
    User hanako = users.findByUsername("hanako");
    User yamada = users.findByUsername("yamada");
    if (hanako == null || yamada == null) {
      return;
    }
    Follow row = new Follow();
    row.setFollowerId(hanako.getId());
    row.setFolloweeId(yamada.getId());
    follows.insert(row);
  }

  private User requireUser(String username) {
    User user = users.findByUsername(username);
    if (user == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません");
    }
    return user;
  }

  private Profile requireProfile(String username, long viewerId) {
    Profile profile = users.findProfile(username, viewerId);
    if (profile == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません");
    }
    return profile;
  }

  static String normalizeQuery(String q) {
    String needle = q == null ? "" : q.trim();
    while (needle.startsWith("@")) {
      needle = needle.substring(1).trim();
    }
    return needle.replace("%", "").replace("_", "");
  }

  static List<String> searchPatterns(String q) {
    String safe = normalizeQuery(q);
    if (safe.isEmpty()) {
      return List.of();
    }
    Set<String> needles = new LinkedHashSet<>();
    needles.add(safe.toLowerCase(Locale.ROOT));
    needles.add(JapaneseText.toHiragana(safe).toLowerCase(Locale.ROOT));
    needles.add(JapaneseText.toKatakana(safe).toLowerCase(Locale.ROOT));
    List<String> snapshot = new ArrayList<>(needles);
    for (String form : snapshot) {
      String roma = JapaneseText.toRomaji(form);
      if (!roma.isEmpty()) {
        needles.add(roma);
        needles.add(JapaneseText.compactRomaji(roma));
      }
    }
    List<String> patterns = new ArrayList<>();
    for (String needle : needles) {
      if (!needle.isEmpty()) {
        patterns.add("%" + needle + "%");
      }
    }
    return patterns;
  }
}
