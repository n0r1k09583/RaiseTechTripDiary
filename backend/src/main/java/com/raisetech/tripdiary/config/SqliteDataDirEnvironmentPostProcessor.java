package com.raisetech.tripdiary.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * SQLite の親フォルダを Flyway より先に作る。無いとマイグレーションがファイルを開けない。
 */
public class SqliteDataDirEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    String url = environment.getProperty("spring.datasource.url", "");
    if (!url.startsWith("jdbc:sqlite:")) {
      return;
    }
    String path = url.substring("jdbc:sqlite:".length());
    if (path.contains(":memory:") || path.startsWith("file:")) {
      return;
    }
    Path parent = Path.of(path).getParent();
    if (parent == null) {
      return;
    }
    try {
      Files.createDirectories(parent);
    } catch (IOException ex) {
      throw new IllegalStateException("SQLite のディレクトリを作れません: " + parent, ex);
    }
  }
}
