package com.raisetech.tripdiary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.raisetech.tripdiary.web.ApiException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class ImageStorageTest {

  @TempDir
  Path dir;

  ImageStorage storage;

  @BeforeEach
  void setUp() {
    storage = new ImageStorage(dir.toString());
    storage.init();
  }

  @Test
  void 空ファイルは保存しない() {
    MockMultipartFile empty = new MockMultipartFile("image", "x.jpg", "image/jpeg", new byte[0]);
    assertThat(storage.save(empty)).isNull();
    assertThat(storage.save(null)).isNull();
  }

  @Test
  void ContentType空は拒否する() {
    MockMultipartFile file = new MockMultipartFile("image", "x.jpg", "", new byte[] {1, 2, 3});
    assertThatThrownBy(() -> storage.save(file))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getMessage())
        .isEqualTo("JPEG / PNG / WebP のみです");
  }

  @Test
  void 大文字JPEGと5MBちょうどは保存できる() {
    MockMultipartFile jpeg =
        new MockMultipartFile("image", "ok.jpg", "IMAGE/JPEG", new byte[5 * 1024 * 1024]);
    String name = storage.save(jpeg);
    assertThat(name).endsWith(".jpg");
    assertThat(dir.resolve(name)).exists();
  }

  @Test
  void PNGとWebPは保存できる() {
    assertThat(
            storage.save(new MockMultipartFile("image", "a.png", "image/png", new byte[] {1})))
        .endsWith(".png");
    assertThat(
            storage.save(new MockMultipartFile("image", "a.webp", "image/webp", new byte[] {1})))
        .endsWith(".webp");
  }

  @Test
  void 空ファイル名の削除は何もしない() {
    storage.delete(null);
    storage.delete("  ");
  }

  @Test
  void GIFは拒否する() {
    MockMultipartFile gif = new MockMultipartFile("image", "x.gif", "image/gif", new byte[] {1, 2, 3});
    assertThatThrownBy(() -> storage.save(gif))
        .isInstanceOf(ApiException.class)
        .satisfies(
            ex -> {
              ApiException api = (ApiException) ex;
              assertThat(api.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(api.getMessage()).isEqualTo("JPEG / PNG / WebP のみです");
            });
  }

  @Test
  void 容量5MB超は拒否する() {
    byte[] tooBig = new byte[5 * 1024 * 1024 + 1];
    MockMultipartFile file = new MockMultipartFile("image", "x.jpg", "image/jpeg", tooBig);
    assertThatThrownBy(() -> storage.save(file))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getMessage())
        .isEqualTo("画像は5MBまでです");
  }

  @Test
  void 削除はアップロード先の外に出ない() throws Exception {
    MockMultipartFile jpeg =
        new MockMultipartFile("image", "ok.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8});
    String name = storage.save(jpeg);
    assertThat(name).endsWith(".jpg");
    assertThat(dir.resolve(name)).exists();

    Path outside = dir.getParent().resolve("keep-outside.txt");
    Files.writeString(outside, "keep");
    storage.delete("../keep-outside.txt");
    assertThat(outside).exists();
    assertThat(dir.resolve("keep-outside.txt")).doesNotExist();

    storage.delete(name);
    assertThat(dir.resolve(name)).doesNotExist();
  }
}
