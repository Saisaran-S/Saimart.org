package saimart.controller;

import saimart.entity.Product;
import saimart.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatbotController {

    private final ProductRepository productRepository;

    public ChatbotController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "").trim().toLowerCase();
        Map<String, Object> response = new HashMap<>();

        if (message.isEmpty()) {
            response.put("reply", "Hello! I am SaiBot, your Saimart shopping assistant. How can I help you today?");
            response.put("suggestions", List.of("Show Electronics", "Find Shoes", "Track My Order", "How to Sell?"));
            return response;
        }

        // 1. Order tracking & Status
        if (message.contains("track") || message.contains("order status") || message.contains("where is my order") || message.contains("delivery status")) {
            response.put("reply", "You can monitor your order in real-time from the 'My Orders' page. Each order updates across 5 live milestones: Order Placed → Confirmed → Shipped → Out for Delivery → Delivered.");
            response.put("actionUrl", "/orders");
            response.put("actionLabel", "Go to My Orders 📦");
            response.put("suggestions", List.of("View My Orders", "Cancel an order", "Browse Products"));
            return response;
        }

        // 2. Cancellation & Returns
        if (message.contains("cancel") || message.contains("return") || message.contains("refund")) {
            response.put("reply", "You can cancel any order directly from the 'My Orders' page as long as it has not yet been delivered. Stock will automatically be restored.");
            response.put("actionUrl", "/orders");
            response.put("actionLabel", "View My Orders 📦");
            response.put("suggestions", List.of("Go to Orders", "Payment methods", "Contact support"));
            return response;
        }

        // 3. Payment & Delivery methods
        if (message.contains("payment") || message.contains("cod") || message.contains("cash on delivery") || message.contains("pay") || message.contains("card")) {
            response.put("reply", "Saimart supports Cash on Delivery (COD) as well as UPI and card payments during checkout. Delivery is standard and free on eligible orders!");
            response.put("suggestions", List.of("Go to Checkout", "View Cart", "Browse Products"));
            return response;
        }

        // 4. Seller / How to sell
        if (message.contains("sell") || message.contains("seller") || message.contains("vendor") || message.contains("add product")) {
            response.put("reply", "Want to sell on Saimart? Simply register a 'Seller' account at signup or log in to access your Seller Dashboard (/seller-dashboard) where you can add products, track stock, and manage customer orders.");
            response.put("actionUrl", "/seller-dashboard");
            response.put("actionLabel", "Seller Dashboard 🏪");
            response.put("suggestions", List.of("Seller Dashboard", "Add Product", "Login"));
            return response;
        }

        // 5. Cart help
        if (message.contains("cart") || message.contains("checkout") || message.contains("buy")) {
            response.put("reply", "You can add items to your cart by clicking 'Add to Cart' on any product card or details page. Once ready, click the Cart icon at the top right to proceed to Checkout!");
            response.put("actionUrl", "/cart");
            response.put("actionLabel", "Open Cart 🛒");
            response.put("suggestions", List.of("View Cart", "Featured Products", "Payment info"));
            return response;
        }

        // 6. Price query: "under 5000", "under 10000", etc.
        Pattern pricePattern = Pattern.compile("under\\s+(\\d+)");
        Matcher matcher = pricePattern.matcher(message);
        if (matcher.find()) {
            try {
                double maxPrice = Double.parseDouble(matcher.group(1));
                List<Product> matches = productRepository.findAll().stream()
                        .filter(p -> p.getPrice() <= maxPrice)
                        .limit(4)
                        .collect(Collectors.toList());

                if (!matches.isEmpty()) {
                    response.put("reply", "Here are great products under ₹" + (int)maxPrice + ":");
                    response.put("products", matches);
                    response.put("suggestions", List.of("View all products", "Electronics", "Fashion"));
                    return response;
                } else {
                    response.put("reply", "I couldn't find any products under ₹" + (int)maxPrice + ". Try a slightly higher budget!");
                    response.put("suggestions", List.of("Products under 5000", "Products under 20000", "All Products"));
                    return response;
                }
            } catch (Exception ignored) {}
        }

        // 7. Search products dynamically in database
        List<Product> allProducts = productRepository.findAll();
        List<Product> matchedProducts = new ArrayList<>();

        // Stopwords to ignore
        Set<String> stopwords = Set.of("the", "a", "an", "is", "for", "in", "of", "and", "show", "me", "find", "looking", "want", "product", "products", "item", "items", "do", "you", "have", "any");
        String[] words = message.split("\\s+");

        for (String word : words) {
            String w = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (w.length() >= 3 && !stopwords.contains(w)) {
                for (Product p : allProducts) {
                    boolean nameMatch = p.getName() != null && p.getName().toLowerCase().contains(w);
                    boolean descMatch = p.getDescription() != null && p.getDescription().toLowerCase().contains(w);
                    boolean catMatch = p.getCategory() != null && p.getCategory().toLowerCase().contains(w);

                    if ((nameMatch || descMatch || catMatch) && !matchedProducts.contains(p)) {
                        matchedProducts.add(p);
                    }
                }
            }
        }

        if (!matchedProducts.isEmpty()) {
            List<Product> topMatches = matchedProducts.stream().limit(4).collect(Collectors.toList());
            response.put("reply", "I found " + matchedProducts.size() + " matching item(s) on Saimart for you:");
            response.put("products", topMatches);
            response.put("actionUrl", "/products?search=" + words[0]);
            response.put("actionLabel", "Browse All Results →");
            response.put("suggestions", List.of("View Cart", "Track Order", "Electronics", "Fashion"));
            return response;
        }

        // 8. Categories
        if (message.contains("electronics") || message.contains("phone") || message.contains("gadget")) {
            List<Product> el = allProducts.stream().filter(p -> "Electronics".equalsIgnoreCase(p.getCategory())).limit(4).toList();
            response.put("reply", "Here are top products from our Electronics category:");
            response.put("products", el);
            response.put("actionUrl", "/products?category=Electronics");
            response.put("actionLabel", "View Electronics →");
            response.put("suggestions", List.of("Fashion", "Gaming", "Home", "Track Order"));
            return response;
        }

        if (message.contains("fashion") || message.contains("cloth") || message.contains("shoe") || message.contains("bag")) {
            List<Product> fash = allProducts.stream().filter(p -> "Fashion".equalsIgnoreCase(p.getCategory())).limit(4).toList();
            response.put("reply", "Explore our Fashion collection:");
            response.put("products", fash);
            response.put("actionUrl", "/products?category=Fashion");
            response.put("actionLabel", "View Fashion →");
            response.put("suggestions", List.of("Electronics", "Gaming", "Home"));
            return response;
        }

        // Fallback default response
        response.put("reply", "I'm SaiBot, your AI shopping assistant. You can ask me to find products (e.g. 'find headphones' or 'shoes'), check prices ('products under 5000'), get order tracking help, or learn about selling on Saimart!");
        response.put("suggestions", List.of("Find Headphones", "Shop Electronics", "Track My Order", "Payment Methods", "How to Sell?"));
        return response;
    }
}
