package com.raisetech.tripdiary.service;

import java.util.List;

public final class Areas {

  public static final List<String> ALL =
      List.of("北海道", "東北", "関東", "中部", "近畿", "中国", "四国", "九州", "沖縄", "海外");

  private Areas() {}

  public static boolean contains(String area) {
    return area != null && ALL.contains(area);
  }
}
