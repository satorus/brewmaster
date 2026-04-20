package com.brewmaster.order;

import com.brewmaster.order.dto.GenerateOrderRequest;
import com.brewmaster.order.dto.OrderResultDto;
import com.brewmaster.order.dto.OrderSummaryDto;
import com.brewmaster.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResultDto generate(@Valid @RequestBody GenerateOrderRequest req,
                                   @AuthenticationPrincipal User user) {
        return orderService.generateOrder(req, user);
    }

    @GetMapping
    public Page<OrderSummaryDto> list(@AuthenticationPrincipal User user,
                                      @PageableDefault(size = 20, sort = "createdAt",
                                              direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.listOrders(user, pageable);
    }

    @GetMapping("/{id}")
    public OrderResultDto getById(@PathVariable UUID id,
                                  @AuthenticationPrincipal User user) {
        return orderService.getOrder(id, user);
    }
}
