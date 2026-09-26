package com.raisetech.tripdiary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class RecordAccessApiTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void 非公開は本人だけが見られて公開記録だけいいねとコメントとお気に入りできる() throws Exception {
    String yamada = token("yamada@example.com");
    String hanako = token("hanako@example.com");

    MvcResult created = mockMvc.perform(multipart("/api/posts")
            .param("spotName", "秘密の浜")
            .param("areaTag", "沖縄")
            .param("body", "本人だけの記録")
            .param("visibility", "private")
            .param("latitude", "26.5")
            .param("longitude", "127.8")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.visibility").value("private"))
        .andExpect(jsonPath("$.latitude").value(26.5))
        .andReturn();
    long privateId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(get("/api/posts").param("tab", "all").header("Authorization", "Bearer " + hanako))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posts[?(@.id == " + privateId + ")]").isEmpty());

    mockMvc.perform(get("/api/posts/" + privateId).header("Authorization", "Bearer " + hanako))
        .andExpect(status().isNotFound());

    mockMvc.perform(post("/api/posts/" + privateId + "/likes").header("Authorization", "Bearer " + yamada))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/posts").param("tab", "visited").header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posts[?(@.id == " + privateId + ")]").isNotEmpty());

    MvcResult publicPost = mockMvc.perform(multipart("/api/posts")
            .param("spotName", "公開岬")
            .param("areaTag", "北海道")
            .param("body", "みんなの記録")
            .param("visibility", "public")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isCreated())
        .andReturn();
    long publicId = objectMapper.readTree(publicPost.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post("/api/posts/" + publicId + "/likes").header("Authorization", "Bearer " + hanako))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.likedByMe").value(true));

    mockMvc.perform(post("/api/posts/" + publicId + "/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"body\":\"行き方を教えてください\"}")
            .header("Authorization", "Bearer " + hanako))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/posts/" + publicId + "/favorites").header("Authorization", "Bearer " + hanako))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.favoritedByMe").value(true));

    mockMvc.perform(get("/api/posts").param("tab", "favorites").header("Authorization", "Bearer " + hanako))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posts[?(@.id == " + publicId + ")]").isNotEmpty());

    mockMvc.perform(get("/api/posts").param("tab", "favorites").header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posts[?(@.id == " + publicId + ")]").isEmpty());
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
