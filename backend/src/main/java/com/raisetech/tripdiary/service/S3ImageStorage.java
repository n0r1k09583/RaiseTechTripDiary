package com.raisetech.tripdiary.service;

import com.raisetech.tripdiary.web.ApiException;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@ConditionalOnProperty(name = "app.storage", havingValue = "s3")
public class S3ImageStorage implements ImageStore {

  private static final Logger log = LoggerFactory.getLogger(S3ImageStorage.class);

  private final S3Client s3;
  private final String bucket;

  public S3ImageStorage(
      @Value("${app.image-bucket}") String bucket,
      @Value("${app.image-region:ap-northeast-1}") String region) {
    this(S3Client.builder().region(Region.of(region)).build(), bucket);
  }

  S3ImageStorage(S3Client s3, String bucket) {
    this.s3 = s3;
    this.bucket = bucket;
  }

  @Override
  public String save(MultipartFile file) {
    String name = ImageRules.newFilename(file, log);
    if (name == null) {
      return null;
    }
    try {
      s3.putObject(
          PutObjectRequest.builder()
              .bucket(bucket)
              .key("uploads/" + name)
              .contentType(file.getContentType())
              .build(),
          RequestBody.fromBytes(file.getBytes()));
    } catch (IOException ex) {
      log.error("画像の保存に失敗しました", ex);
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "画像の保存に失敗しました");
    }
    return name;
  }

  @Override
  public void delete(String filename) {
    if (filename == null || filename.isBlank()) {
      return;
    }
    String key = "uploads/" + Path.of(filename).getFileName();
    s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
  }
}
