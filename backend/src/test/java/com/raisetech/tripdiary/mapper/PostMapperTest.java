package com.raisetech.tripdiary.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.raisetech.tripdiary.domain.Comment;
import com.raisetech.tripdiary.domain.Follow;
import com.raisetech.tripdiary.domain.Like;
import com.raisetech.tripdiary.domain.Post;
import com.raisetech.tripdiary.domain.User;
import com.raisetech.tripdiary.support.MapperH2Test;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@MapperH2Test
class PostMapperTest {

  @Autowired
  PostMapper posts;

  @Autowired
  UserMapper users;

  @Autowired
  CommentMapper comments;

  @Autowired
  LikeMapper likes;

  @Autowired
  FollowMapper follows;

  @Test
  void 無い投稿はnull() {
    assertThat(posts.findById(404)).isNull();
  }

  @Test
  void コメント件数は同じSELECTで取る() {
    User author = user("a@example.com", "author", "投稿者");
    User other = user("b@example.com", "other", "他者");
    Post post = post(author.getId(), "件数確認", "2026-09-01 10:00:00");
    comment(post.getId(), other.getId(), "1");
    comment(post.getId(), other.getId(), "2");

    Post loaded = posts.findById(post.getId());
    assertThat(loaded.getCommentCount()).isEqualTo(2);
    assertThat(loaded.getLikeCount()).isZero();
    assertThat(loaded.isLikedByMe()).isFalse();
    assertThat(loaded.getUsername()).isEqualTo("author");
  }

  @Test
  void フォロー中タブは他人の投稿を出さない() {
    User me = user("me@example.com", "me_user", "自分");
    User them = user("them@example.com", "them_user", "他人");
    post(me.getId(), "自分の投稿", "2026-09-01 12:00:00");
    post(them.getId(), "他人の投稿", "2026-09-01 13:00:00");

    assertThat(posts.list(me.getId(), "following", null, 20, null, null, null, null))
        .extracting(Post::getBody)
        .containsExactly("自分の投稿");
    assertThat(posts.list(me.getId(), "all", null, 20, null, null, null, null))
        .extracting(Post::getBody)
        .containsExactly("他人の投稿", "自分の投稿");
  }

  @Test
  void コメント無しの件数は0() {
    User author = user("z@example.com", "zero_c", "零");
    Post post = post(author.getId(), "件数ゼロ", "2026-09-01 09:00:00");
    assertThat(posts.findById(post.getId()).getCommentCount()).isZero();
  }

  @Test
  void 同時刻ならidでタイブレークする() {
    User me = user("t@example.com", "tie_user", "同刻");
    Post first = post(me.getId(), "先", "2026-09-01 10:00:00");
    Post second = post(me.getId(), "後", "2026-09-01 10:00:00");

    assertThat(posts.list(me.getId(), "all", null, 20, null, null, first.getCreatedAt(), first.getId()))
        .extracting(Post::getId)
        .containsExactly(second.getId());
  }

  @Test
  void 古いページに新しい投稿を混ぜない() {
    User me = user("p@example.com", "pager", "頁");
    Post older = post(me.getId(), "古い", "2026-09-01 10:00:00");
    Post newer = post(me.getId(), "新しい", "2026-09-01 11:00:00");

    assertThat(posts.list(me.getId(), "all", null, 20, newer.getCreatedAt(), newer.getId(), null, null))
        .extracting(Post::getId)
        .containsExactly(older.getId());
  }

  @Test
  void いいね件数と自分の状態は同じSELECTで取る() {
    User author = user("like_a@example.com", "like_a", "いいねA");
    User fan = user("like_b@example.com", "like_b", "いいねB");
    Post post = post(author.getId(), "いいね確認", "2026-09-01 10:00:00");
    Like like = new Like();
    like.setPostId(post.getId());
    like.setUserId(fan.getId());
    likes.insert(like);

    Post viewer = posts.findForViewer(post.getId(), fan.getId());
    assertThat(viewer.getLikeCount()).isEqualTo(1);
    assertThat(viewer.isLikedByMe()).isTrue();
    Post other = posts.findForViewer(post.getId(), author.getId());
    assertThat(other.getLikeCount()).isEqualTo(1);
    assertThat(other.isLikedByMe()).isFalse();
  }

