package com.raisetech.tripdiary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class FlywayMigrationTest {

  @Autowired
  Flyway flyway;

  @Autowired
  JdbcTemplate jdbc;

  @Test
  void appliesUsersThroughFollowsOnH2() {
    assertNotNull(flyway.info().current());
    assertEquals("7", flyway.info().current().getVersion().getVersion());
    assertEquals(7, flyway.info().applied().length);

    MigrateResult result = flyway.migrate();
    assertEquals(0, result.migrationsExecuted);

    Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    Integer refreshTokens = jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens", Integer.class);
    Integer posts = jdbc.queryForObject("SELECT COUNT(*) FROM posts", Integer.class);
    Integer comments = jdbc.queryForObject("SELECT COUNT(*) FROM comments", Integer.class);
    Integer likes = jdbc.queryForObject("SELECT COUNT(*) FROM likes", Integer.class);
    Integer follows = jdbc.queryForObject("SELECT COUNT(*) FROM follows", Integer.class);
    Integer favorites = jdbc.queryForObject("SELECT COUNT(*) FROM favorites", Integer.class);
    assertTrue(users != null && users >= 3);
    assertTrue(refreshTokens != null && refreshTokens >= 0);
    assertTrue(posts != null && posts >= 1);
    assertTrue(comments != null && comments >= 1);
    assertTrue(likes != null && likes >= 0);
    assertTrue(follows != null && follows >= 0);
    assertTrue(favorites != null && favorites >= 0);
  }
}
