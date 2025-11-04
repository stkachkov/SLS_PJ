package com.example.shortener;

import java.time.LocalDateTime;

public class Link {
    private final String originalUrl;
    private final String shortUrl;
    private final String userUuid;
    private final int limit;
    private final LocalDateTime createdAt;
    private int visitCount;

    public Link(String originalUrl, String shortUrl, String userUuid, int limit) {
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.userUuid = userUuid;
        this.limit = limit;
        this.createdAt = LocalDateTime.now();
        this.visitCount = 0;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public int getLimit() {
        return limit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public void incrementVisitCount() {
        this.visitCount++;
    }
}
