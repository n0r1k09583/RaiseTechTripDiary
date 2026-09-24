package com.raisetech.tripdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "起動確認。ok が true なら API は生きている。")
public class HealthResponse {

  private boolean ok = true;

  public boolean isOk() {
    return ok;
  }
}
