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
class CommentApiTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void commentsRequireLogin() throws Exception {
    mockMvc.perform(get("/api/posts/1/comments"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createListDeleteAndUpdateCount() throws Exception {
    String yamada = token("yamada@example.com");
    String hanako = token("hanako@example.com");
    long postId = createPost(yamada, "コメント用の投稿");

    mockMvc.perform(post("/api/posts/" + postId + "/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"body\":\"最初のコメントです\"}")
            .header("Authorization", "Bearer " + hanako))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.body").value("最初のコメントです"))
        .andExpect(jsonPath("$.username").value("hanako"))
        .andExpect(jsonPath("$.mine").value(true));

    mockMvc.perform(post("/api/posts/" + postId + "/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"body\":\"あとのコメントです\"}")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isCreated());

    MvcResult listed = mockMvc.perform(get("/api/posts/" + postId + "/comments")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.comments.length()").value(2))
        .andExpect(jsonPath("$.comments[0].body").value("最初のコメントです"))
        .andExpect(jsonPath("$.comments[1].body").value("あとのコメントです"))
        .andExpect(jsonPath("$.comments[0].mine").value(false))
        .andReturn();

    mockMvc.perform(get("/api/posts/" + postId).header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.commentCount").value(2));

    long hanakoCommentId = objectMapper.readTree(listed.getResponse().getContentAsString())
        .get("comments").get(0).get("id").asLong();

    mockMvc.perform(delete("/api/comments/" + hanakoCommentId)
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("自分のコメントだけ削除できます"));

    mockMvc.perform(delete("/api/comments/" + hanakoCommentId)
            .header("Authorization", "Bearer " + hanako))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/posts/" + postId).header("Authorization", "Bearer " + yamada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.commentCount").value(1));
  }

  @Test
  void rejectsEmptyCommentAndMissingPost() throws Exception {
    String yamada = token("yamada@example.com");
    long postId = createPost(yamada, "空コメント拒否");

    mockMvc.perform(post("/api/posts/" + postId + "/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"body\":\"   \"}")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("コメントは1〜140文字です"));

    mockMvc.perform(get("/api/posts/999999/comments")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("投稿が見つかりません"));
  }

  @Test
  void rejectsTooLongComment() throws Exception {
    String yamada = token("yamada@example.com");
    long postId = createPost(yamada, "長いコメント拒否");
    mockMvc.perform(post("/api/posts/" + postId + "/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"body\":\"" + "あ".repeat(141) + "\"}")
            .header("Authorization", "Bearer " + yamada))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("コメントは1〜140文字です"));
  }

  private long createPost(String token, String body) throws Exception {
    MvcResult created = mockMvc.perform(multipart("/api/posts")
            .param("spotName", "東京駅").param("areaTag", "関東").param("body", body)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
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
