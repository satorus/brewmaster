package com.brewmaster.brew;

import com.brewmaster.auth.dto.LoginRequest;
import com.brewmaster.auth.dto.RegisterRequest;
import com.brewmaster.brew.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BrewSessionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean BrewSessionService brewSessionService;

    private String token;
    private final UUID sessionId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin("brewer1", "brewer1@test.com", "password123");
    }

    @Test
    void startSession_returns201() throws Exception {
        when(brewSessionService.startSession(any(), any())).thenReturn(buildMockResponse());

        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void startSession_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startSession_returns400WhenMissingRecipeId() throws Exception {
        String body = "{\"targetVolumeL\":20}";

        mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSession_returns200() throws Exception {
        when(brewSessionService.getSession(eq(sessionId))).thenReturn(buildMockResponse());

        mockMvc.perform(get("/api/v1/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(0));
    }

    @Test
    void getSession_returns404WhenNotFound() throws Exception {
        when(brewSessionService.getSession(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Brew session not found"));

        mockMvc.perform(get("/api/v1/sessions/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void advanceStep_returns200() throws Exception {
        BrewSessionResponse advanced = buildMockResponseWithStep(1);
        when(brewSessionService.advanceStep(any(), any(), any())).thenReturn(advanced);

        String body = objectMapper.writeValueAsString(new AdvanceStepRequest(0, null, null));

        mockMvc.perform(put("/api/v1/sessions/" + sessionId + "/step")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(1));
    }

    @Test
    void advanceStep_returns409WhenNotInProgress() throws Exception {
        when(brewSessionService.advanceStep(any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Session is not in progress"));

        String body = objectMapper.writeValueAsString(new AdvanceStepRequest(0, null, null));

        mockMvc.perform(put("/api/v1/sessions/" + sessionId + "/step")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void completeSession_returns200() throws Exception {
        BrewSessionResponse completed = buildCompletedResponse();
        when(brewSessionService.completeSession(any(), any(), any())).thenReturn(completed);

        mockMvc.perform(put("/api/v1/sessions/" + sessionId + "/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void abandonSession_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void abandonSession_returns403WhenNotOwner() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the session owner can modify it"))
                .when(brewSessionService).abandonSession(any(), any());

        mockMvc.perform(delete("/api/v1/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void completeSession_returns403WhenNotOwner() throws Exception {
        when(brewSessionService.completeSession(any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the session owner can modify it"));

        mockMvc.perform(put("/api/v1/sessions/" + sessionId + "/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private String registerAndLogin(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(username, email, password, null))))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private StartSessionRequest buildValidRequest() {
        return new StartSessionRequest(recipeId, null, new BigDecimal("20"),
                new BigDecimal("10"), new BigDecimal("3.0"), null);
    }

    private BrewSessionResponse buildMockResponse() {
        return buildMockResponseWithStep(0);
    }

    private BrewSessionResponse buildMockResponseWithStep(int currentStep) {
        return new BrewSessionResponse(
                sessionId, recipeId, null, new BigDecimal("20"),
                currentStep, "IN_PROGRESS", null,
                new BigDecimal("9.00"), new BigDecimal("13.22"), new BigDecimal("22.22"),
                new BigDecimal("10"), new BigDecimal("3.0"),
                "2025-01-01T10:00:00Z", null,
                List.of(), List.of(), List.of());
    }

    private BrewSessionResponse buildCompletedResponse() {
        return new BrewSessionResponse(
                sessionId, recipeId, null, new BigDecimal("20"),
                3, "COMPLETED", null,
                new BigDecimal("9.00"), new BigDecimal("13.22"), new BigDecimal("22.22"),
                new BigDecimal("10"), new BigDecimal("3.0"),
                "2025-01-01T10:00:00Z", "2025-01-01T14:00:00Z",
                List.of(), List.of(), List.of());
    }
}
