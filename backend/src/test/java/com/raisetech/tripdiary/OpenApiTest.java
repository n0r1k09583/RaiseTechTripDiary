package com.raisetech.tripdiary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTest {

  @Autowired
  MockMvc mockMvc;

  @Test
  void openApiDocumentIncludesLoginPostsCommentsAndErrors() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists())
        .andExpect(jsonPath("$.info.title").value("TripDiary 旅の記録 API"))
        .andExpect(jsonPath("$.paths['/api/login']").exists())
        .andExpect(jsonPath("$.paths['/api/login'].post.responses['401']").exists())
        .andExpect(jsonPath("$.paths['/api/posts']").exists())
        .andExpect(jsonPath("$.paths['/api/posts'].get.parameters[?(@.name=='tab')]").exists())
        .andExpect(jsonPath("$.paths['/api/posts'].post.requestBody").exists())
        .andExpect(jsonPath("$.paths['/api/posts/{postId}/comments']").exists())
        .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt']").exists())
        .andExpect(jsonPath("$.components.schemas.ErrorResponse").exists())
        .andExpect(jsonPath("$.components.schemas.PostListResponse").exists())
        .andExpect(jsonPath("$.components.schemas.LoginRequest").exists());
  }

  @Test
  void swaggerUiIsPublic() throws Exception {
    mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
  }
}
