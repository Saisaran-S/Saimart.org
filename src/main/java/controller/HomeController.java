package saimart.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/add-product")
    public String addProduct() {
        return "add-product";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
public String signup() {
    return "signup";
}
@GetMapping("/products")
public String products() {
    return "products";
}
@GetMapping("/add-demo-products")
public String addDemoProducts() {
    return "add-demo-products";
}
@GetMapping("/product-details")
public String productDetails() {
    return "product-details";
}
@GetMapping("/cart")
public String cart() {
    return "cart";
}
@GetMapping("/checkout")
public String checkout() {
    return "checkout";
}
@GetMapping("/order-success")
public String orderSuccess() {
    return "order-success";
}
@GetMapping("/orders")
public String orders() {
    return "orders";
}
}