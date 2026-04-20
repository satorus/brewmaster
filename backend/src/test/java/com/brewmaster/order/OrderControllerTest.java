package com.brewmaster.order;

import com.brewmaster.auth.dto.LoginRequest;
import com.brewmaster.auth.dto.RegisterRequest;
import com.brewmaster.order.dto.GenerateOrderRequest;
import com.brewmaster.order.dto.OrderResultDto;
import com.brewmaster.order.dto.OrderSummaryDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean OrderService orderService;

    private String token;
    private final UUID orderId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin("order_user", "order_user@test.com", "password123");
    }

    @Test
    void generate_returns201() throws Exception {
        when(orderService.generateOrder(any(), any())).thenReturn(buildMockResult());

        mockMvc.perform(post("/api/v1/orders/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.recipeName").value("Test IPA"));
    }

    @Test
    void generate_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/orders/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generate_returns400WhenMissingRecipeId() throws Exception {
        String body = "{\"volumeL\":20}";

        mockMvc.perform(post("/api/v1/orders/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generate_returns400WhenVolumeTooSmall() throws Exception {
        String body = objectMapper.writeValueAsString(
                new GenerateOrderRequest(recipeId, BigDecimal.ZERO));

        mockMvc.perform(post("/api/v1/orders/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generate_returns503WhenAiUnavailable() throws Exception {
        when(orderService.generateOrder(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Price search temporarily unavailable. Please try again."));

        mockMvc.perform(post("/api/v1/orders/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void list_returns200() throws Exception {
        OrderSummaryDto summary = new OrderSummaryDto(orderId, "Test IPA",
                new BigDecimal("20"), 45.0, 52.0, "2025-01-01T10:00:00Z");
        when(orderService.listOrders(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].recipeName").value("Test IPA"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_returns200() throws Exception {
        when(orderService.getOrder(eq(orderId), any())).thenReturn(buildMockResult());

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        when(orderService.getOrder(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_returns403WhenNotOwner() throws Exception {
        when(orderService.getOrder(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
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

    private GenerateOrderRequest buildValidRequest() {
        return new GenerateOrderRequest(recipeId, new BigDecimal("20"));
    }

    private OrderResultDto buildMockResult() {
        return new OrderResultDto(
                orderId, recipeId, "Test IPA", new BigDecimal("20"),
                List.of(), 45.0, 52.0,
                "2025-01-01T10:00:00Z",
                "Prices based on web search. May vary.");
    }
}
