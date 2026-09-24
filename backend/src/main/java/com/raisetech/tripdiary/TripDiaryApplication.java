package com.raisetech.tripdiary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TripDiaryApplication {

  public static void main(String[] args) {
    loadDotEnv(Path.of(".env"));
    loadDotEnv(Path.of("backend", ".env"));
    SpringApplication.run(TripDiaryApplication.class, args);
  }

  static void loadDotEnv(Path file) {
    if (!Files.isRegularFile(file)) {
      return;
    }
    try {
      for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
          continue;
        }
        int eq = line.indexOf('=');
        String key = line.substring(0, eq).trim();
        String value = line.substring(eq + 1).trim();
        if (key.isEmpty() || System.getenv(key) != null) {
          continue;
        }
        if ("JWT_SECRET".equals(key)) {
          System.setProperty("jwt.secret", value);
        } else if ("JWT_ACCESS_EXPIRATION_MS".equals(key)) {
          System.setProperty("jwt.access-expiration-ms", value);
        } else if ("JWT_REFRESH_EXPIRATION_MS".equals(key)) {
          System.setProperty("jwt.refresh-expiration-ms", value);
        } else if ("PORT".equals(key)) {
          System.setProperty("server.port", value);
        } else {
          System.setProperty(key, value);
        }
      }
    } catch (IOException ignored) {
      // .env が読めなくてもデフォルト値で起動する
    }
  }
}
