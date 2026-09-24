package com.raisetech.tripdiary.config;

import com.raisetech.tripdiary.web.AuthInterceptor;
import java.nio.file.Path;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final AuthInterceptor authInterceptor;
  private final String[] origins;
  private final Path uploadDir;
  private final boolean localUploads;

  public WebMvcConfig(
      AuthInterceptor authInterceptor,
      @Value("${cors.origins:http://localhost:5173,http://127.0.0.1:5173}") String origins,
      @Value("${app.upload-dir:./uploads}") String uploadDir,
      @Value("${app.storage:local}") String storage) {
    this.authInterceptor = authInterceptor;
    this.origins = Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new);
    this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    this.localUploads = !"s3".equals(storage);
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*");
    registry.addMapping("/uploads/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "OPTIONS")
        .allowedHeaders("*");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    if (!localUploads) {
      return;
    }
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations(uploadDir.toUri().toString());
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns("/api/signup", "/api/login", "/api/refresh", "/api/logout", "/api/health");
  }
}
