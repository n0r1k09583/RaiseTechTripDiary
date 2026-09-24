package com.raisetech.tripdiary.service;

import com.raisetech.tripdiary.web.ApiException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

final class ImageRules {

  static final long MAX_BYTES = 5L * 1024 * 1024;
  private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");
  private static final Map<String, String> EXT = Map.of(
      "image/jpeg", ".jpg",
      "image/png", ".png",
      "image/webp", ".webp");

  private ImageRules() {}

  static String newFilename(MultipartFile file, Logger log) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    if (!TYPES.contains(type)) {
      log.warn("画像形式が対象外 type={}", type);
      throw new ApiException(HttpStatus.BAD_REQUEST, "JPEG / PNG / WebP のみです");
    }
    if (file.getSize() > MAX_BYTES) {
      log.warn("画像サイズ超過 size={}", file.getSize());
      throw new ApiException(HttpStatus.BAD_REQUEST, "画像は5MBまでです");
    }
    return UUID.randomUUID() + EXT.get(type);
  }
}
