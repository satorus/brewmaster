package com.brewmaster.order;

import com.brewmaster.ai.OrderAiService;
import com.brewmaster.order.dto.AiOrderResponse;
import com.brewmaster.order.dto.GenerateOrderRequest;
import com.brewmaster.order.dto.OrderResultDto;
import com.brewmaster.order.dto.OrderSummaryDto;
import com.brewmaster.recipe.Recipe;
import com.brewmaster.recipe.RecipeRepository;
import com.brewmaster.user.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderAiService orderAiService;
    private final RecipeRepository recipeRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OrderAiService orderAiService,
                        RecipeRepository recipeRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderAiService = orderAiService;
        this.recipeRepository = recipeRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResultDto generateOrder(GenerateOrderRequest req, User user) {
        Recipe recipe = recipeRepository.findById(req.recipeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        AiOrderResponse aiResponse = orderAiService.generateOrderList(req.recipeId(), req.volumeL());

        String aiResultJson;
        try {
            aiResultJson = objectMapper.writeValueAsString(aiResponse);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize AI response");
        }

        OrderList orderList = new OrderList(
                recipe, req.volumeL(), aiResultJson,
                BigDecimal.valueOf(aiResponse.estimatedTotalMin()),
                BigDecimal.valueOf(aiResponse.estimatedTotalMax()),
                user);

        OrderList saved = orderRepository.save(orderList);

        return toResultDto(saved, aiResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> listOrders(User user, Pageable pageable) {
        return orderRepository.findByCreatedByIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public OrderResultDto getOrder(UUID id, User user) {
        OrderList order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getCreatedBy().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        AiOrderResponse aiResponse;
        try {
            aiResponse = objectMapper.readValue(order.getAiResult(), AiOrderResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to deserialize order data");
        }

        return toResultDto(order, aiResponse);
    }

    private OrderResultDto toResultDto(OrderList order, AiOrderResponse aiResponse) {
        return new OrderResultDto(
                order.getId(),
                order.getRecipe().getId(),
                order.getRecipe().getName(),
                order.getVolumeL(),
                aiResponse.items(),
                aiResponse.estimatedTotalMin(),
                aiResponse.estimatedTotalMax(),
                order.getCreatedAt().toString(),
                aiResponse.disclaimer());
    }

    private OrderSummaryDto toSummaryDto(OrderList order) {
        return new OrderSummaryDto(
                order.getId(),
                order.getRecipe().getName(),
                order.getVolumeL(),
                order.getEstimatedTotalMin() != null ? order.getEstimatedTotalMin().doubleValue() : 0.0,
                order.getEstimatedTotalMax() != null ? order.getEstimatedTotalMax().doubleValue() : 0.0,
                order.getCreatedAt().toString());
    }
}
