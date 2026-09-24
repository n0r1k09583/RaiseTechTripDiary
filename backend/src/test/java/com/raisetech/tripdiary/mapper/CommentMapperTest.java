package com.raisetech.tripdiary.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.raisetech.tripdiary.domain.Comment;
import com.raisetech.tripdiary.domain.Post;
import com.raisetech.tripdiary.domain.User;
import com.raisetech.tripdiary.support.MapperH2Test;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@MapperH2Test
class CommentMapperTest {

  @Autowired
  CommentMapper comments;

  @Autowired
  PostMapper posts;

  @Autowired
  UserMapper users;

  @Test
  void 無いコメントはnull() {
    assertThat(comments.findById(404)).isNull();
  }

  @Test
  void 一覧は古い順で投稿削除時に消える() {
    User author = user("c1@example.com", "c1", "一");
    User other = user("c2@example.com", "c2", "二");
    Post post = post(author.getId());
    insert(post.getId(), other.getId(), "先", "2026-09-01 10:00:00");
    insert(post.getId(), author.getId(), "後", "2026-09-01 11:00:00");

    assertThat(comments.listByPostId(post.getId())).extracting(Comment::getBody).containsExactly("先", "後");
    assertThat(comments.deleteByPostId(post.getId())).isEqualTo(2);
    assertThat(comments.listByPostId(post.getId())).isEmpty();
  }

  @Test
  void 一件削除で他のコメントは残る() {
    User author = user("c3@example.com", "c3", "三");
    Post post = post(author.getId());
    Comment keep = insert(post.getId(), author.getId(), "残す", "2026-09-01 10:00:00");
    Comment gone = insert(post.getId(), author.getId(), "消す", "2026-09-01 10:01:00");
    comments.deleteById(gone.getId());
    assertThat(comments.findById(keep.getId()).getBody()).isEqualTo("残す");
    assertThat(comments.findById(gone.getId())).isNull();
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

  private Post post(long userId) {
    Post post = new Post();
    post.setUserId(userId);
    post.setBody("親");
    post.setCreatedAt("2026-09-01 09:00:00");
    posts.insert(post);
    return post;
  }

  private Comment insert(long postId, long userId, String body, String createdAt) {
    Comment comment = new Comment();
    comment.setPostId(postId);
    comment.setUserId(userId);
    comment.setBody(body);
    comment.setCreatedAt(createdAt);
    comments.insert(comment);
    return comment;
  }
}
