package com.raisetech.tripdiary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class SocialApiTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void いいねはトグルで件数と自分が変わる() throws Exception {
    String yamada = token("yamada@example.com");
    MvcResult created = mockMvc.perform(multipart("/api/posts")
            .param("spotName", "東京駅").param("areaTag", "関東").param("body", "いいね用の投稿")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.likedByMe").value(false))
        .andReturn();
    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post("/api/posts/" + id + "/likes").header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.likeCount").value(1))
        .andExpect(jsonPath("$.likedByMe").value(true));

    mockMvc.perform(post("/api/posts/" + id + "/likes").header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.likeCount").value(0))
        .andExpect(jsonPath("$.likedByMe").value(false));
  }

  @Test
  void 花子は山田をフォローでき自分は不可() throws Exception {
    String hanako = token("hanako@example.com");
    String yamada = token("yamada@example.com");

    mockMvc.perform(post("/api/users/yamada/follow").header("Authorization", "Bearer " + yamada))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("自分自身はフォローできません"));

    mockMvc.perform(post("/api/users/yamada/follow").header("Authorization", "Bearer " + hanako))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.followedByMe").value(true))
        .andExpect(jsonPath("$.followerCount").value(1));

    mockMvc.perform(get("/api/posts").param("tab", "following")
            .header("Authorization", "Bearer " + hanako))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posts[0].username").exists());

    mockMvc.perform(get("/api/users/yamada/followers").header("Authorization", "Bearer " + hanako))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users[0].username").value("hanako"));

    mockMvc.perform(delete("/api/users/yamada/follow").header("Authorization", "Bearer " + hanako))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.followedByMe").value(false));
  }

  @Test
  void ユーザー検索は部分一致で空は叩かない() throws Exception {
    String yamada = token("yamada@example.com");
    mockMvc.perform(get("/api/users").param("q", "YAMA")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users[0].username").value("yamada"))
        .andExpect(jsonPath("$.users[0].mine").value(true));

    mockMvc.perform(get("/api/users").param("q", "@YAMA")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users[0].username").value("yamada"));

    mockMvc.perform(get("/api/users").param("q", "山田")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users[0].username").value("yamada"));

    mockMvc.perform(get("/api/users").param("q", "やまだ")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users[0].username").value("yamada"));

    mockMvc.perform(get("/api/users").param("q", "ヤマダ")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users[0].username").value("yamada"));

    mockMvc.perform(get("/api/users").param("q", "はなこ")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users[0].username").value("hanako"));

    mockMvc.perform(get("/api/users").param("q", "ハナコ")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users[0].username").value("hanako"));

    mockMvc.perform(get("/api/users").param("q", "   ")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users").isEmpty());

    mockMvc.perform(get("/api/users/nobody").header("Authorization", "Bearer " + yamada))
        .andExpect(status().isNotFound());
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
