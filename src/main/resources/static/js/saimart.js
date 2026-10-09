/**
 * Saimart Shared Client Library
 * Handles authentication state, cart management, toast notifications,
 * and the intelligent Saimart AI Chatbot assistant.
 */

// ============================================================================
// 1. AUTHENTICATION & USER HELPERS
// ============================================================================

function getUser() {
    try {
        const data = localStorage.getItem("saimartUser");
        return data ? JSON.parse(data) : null;
    } catch (e) {
        return null;
    }
}

function setUser(user) {
    if (user) {
        localStorage.setItem("saimartUser", JSON.stringify(user));
    } else {
        localStorage.removeItem("saimartUser");
    }
}

function logout() {
    localStorage.removeItem("saimartUser");
    showToast("Logged out successfully", "info");
    setTimeout(() => {
        window.location.href = "/login";
    }, 400);
}

// ============================================================================
// 2. CART MANAGEMENT
// ============================================================================

function getCart() {
    try {
        const data = localStorage.getItem("saimartCart");
        return data ? JSON.parse(data) : [];
    } catch (e) {
        return [];
    }
}

function saveCart(cart) {
    localStorage.setItem("saimartCart", JSON.stringify(cart));
    updateNavCartBadge();
}

function getCartCount() {
    const cart = getCart();
    return cart.reduce((sum, item) => sum + (Number(item.quantity) || 1), 0);
}

function updateNavCartBadge() {
    const badges = document.querySelectorAll(".cart-count-badge, #cartCount, #navCartCount");
    const count = getCartCount();
    badges.forEach(b => {
        if (b) {
            b.innerText = count;
            b.style.display = count > 0 ? "inline-block" : "inline-block";
        }
    });
}

function addToCart(product, qty = 1) {
    if (!product || !product.id) {
        showToast("Invalid product details", "error");
        return;
    }

    const cart = getCart();
    const existing = cart.find(item => item.id === product.id);

    const availableStock = Number(product.stock) || 0;
    const currentQtyInCart = existing ? existing.quantity : 0;

    if (availableStock <= 0) {
        showToast("Sorry, this item is out of stock!", "error");
        return;
    }

    if (currentQtyInCart + qty > availableStock) {
        showToast(`Only ${availableStock} units available in stock.`, "warning");
        return;
    }

    if (existing) {
        existing.quantity += qty;
        // Ensure sellerId and critical fields are up to date
        if (!existing.sellerId && product.sellerId) {
            existing.sellerId = product.sellerId;
        }
    } else {
        cart.push({
            id: product.id,
            sellerId: product.sellerId || null,
            name: product.name,
            description: product.description || "",
            category: product.category || "",
            price: Number(product.price) || 0,
            stock: availableStock,
            imageUrl: product.imageUrl || "",
            quantity: qty
        });
    }

    saveCart(cart);
    showToast(`Added "${product.name}" to cart! 🛍️`, "success");
}

// ============================================================================
// 3. TOAST NOTIFICATIONS
// ============================================================================

