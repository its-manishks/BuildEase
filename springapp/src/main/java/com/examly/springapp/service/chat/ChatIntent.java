package com.examly.springapp.service.chat;

/** What the question is about. Decided by IntentDetector (rules), never by the AI. */
public enum ChatIntent {
    GENERAL,
    SENSITIVE,
    // the signed-in customer's own data
    MY_ORDERS,
    MY_LATEST_ORDER,
    MY_MATERIALS,
    MY_REVIEWS,
    MY_RECOMMENDATIONS,
    // admin statistics (no customer personal data)
    ADMIN_STOCK,
    ADMIN_OUT_OF_STOCK,
    ADMIN_PENDING,
    ADMIN_SHIPPED,
    ADMIN_ORDER_TOTALS
}