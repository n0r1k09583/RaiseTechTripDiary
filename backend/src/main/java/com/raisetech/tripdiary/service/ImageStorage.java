package com.raisetech.tripdiary.service;

import com.raisetech.tripdiary.web.ApiException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "app.storage", havingValue = "local", matchIfMissing = true)
public class ImageStorage implements ImageStore {

  private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);

  private final Path dir;

  public ImageStorage(@Value("${app.upload-dir:./uploads}") String uploadDir) {
    this.dir = Path.of(uploadDir).toAbsolutePath().normalize();
  }

  @PostConstruct
  public void init() {
    try {
      Files.createDirectories(dir);
    } catch (IOException ex) {
      throw new IllegalStateException("uploads ディレクトリを作れません: " + dir, ex);
    }
  }

  public Path directory() {
    return dir;
  }

  @Override
  public String save(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    String name = ImageRules.newFilename(file, log);
    if (name == null) {
      return null;
    }
    Path dest = dir.resolve(name);
    try {
      file.transferTo(dest);
    } catch (IOException ex) {
      log.error("画像の保存に失敗しました", ex);
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "画像の保存に失敗しました");
    }
    return name;
  }

  @Override
  public String installClasspath(String classpath, String destName) {
    if (classpath == null || destName == null || destName.isBlank()) {
      return null;
    }
    String safe = Path.of(destName).getFileName().toString();
    Path dest = dir.resolve(safe);
    try {
      if (Files.exists(dest) && Files.size(dest) > 0) {
        return safe;
      }
      try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpath)) {
        if (in == null) {
          log.warn("シード写真が見つかりません {}", classpath);
          return null;
        }
        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
      }
      return safe;
    } catch (IOException ex) {
      log.warn("シード写真を置けません {}", destName, ex);
      return null;
    }
  }

  @Override
  public void delete(String filename) {
    if (filename == null || filename.isBlank()) {
      return;
    }
    Path file = dir.resolve(Path.of(filename).getFileName().toString());
    try {
      Files.deleteIfExists(file);
    } catch (IOException ignored) {
      // 本文の削除は進める
    }
  }
}