  @Test
  void フォロー中タブはフォロー先と自分を1クエリで出す() {
    User me = user("fol_me@example.com", "fol_me", "自分");
    User them = user("fol_them@example.com", "fol_them", "相手");
    User other = user("fol_other@example.com", "fol_other", "第三者");
    post(me.getId(), "自分の投稿", "2026-09-01 12:00:00");
    post(them.getId(), "相手の投稿", "2026-09-01 13:00:00");
    post(other.getId(), "第三者の投稿", "2026-09-01 14:00:00");
    Follow follow = new Follow();
    follow.setFollowerId(me.getId());
    follow.setFolloweeId(them.getId());
    follows.insert(follow);

    assertThat(posts.list(me.getId(), "following", null, 20, null, null, null, null))
        .extracting(Post::getBody)
        .containsExactly("相手の投稿", "自分の投稿");
  }

  @Test
  void エリアと行きたいリストで絞り込める() {
    User me = user("area@example.com", "area_me", "自分");
    User them = user("area2@example.com", "area_them", "相手");
    Post hokkaido = post(them.getId(), "函館", "北海道", "visited", "夜景", "2026-09-01 10:00:00");
    post(me.getId(), "金閣寺", "近畿", "visited", "金色", "2026-09-01 11:00:00");

    assertThat(posts.list(me.getId(), "all", "北海道", 20, null, null, null, null))
        .extracting(Post::getSpotName)
        .containsExactly("函館");
    assertThat(posts.list(me.getId(), "visited", null, 20, null, null, null, null))
        .extracting(Post::getSpotName)
        .containsExactly("金閣寺");

    Like like = new Like();
    like.setPostId(hokkaido.getId());
    like.setUserId(me.getId());
    likes.insert(like);

    assertThat(posts.list(me.getId(), "want", null, 20, null, null, null, null))
        .extracting(Post::getSpotName)
        .containsExactly("函館");
  }

  @Test
  void 写真タブは画像なしを出さない() {
    User me = user("photo@example.com", "photo_me", "自分");
    Post withPhoto = post(me.getId(), "函館", "北海道", "visited", "夜景", "2026-09-01 10:00:00");
    withPhoto.setImagePath("hakodate.jpg");
    posts.update(withPhoto);
    post(me.getId(), "金閣寺", "近畿", "visited", "金色", "2026-09-01 11:00:00");

    assertThat(posts.list(me.getId(), "photos", null, 20, null, null, null, null))
        .extracting(Post::getSpotName)
        .containsExactly("函館");
  }

  @Test
  void 非公開はみんなの記録に出ない() {
    User me = user("priv@example.com", "priv_user", "非公開");
    User them = user("pub@example.com", "pub_user", "公開");
    Post hidden = post(me.getId(), "秘密の浜", "沖縄", "visited", "非公開の本文", "2026-09-02 10:00:00");
    hidden.setVisibility("private");
    posts.update(hidden);
    post(them.getId(), "東京駅", "関東", "visited", "公開の本文", "2026-09-02 11:00:00");

    assertThat(posts.list(them.getId(), "all", null, 20, null, null, null, null))
        .extracting(Post::getBody)
        .contains("公開の本文")
        .doesNotContain("非公開の本文");
    assertThat(posts.list(me.getId(), "visited", null, 20, null, null, null, null))
        .extracting(Post::getBody)
        .contains("非公開の本文");
  }

  private User user(String email, String username, String displayName) {
    User user = new User();
    user.setEmail(email);
    user.setUsername(username);
    user.setDisplayName(displayName);
    user.setPasswordDigest("hash");
    users.insert(user);
    return user;
  }

  private Post post(long userId, String body, String createdAt) {
    return post(userId, "東京駅", "関東", "visited", body, createdAt);
  }

  private Post post(long userId, String spotName, String areaTag, String visitStatus, String body, String createdAt) {
    Post post = new Post();
    post.setUserId(userId);
    post.setSpotName(spotName);
    post.setAreaTag(areaTag);
    post.setVisitStatus(visitStatus);
    post.setBody(body);
    post.setCreatedAt(createdAt);
    posts.insert(post);
    return posts.findById(post.getId());
  }

  private void comment(long postId, long userId, String body) {
    Comment comment = new Comment();
    comment.setPostId(postId);
    comment.setUserId(userId);
    comment.setBody(body);
    comments.insert(comment);
  }
}
