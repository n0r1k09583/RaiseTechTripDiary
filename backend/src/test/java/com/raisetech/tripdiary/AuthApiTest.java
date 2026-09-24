package com.raisetech.tripdiary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
class AuthApiTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void healthIsOk() throws Exception {
    mockMvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
  }

  @Test
  void signupLoginAndMe() throws Exception {
    mockMvc.perform(post("/api/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "username": "newuser",
                  "displayName": "新規",
                  "email": "newuser@example.com",
                  "password": "password123",
                  "confirm": "password123"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.refreshToken").isString())
        .andExpect(jsonPath("$.token").isString())
        .andExpect(jsonPath("$.user.username").value("newuser"))
        .andExpect(jsonPath("$.user.password").doesNotExist())
        .andExpect(jsonPath("$.user.passwordDigest").doesNotExist());

    mockMvc.perform(post("/api/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "username": "newuser",
                  "displayName": "重複",
                  "email": "other@example.com",
                  "password": "password123",
                  "confirm": "password123"
                }
                """))
        .andExpect(status().isConflict());

    MvcResult login = mockMvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "yamada@example.com",
                  "password": "password123"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.refreshToken").isString())
        .andExpect(jsonPath("$.user.username").value("yamada"))
        .andExpect(jsonPath("$.user.password").doesNotExist())
        .andReturn();

    JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
    String accessToken = body.get("accessToken").asText();
    String refreshToken = body.get("refreshToken").asText();

    mockMvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "yamada@example.com",
                  "password": "wrong-password"
                }
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.error").value("メールアドレスまたはパスワードが違います"));

    mockMvc.perform(get("/api/me"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.username").value("yamada"))
        .andExpect(jsonPath("$.user.password").doesNotExist());

    mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + refreshToken))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void signupRejectsInvalidFields() throws Exception {
    mockMvc.perform(post("/api/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "username": "ab",
                  "displayName": "新規",
                  "email": "bad",
                  "password": "short",
                  "confirm": "nope"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION"))
        .andExpect(jsonPath("$.error").value("ユーザー名は3〜20文字の半角英小文字・数字・_です"));

    mockMvc.perform(post("/api/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "username": "valid_user",
                  "displayName": "新規",
                  "email": "valid_user@example.com",
                  "password": "password123",
                  "confirm": "different1"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("パスワードが一致しません"));
  }

  @Test
  void refreshRotatesTokenAndLogoutRevokes() throws Exception {
    MvcResult login = mockMvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "hanako@example.com",
                  "password": "password123"
                }
                """))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode first = objectMapper.readTree(login.getResponse().getContentAsString());
    String oldRefresh = first.get("refreshToken").asText();

    MvcResult refreshed = mockMvc.perform(post("/api/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + oldRefresh + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.refreshToken").isString())
        .andExpect(jsonPath("$.user.username").value("hanako"))
        .andReturn();

    JsonNode second = objectMapper.readTree(refreshed.getResponse().getContentAsString());
    String newAccess = second.get("accessToken").asText();
    String newRefresh = second.get("refreshToken").asText();

    mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + newAccess))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.username").value("hanako"));

    mockMvc.perform(post("/api/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + oldRefresh + "\"}"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post("/api/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + newRefresh + "\"}"))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + newRefresh + "\"}"))
        .andExpect(status().isUnauthorized());
  }
}
