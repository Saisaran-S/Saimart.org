package saimart.controller;

import saimart.entity.Order;
import saimart.entity.OrderItem;
import saimart.entity.Product;
import saimart.repository.OrderRepository;
import saimart.repository.OrderItemRepository;
import saimart.repository.ProductRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
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
    public Order placeOrder(
            @RequestBody Order order) {

        // Check stock before saving the order
        if (order.getItems() != null) {

            for (OrderItem item : order.getItems()) {

                Product product =
                        productRepository.findById(item.getProductId())
                                .orElseThrow(
                                        () -> new RuntimeException(
                                                "Product not found: "
                                                        + item.getProductId()
                                        )
                                );

                if (product.getStock() < item.getQuantity()) {

                    throw new RuntimeException(
                            "Not enough stock for product: "
                                    + product.getName()
                    );
                }
            }
        }

        // Save main order
        Order savedOrder =
                orderRepository.save(order);

        // Save items and reduce stock
        if (order.getItems() != null) {

            for (OrderItem item : order.getItems()) {

                item.setOrderId(savedOrder.getId());

                orderItemRepository.save(item);

                // Reduce product stock
                Product product =
                        productRepository.findById(item.getProductId())
                                .orElseThrow(
                                        () -> new RuntimeException(
                                                "Product not found"
                                        )
                                );

                product.setStock(
                        product.getStock() -
                        item.getQuantity()
                );

                productRepository.save(product);
            }
        }

        // Return saved order
        savedOrder.setItems(order.getItems());

        return savedOrder;
    }

    // GET ALL ORDERS
    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // GET BUYER'S ORDERS
    @GetMapping("/buyer/{buyerId}")
    public List<Order> getBuyerOrders(
            @PathVariable Long buyerId) {

        List<Order> orders =
                orderRepository.findByBuyerId(buyerId);

        for (Order order : orders) {

            List<OrderItem> items =
                    orderItemRepository
                            .findByOrderId(order.getId());

            order.setItems(items);
        }

        return orders;
    }

    // GET ONE ORDER
    @GetMapping("/{id}")
    public Order getOrder(
            @PathVariable Long id) {

        Order order =
                orderRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Order not found"
                                )
                        );

        List<OrderItem> items =
                orderItemRepository.findByOrderId(id);

        order.setItems(items);

        return order;
    }

    // GET SELLER'S ORDER ITEMS
    @GetMapping("/seller/{sellerId}")
    public List<OrderItem> getSellerOrders(
            @PathVariable Long sellerId) {

        return orderItemRepository
                .findBySellerId(sellerId);
    }
}