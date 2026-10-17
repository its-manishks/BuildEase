package com.examly.springapp.service.chat;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.examly.springapp.config.UserPrinciple;
import com.examly.springapp.dto.chat.ChatRequest;
import com.examly.springapp.dto.chat.ChatResponse;
import com.examly.springapp.dto.dashboard.AdminDashboardResponse;
import com.examly.springapp.dto.dashboard.LowStockItem;
import com.examly.springapp.dto.feedback.ReviewableMaterial;
import com.examly.springapp.dto.recommendation.RecommendationItem;
import com.examly.springapp.dto.request.MaterialRequestResponse;
import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;
import com.examly.springapp.model.enums.RequestStatus;
import com.examly.springapp.service.DashboardService;
import com.examly.springapp.service.FeedbackService;
import com.examly.springapp.service.MaterialRequestService;
import com.examly.springapp.service.ai.AiUnavailableException;
import com.examly.springapp.service.ai.GeminiClient;
import com.examly.springapp.service.recommendation.RecommendationService;

/**
 * The BuildEase assistant.
 *
 * Privacy and authorization are enforced HERE, not by the AI prompt:
 *  - the caller's identity comes only from the JWT (security context), never from the request body
 *  - visitors get general help only; personal questions get "please log in" without any lookup
 *  - a customer's answers are built from THEIR OWN records through the existing services (which
 *    re-check ownership); admins get catalogue/order statistics without customer personal data
 *  - personal answers are written by the backend and are NOT sent to Gemini; Gemini only sees the
 *    BuildEase knowledge text and general questions
 *  - requests for secrets or other people's data are refused before any lookup or AI call
 *  - message text is never logged
 */
