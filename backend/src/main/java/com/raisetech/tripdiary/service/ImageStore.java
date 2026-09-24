package com.raisetech.tripdiary.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStore {

  String save(MultipartFile file);

  void delete(String filename);

  default String installClasspath(String classpath, String destName) {
    return null;
  }
}
