package saimart.repository;

import saimart.entity.OrderItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findBySellerId(Long sellerId);

    List<OrderItem> findByOrderId(Long orderId);
}