@Service
public class ChatService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final String REFUSAL = "Sorry, I can't help with that. I never share passwords, codes, keys or"
            + " anyone's personal information, and I can only show your own orders once you're logged in.";

    private final GeminiClient gemini;
    private final ChatRateLimiter rateLimiter;
    private final MaterialRequestService materialRequestService;
    private final FeedbackService feedbackService;
    private final DashboardService dashboardService;
    private final RecommendationService recommendationService;

    public ChatService(GeminiClient gemini, ChatRateLimiter rateLimiter, MaterialRequestService materialRequestService,
                       FeedbackService feedbackService, DashboardService dashboardService,
                       RecommendationService recommendationService) {
        this.gemini = gemini;
        this.rateLimiter = rateLimiter;
        this.materialRequestService = materialRequestService;
        this.feedbackService = feedbackService;
        this.dashboardService = dashboardService;
        this.recommendationService = recommendationService;
    }

    public ChatResponse reply(ChatRequest request, String clientIp) {
        UserPrinciple caller = currentCaller();
        String limitKey = caller == null ? "ip:" + clientIp : "user:" + caller.getUserId();
        if (!rateLimiter.tryAcquire(limitKey, caller != null)) {
            throw new BusinessException(ErrorCode.CHAT_RATE_LIMITED,
                    "You're sending messages too quickly. Please wait a moment and try again.");
        }
        String message = request.message().trim();
        String role = caller == null ? null : caller.getRole();
        ChatIntent intent = IntentDetector.detect(message, role);
        switch (intent) {
            case SENSITIVE:
                return ChatResponse.assistant(REFUSAL);
            case GENERAL:
                return general(request, message, role);
            case MY_ORDERS:
            case MY_LATEST_ORDER:
            case MY_MATERIALS:
            case MY_REVIEWS:
            case MY_RECOMMENDATIONS:
                if (caller == null) {
                    return ChatResponse.loginRequired("Please log in to see your own requests, orders and reviews,"
                            + " then ask me again. I can still help with general questions about BuildEase.");
                }
                return customerAnswer(intent, caller.getUserId());
            default:
                return adminAnswer(intent);
        }
    }

    // ------------------------------------------------------------------ general questions (AI)

    private ChatResponse general(ChatRequest request, String message, String role) {
        String context = role == null ? "The person is a visitor who is not logged in."
                : "The person is logged in as " + ("Admin".equals(role) ? "an admin." : "a customer.")
                + " For their own orders, suggest asking e.g. 'What is the status of my latest order?'.";
        try {
            return ChatResponse.ai(gemini.generate(BuildEaseKnowledge.SYSTEM_INSTRUCTION + "\n\n" + context,
                    request.history(), message));
        } catch (AiUnavailableException ex) {
            return ChatResponse.assistant(BuildEaseKnowledge.localAnswer(message));
        }
    }

    // ------------------------------------------------------------------ customer: own data only

    private ChatResponse customerAnswer(ChatIntent intent, Long userId) {
        switch (intent) {
            case MY_REVIEWS:
                return ChatResponse.assistant(reviewsAnswer(feedbackService.getMaterialsAwaitingReview(userId)));
            case MY_RECOMMENDATIONS:
                return ChatResponse.assistant(recommendationsAnswer(recommendationService.forUser(userId, 3).items()));
            case MY_MATERIALS:
                return ChatResponse.assistant(materialsAnswer(
                        materialRequestService.getMyRequests(userId, null, null, 0, 50).getContent()));
            case MY_LATEST_ORDER:
                return ChatResponse.assistant(latestAnswer(
                        materialRequestService.getMyRequests(userId, null, null, 0, 1).getContent()));
            default:
                var page = materialRequestService.getMyRequests(userId, null, null, 0, 5);
                return ChatResponse.assistant(ordersAnswer(page.getContent(), page.getTotalElements()));
        }
    }

    private String ordersAnswer(List<MaterialRequestResponse> requests, long total) {
        if (requests.isEmpty()) {
            return "You haven't placed any requests yet. Open View Materials and click Request on a material to"
                    + " place one.";
        }
        String lines = requests.stream().map(this::describe).collect(Collectors.joining("\n"));
        return "Your most recent requests (" + total + " in total):\n" + lines
                + "\nOpen My Requests for details and status filters.";
    }

    private String latestAnswer(List<MaterialRequestResponse> requests) {
        if (requests.isEmpty()) {
            return "You haven't placed any requests yet.";
        }
        MaterialRequestResponse latest = requests.get(0);
        String next;
        switch (latest.status()) {
            case PENDING:
                next = "It's waiting for admin approval. You can still edit the quantity or delete it in My Requests.";
                break;
            case APPROVED:
                next = "It has been approved and will be shipped.";
                break;
            case REJECTED:
                next = "It was rejected. Open My Requests and click Show More to see the reason.";
                break;
            default:
                next = "It has been shipped. You can now write a review for it in My Feedbacks.";
        }
        return "Your latest request: " + describe(latest) + "\n" + next;
    }

    private String materialsAnswer(List<MaterialRequestResponse> requests) {
        List<MaterialRequestResponse> counted = requests.stream()
                .filter(r -> r.status() != RequestStatus.REJECTED).toList();
        if (counted.isEmpty()) {
            return "You haven't ordered any materials yet.";
        }
        Map<String, int[]> totals = new LinkedHashMap<>();
        Map<String, String> units = new LinkedHashMap<>();
        for (MaterialRequestResponse r : counted) {
            String name = r.material().materialName();
            int[] t = totals.computeIfAbsent(name, k -> new int[2]);
            t[0]++;
            t[1] += r.quantity();
            units.putIfAbsent(name, r.material().unitType().getLabel());
        }
        String lines = totals.entrySet().stream().limit(8)
                .map(e -> "- " + e.getKey() + ": " + e.getValue()[1] + " " + units.get(e.getKey()) + " across "
                        + e.getValue()[0] + (e.getValue()[0] == 1 ? " request" : " requests"))
                .collect(Collectors.joining("\n"));
        return "Materials you've requested (excluding rejected requests):\n" + lines
                + "\nTo order one again, use 'Order again' on the Home page or 'Request Again' on its card.";
    }

    private String reviewsAnswer(List<ReviewableMaterial> awaiting) {
        if (awaiting.isEmpty()) {
            return "No materials are waiting for your review right now. You can review a material once its request"
                    + " has been shipped (one review per material).";
        }
        String names = awaiting.stream().map(ReviewableMaterial::materialName).limit(6).collect(Collectors.joining(", "));
        return "You can review: " + names + ". Open My Feedbacks and use 'Ready to review'.";
    }

    private String recommendationsAnswer(List<RecommendationItem> items) {
        if (items.isEmpty()) {
            return "I don't have suggestions right now; please check View Materials for what's in stock.";
        }
        String lines = items.stream().map(i -> "- " + i.material().materialName() + " (" + i.reason() + ")")
                .collect(Collectors.joining("\n"));
        return "You might find these useful:\n" + lines + "\nYou'll also see them under 'Recommended for you' on"
                + " View Materials.";
    }

    private String describe(MaterialRequestResponse r) {
        return "- #" + r.materialRequestId() + " " + r.material().materialName() + ": " + r.quantity() + " "
                + r.material().unitType().getLabel() + ", " + r.status().getLabel()
                + (r.requestDate() == null ? "" : " (placed " + r.requestDate().format(DATE) + ")");
    }

    // ------------------------------------------------------------------ admin: statistics only

    private ChatResponse adminAnswer(ChatIntent intent) {
        AdminDashboardResponse d = dashboardService.getAdminDashboard();
        switch (intent) {
            case ADMIN_OUT_OF_STOCK: {
                if (d.outOfStockMaterials() == 0) {
                    return ChatResponse.assistant("No active materials are out of stock right now.");
                }
                String names = d.lowStockItems().stream().filter(i -> i.quantity() == 0)
                        .map(LowStockItem::materialName).collect(Collectors.joining(", "));
                return ChatResponse.assistant(d.outOfStockMaterials() + " active material(s) are out of stock"
                        + (names.isEmpty() ? "." : ", including: " + names + ".")
                        + " Use Restock on the Materials page to add stock.");
            }
            case ADMIN_PENDING:
                return ChatResponse.assistant(d.pendingRequests() + " request(s) are waiting for approval. Open"
                        + " Requests and filter by Pending to review them.");
            case ADMIN_SHIPPED:
                return ChatResponse.assistant(d.shippedRequests() + " request(s) have been shipped so far.");
            case ADMIN_ORDER_TOTALS: {
                String breakdown = d.requestsByStatus().stream().map(p -> p.label() + " " + p.value())
                        .collect(Collectors.joining(", "));
                return ChatResponse.assistant(d.totalRequests() + " request(s) have been placed in total ("
                        + breakdown + ").");
            }
            default: {
                long inStock = d.activeMaterials() - d.outOfStockMaterials();
                String low = d.lowStockItems().stream().filter(i -> i.quantity() > 0)
                        .map(i -> i.materialName() + " (" + i.quantity() + " " + i.unitType() + ")")
                        .collect(Collectors.joining(", "));
                return ChatResponse.assistant("There are " + d.activeMaterials() + " active materials: " + inStock
                        + " in stock, " + d.lowStockMaterials() + " running low and " + d.outOfStockMaterials()
                        + " out of stock." + (low.isEmpty() ? "" : " Running low: " + low + "."));
            }
        }
    }

    /** The signed-in caller from the JWT, or null for visitors (never throws, unlike SecurityUtils). */
    private static UserPrinciple currentCaller() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof UserPrinciple principal
                ? principal : null;
    }
}