function showToast(message, type = "info") {
    let container = document.getElementById("saimart-toast-container");
    if (!container) {
        container = document.createElement("div");
        container.id = "saimart-toast-container";
        document.body.appendChild(container);
    }

    const toast = document.createElement("div");
    toast.className = `saimart-toast ${type}`;

    let icon = "ℹ️";
    if (type === "success") icon = "✓";
    if (type === "error") icon = "✕";
    if (type === "warning") icon = "⚠️";

    toast.innerHTML = `<span style="font-size:16px;">${icon}</span><span>${escapeHTML(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = "toastSlideOut 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards";
        setTimeout(() => toast.remove(), 300);
    }, 3200);
}

// ============================================================================
// 4. HTML ESCAPING
// ============================================================================

function escapeHTML(str) {
    if (str === null || str === undefined) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// ============================================================================
// 5. UNIVERSAL AI CHATBOT WIDGET
// ============================================================================

function initChatbot() {
    // Check if chatbot container already exists
    if (document.getElementById("saimartChatbotTrigger")) return;

    // Trigger button
    const trigger = document.createElement("button");
    trigger.id = "saimartChatbotTrigger";
    trigger.className = "saimart-chatbot-trigger";
    trigger.innerHTML = "🤖";
    trigger.title = "Ask SaiBot - AI Shopping Assistant";

    // Chatbot window
    const win = document.createElement("div");
    win.id = "saimartChatbotWindow";
    win.className = "saimart-chatbot-window";
    win.innerHTML = `
        <div class="chat-header">
            <div class="chat-header-info">
                <div class="chat-avatar">🤖</div>
                <div>
                    <div class="chat-title">SaiBot Assistant</div>
                    <div class="chat-subtitle">● Online • Saimart AI</div>
                </div>
            </div>
            <button class="chat-close" id="closeChatbot">&times;</button>
        </div>
        <div class="chat-messages" id="chatbotMessages">
            <div class="chat-msg bot">
                Hello! 👋 I'm <strong>SaiBot</strong>, your shopping assistant. How can I help you today?
                <div class="chat-suggestions">
                    <button class="chat-chip" onclick="sendChatPrompt('Find Headphones')">🎧 Headphones</button>
                    <button class="chat-chip" onclick="sendChatPrompt('Show Electronics')">📱 Electronics</button>
                    <button class="chat-chip" onclick="sendChatPrompt('Track my order')">🚚 Track Order</button>
                    <button class="chat-chip" onclick="sendChatPrompt('How does payment work?')">💳 Payment</button>
                    <button class="chat-chip" onclick="sendChatPrompt('How to sell?')">🏪 Sell on Saimart</button>
                </div>
            </div>
        </div>
        <form class="chat-input-area" id="chatbotForm" onsubmit="event.preventDefault(); submitChatMessage();">
            <input type="text" id="chatbotInput" placeholder="Ask about products, orders, delivery..." autocomplete="off">
            <button type="submit">➤</button>
        </form>
    `;

    document.body.appendChild(trigger);
    document.body.appendChild(win);

    trigger.addEventListener("click", () => {
        win.classList.toggle("open");
        if (win.classList.contains("open")) {
            document.getElementById("chatbotInput").focus();
        }
    });

    document.getElementById("closeChatbot").addEventListener("click", () => {
        win.classList.remove("open");
    });
}

function sendChatPrompt(text) {
    const input = document.getElementById("chatbotInput");
    if (input) {
        input.value = text;
        submitChatMessage();
    }
}

async function submitChatMessage() {
    const input = document.getElementById("chatbotInput");
    const container = document.getElementById("chatbotMessages");
    const query = input.value.trim();
    if (!query) return;

    // Append user message
    const userMsg = document.createElement("div");
    userMsg.className = "chat-msg user";
    userMsg.textContent = query;
    container.appendChild(userMsg);
    input.value = "";
    container.scrollTop = container.scrollHeight;

    // Typing placeholder
    const typing = document.createElement("div");
    typing.className = "chat-msg bot";
    typing.id = "botTyping";
    typing.textContent = "SaiBot is thinking...";
    container.appendChild(typing);
    container.scrollTop = container.scrollHeight;

    try {
        const user = getUser();
        const response = await fetch("/api/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: query, userId: user ? user.id : null })
        });

        typing.remove();

        if (!response.ok) {
            throw new Error("Failed to get response");
        }

        const data = await response.json();
        const botMsg = document.createElement("div");
        botMsg.className = "chat-msg bot";

        let html = `<div>${escapeHTML(data.reply || "")}</div>`;

        // Render product cards if any
        if (data.products && data.products.length > 0) {
            data.products.forEach(p => {
                html += `
                    <a href="/product-details?id=${p.id}" class="chat-product-card">
                        <img src="${p.imageUrl || 'https://via.placeholder.com/60x60?text=Saimart'}" alt="${escapeHTML(p.name)}" onerror="this.src='https://via.placeholder.com/60x60?text=Saimart'">
                        <div class="chat-product-info">
                            <div class="chat-product-name">${escapeHTML(p.name)}</div>
                            <div class="chat-product-price">₹${Number(p.price).toLocaleString("en-IN")}</div>
                        </div>
                    </a>
                `;
            });
        }

        // Action button if present
        if (data.actionUrl && data.actionLabel) {
            html += `
                <div style="margin-top:8px;">
                    <a href="${data.actionUrl}" class="btn btn-primary" style="padding:4px 10px; font-size:11px; text-decoration:none;">${escapeHTML(data.actionLabel)}</a>
                </div>
            `;
        }

        // Suggestions
        if (data.suggestions && data.suggestions.length > 0) {
            html += `<div class="chat-suggestions">`;
            data.suggestions.forEach(s => {
                html += `<button class="chat-chip" onclick="sendChatPrompt('${escapeHTML(s)}')">${escapeHTML(s)}</button>`;
            });
            html += `</div>`;
        }

        botMsg.innerHTML = html;
        container.appendChild(botMsg);
        container.scrollTop = container.scrollHeight;

    } catch (e) {
        if (document.getElementById("botTyping")) {
            document.getElementById("botTyping").remove();
        }
        const errorMsg = document.createElement("div");
        errorMsg.className = "chat-msg bot";
        errorMsg.textContent = "I'm having trouble connecting right now. Please try again or browse our catalogue.";
        container.appendChild(errorMsg);
        container.scrollTop = container.scrollHeight;
    }
}

// Auto-initialize components on DOM ready
document.addEventListener("DOMContentLoaded", () => {
    updateNavCartBadge();
    initChatbot();
});
