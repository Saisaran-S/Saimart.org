package saimart.controller;

import saimart.entity.Order;
import saimart.entity.OrderItem;
import saimart.entity.Product;
import saimart.repository.OrderRepository;
import saimart.repository.OrderItemRepository;
import saimart.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderController(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    // PLACE ORDER
    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Order must contain at least one item."));
        }

        // Validate stock and items before saving the order
        for (OrderItem item : order.getItems()) {
            if (item.getQuantity() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Quantity must be greater than zero."));
            }

            Optional<Product> productOpt = productRepository.findById(item.getProductId());
            if (productOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Product not found: ID " + item.getProductId()));
            }

            Product product = productOpt.get();
            if (product.getStock() < item.getQuantity()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Insufficient stock for \"" + product.getName() + "\". Only " + product.getStock() + " available."
                ));
            }
        }

        // Set default status and date if missing
        if (order.getStatus() == null || order.getStatus().trim().isEmpty()) {
            order.setStatus("PLACED");
        }
        if (order.getOrderDate() == null || order.getOrderDate().trim().isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
            order.setOrderDate(LocalDateTime.now().format(formatter));
        }

        // Save main order
        Order savedOrder = orderRepository.save(order);

        // Save items and reduce stock
        for (OrderItem item : order.getItems()) {
            item.setOrderId(savedOrder.getId());

            Product product = productRepository.findById(item.getProductId()).orElseThrow();
            // Safeguard: Ensure sellerId is set from the product if client missed it
            if (item.getSellerId() == null || item.getSellerId() <= 0) {
                item.setSellerId(product.getSellerId());
            }
            if (item.getProductName() == null || item.getProductName().trim().isEmpty()) {
                item.setProductName(product.getName());
            }

            orderItemRepository.save(item);

            // Deduct stock
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }

        savedOrder.setItems(order.getItems());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedOrder);
    }

    // GET ALL ORDERS
    @GetMapping
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        for (Order order : orders) {
            order.setItems(orderItemRepository.findByOrderId(order.getId()));
        }
        return orders;
    }

    // GET BUYER'S ORDERS
    @GetMapping("/buyer/{buyerId}")
    public List<Order> getBuyerOrders(@PathVariable Long buyerId) {
        List<Order> orders = orderRepository.findByBuyerId(buyerId);
        // Order by latest first
        orders.sort((a, b) -> Long.compare(b.getId(), a.getId()));

        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            order.setItems(items);
        }
        return orders;
    }

    // GET ONE ORDER
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Order not found with ID: " + id));
        }

        Order order = orderOpt.get();
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        order.setItems(items);
        return ResponseEntity.ok(order);
    }

    // UPDATE ORDER STATUS (e.g. by Seller or Admin)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String newStatus = body.get("status");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status cannot be empty."));
        }

        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Order not found."));
        }

        Order order = orderOpt.get();
        order.setStatus(newStatus.toUpperCase().trim());
        orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        order.setItems(items);
        return ResponseEntity.ok(order);
    }

    // CANCEL ORDER (by Buyer or Seller)
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Order not found."));
        }

        Order order = orderOpt.get();
        String currentStatus = order.getStatus() != null ? order.getStatus() : "PLACED";

        if ("DELIVERED".equalsIgnoreCase(currentStatus)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot cancel an order that has already been delivered."));
        }
        if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Order is already cancelled."));
        }

        // Restore stock
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        for (OrderItem item : items) {
            Optional<Product> prodOpt = productRepository.findById(item.getProductId());
            if (prodOpt.isPresent()) {
                Product product = prodOpt.get();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);
        order.setItems(items);

        return ResponseEntity.ok(Map.of("message", "Order cancelled successfully.", "order", order));
    }

    // GET SELLER'S ORDER ITEMS
    @GetMapping("/seller/{sellerId}")
    public List<OrderItem> getSellerOrders(@PathVariable Long sellerId) {
        List<OrderItem> items = orderItemRepository.findBySellerId(sellerId);
        if (items.isEmpty() || Long.valueOf(1L).equals(sellerId)) {
            List<OrderItem> all = orderItemRepository.findAll();
            List<OrderItem> matched = all.stream()
                    .filter(i -> i.getSellerId() == null || i.getSellerId().equals(sellerId))
                    .collect(Collectors.toList());
            if (!matched.isEmpty()) return matched;
        }
        return items;
    }

    // GET SELLER'S ORDERS WITH FULL ORDER CONTEXT (Fast 1-call API for seller-orders)
    @GetMapping("/seller/{sellerId}/detailed")
    public List<Map<String, Object>> getSellerOrdersDetailed(@PathVariable Long sellerId) {
        List<OrderItem> items = getSellerOrders(sellerId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (OrderItem item : items) {
            Map<String, Object> map = new HashMap<>();
            map.put("item", item);

            if (item.getOrderId() != null) {
                Optional<Order> orderOpt = orderRepository.findById(item.getOrderId());
                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    map.put("orderId", order.getId());
                    map.put("orderDate", order.getOrderDate());
                    map.put("orderStatus", order.getStatus());
                    map.put("customerName", order.getFullName());
                    map.put("customerPhone", order.getPhone());
                    map.put("customerAddress", order.getAddress());
                    map.put("customerCity", order.getCity());
                    map.put("customerPincode", order.getPincode());
                    map.put("paymentMethod", order.getPaymentMethod());
                }
            }
            result.add(map);
        }

        // Reverse to show latest first
        Collections.reverse(result);
        return result;
    }
}