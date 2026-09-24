package com.raisetech.tripdiary;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PostApiTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void 投稿APIはログイン必須() throws Exception {
    mockMvc.perform(get("/api/posts"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void 自分の投稿は編集削除でき他人は403() throws Exception {
    String yamada = token("yamada@example.com");
    String hanako = token("hanako@example.com");

    MvcResult created = mockMvc.perform(multipart("/api/posts")
            .param("spotName", "東京駅").param("areaTag", "関東").param("body", "投稿の確認です")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.body").value("投稿の確認です"))
        .andExpect(jsonPath("$.username").value("yamada"))
        .andExpect(jsonPath("$.mine").value(true))
        .andExpect(jsonPath("$.commentCount").value(0))
        .andExpect(jsonPath("$.likeCount").value(0))
        .andReturn();

    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(get("/api/posts").header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posts[0].id").value(id))
        .andExpect(jsonPath("$.posts[0].body").value("投稿の確認です"));

    mockMvc.perform(get("/api/posts").param("tab", "following")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posts[*].username", everyItem(is("yamada"))));

    mockMvc.perform(multipart(HttpMethod.PATCH, "/api/posts/" + id)
            .param("spotName", "東京駅").param("areaTag", "関東").param("body", "本文を直した")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.body").value("本文を直した"));

    mockMvc.perform(multipart(HttpMethod.PATCH, "/api/posts/" + id)
            .param("spotName", "東京駅").param("areaTag", "関東").param("body", "他人は編集できない")
            .header("Authorization", "Bearer " + hanako))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("自分の投稿だけ編集できます"));

    mockMvc.perform(delete("/api/posts/" + id).header("Authorization", "Bearer " + hanako))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("自分の投稿だけ削除できます"));

    mockMvc.perform(delete("/api/posts/" + id).header("Authorization", "Bearer " + yamada))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/posts/" + id).header("Authorization", "Bearer " + yamada))
        .andExpect(status().isNotFound());
  }

  @Test
  void timelineLoadsOlderPostsWithoutAButton() throws Exception {
    String yamada = token("yamada@example.com");
    for (int i = 0; i < 18; i++) {
      mockMvc.perform(multipart("/api/posts")
              .param("spotName", "東京駅").param("areaTag", "関東").param("body", "無限スクロール用 " + i)
              .header("Authorization", "Bearer " + yamada))
          .andExpect(status().isCreated());
    }

    MvcResult firstPage = mockMvc.perform(get("/api/posts")
            .param("limit", "20")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posts.length()").value(20))
        .andExpect(jsonPath("$.hasMore").value(true))
        .andReturn();

    JsonNode posts = objectMapper.readTree(firstPage.getResponse().getContentAsString()).get("posts");
    JsonNode last = posts.get(posts.size() - 1);

    mockMvc.perform(get("/api/posts")
            .param("limit", "20")
            .param("beforeCreatedAt", last.get("createdAt").asText())
            .param("beforeId", last.get("id").asText())
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasMore").value(false));

    JsonNode newest = posts.get(0);
    mockMvc.perform(multipart("/api/posts")
            .param("spotName", "東京駅").param("areaTag", "関東").param("body", "先頭にすぐ出す")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/posts")
            .param("afterCreatedAt", newest.get("createdAt").asText())
            .param("afterId", newest.get("id").asText())
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posts[0].body").value("先頭にすぐ出す"));
  }

  @Test
  void 空本文は投稿できない() throws Exception {
    String yamada = token("yamada@example.com");
    mockMvc.perform(multipart("/api/posts")
            .param("spotName", "東京駅").param("areaTag", "関東").param("body", "   ")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("本文は1〜280文字です"));
  }

  @Test
  void 非対応画像と5MB超は拒否する() throws Exception {
    String yamada = token("yamada@example.com");
    MockMultipartFile gif =
        new MockMultipartFile("image", "x.gif", "image/gif", new byte[] {1, 2, 3});
    mockMvc.perform(multipart("/api/posts")
            .file(gif)
            .param("spotName", "東京駅").param("areaTag", "関東").param("body", "画像の確認")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("JPEG / PNG / WebP のみです"));

    MockMultipartFile huge =
        new MockMultipartFile("image", "huge.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]);
    mockMvc.perform(multipart("/api/posts")
            .file(huge)
            .param("spotName", "東京駅").param("areaTag", "関東").param("body", "大きすぎる画像")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("画像は5MBまでです"));
  }

  @Test
  void missingPostIsNotFound() throws Exception {
    String yamada = token("yamada@example.com");
    mockMvc.perform(get("/api/posts/999999").header("Authorization", "Bearer " + yamada))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  private String token(String email) throws Exception {
    MvcResult login = mockMvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
  }
}
