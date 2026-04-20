package com.brewmaster.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderList, UUID> {

    @Query("SELECT o FROM OrderList o JOIN FETCH o.recipe WHERE o.createdBy.id = :userId ORDER BY o.createdAt DESC")
    Page<OrderList> findByCreatedByIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);
}
