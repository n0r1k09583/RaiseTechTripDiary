package com.raisetech.tripdiary.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  public static final String BEARER = "bearer-jwt";

  @Bean
  public OpenAPI timelineOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("TripDiary 旅の記録 API")
                .version("1.0")
                .description(
                    """
                    この仕様書は **コードから自動生成** される。正はコントローラと DTO である。

                    別チーム（画面担当）が合わせるインターフェース:
                    - パスと HTTP 方法
                    - リクエスト（JSON のフィールド、multipart の `body` / `image`）
                    - レスポンスの形
                    - エラー時の挙動（HTTP ステータスと `{ status, code, error }`）

                    コントローラを直して **再起動** すると `/v3/api-docs` と Swagger UI が更新される。
                    Word や Markdown の仕様書を二重管理しない。

                    要ログインの API は Authorize に `accessToken` を入れる（Bearer）。
                    `/v3/api-docs` と `/swagger-ui.html` は `/api/**` の外なのでログイン不要。
                    """))
        .servers(
            List.of(
                new Server()
                    .url("http://127.0.0.1:8080")
                    .description("ローカル（本実装）")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("POST /api/login の accessToken。Authorization: Bearer {token}")));
  }
}
