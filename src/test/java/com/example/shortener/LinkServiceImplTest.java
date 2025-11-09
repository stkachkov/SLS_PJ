package com.example.shortener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LinkServiceImplTest {

    private LinkServiceImpl linkService;
    private Clock clock;
    private final String testUserUuid = "test-user-1";
    private final String anotherUserUuid = "test-user-2";
    private final String testUrl = "https://example.com";

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2025-11-04T10:00:00Z"), ZoneId.of("UTC"));
        linkService = new LinkServiceImpl(clock);
    }

    @Test
    void create_ShouldReturnLinkWithCorrectProperties() {
        Link link = linkService.create(testUrl, testUserUuid, 10);

        assertNotNull(link);
        assertEquals(testUrl, link.getOriginalUrl());
        assertEquals(testUserUuid, link.getUserUuid());
        assertEquals(10, link.getLimit());
        assertNotNull(link.getShortUrl());
        assertEquals(8, link.getShortUrl().length());
    }

    @Test
    void delete_ShouldDeleteLink_WhenUserIsOwner() {
        Link link = linkService.create(testUrl, testUserUuid, 10);
        boolean isDeleted = linkService.delete(link.getShortUrl(), testUserUuid);

        assertTrue(isDeleted);
        assertTrue(linkService.getByShortUrl(link.getShortUrl()).isEmpty());
    }

    @Test
    void delete_ShouldNotDeleteLink_WhenUserIsNotOwner() {
        Link link = linkService.create(testUrl, testUserUuid, 10);
        boolean isDeleted = linkService.delete(link.getShortUrl(), anotherUserUuid);

        assertFalse(isDeleted);
        assertTrue(linkService.getByShortUrl(link.getShortUrl()).isPresent());
    }

    @Test
    void getOriginalUrlAndRegisterVisit_ShouldReturnUrl_WhenLinkIsValid() {
        Link link = linkService.create(testUrl, testUserUuid, 1);
        Optional<String> originalUrl = linkService.getOriginalUrlAndRegisterVisit(link.getShortUrl());

        assertTrue(originalUrl.isPresent());
        assertEquals(testUrl, originalUrl.get());
    }

    @Test
    void getOriginalUrlAndRegisterVisit_ShouldIncrementVisitCount() {
        Link link = linkService.create(testUrl, testUserUuid, 5);
        linkService.getOriginalUrlAndRegisterVisit(link.getShortUrl());
        linkService.getOriginalUrlAndRegisterVisit(link.getShortUrl());

        Optional<Link> updatedLink = linkService.getByShortUrl(link.getShortUrl());

        assertTrue(updatedLink.isPresent());
        assertEquals(2, updatedLink.get().getVisitCount());
    }

    @Test
    void getOriginalUrlAndRegisterVisit_ShouldReturnEmpty_WhenLimitIsExceeded() {
        Link link = linkService.create(testUrl, testUserUuid, 1);
        linkService.getOriginalUrlAndRegisterVisit(link.getShortUrl());

        Optional<String> secondVisitUrl = linkService.getOriginalUrlAndRegisterVisit(link.getShortUrl());

        assertTrue(secondVisitUrl.isEmpty());
    }

    @Test
    void updateLimit_ShouldUpdateLimit_WhenUserIsOwner() {
        Link link = linkService.create(testUrl, testUserUuid, 10);
        boolean isUpdated = linkService.updateLimit(link.getShortUrl(), testUserUuid, 20);

        assertTrue(isUpdated);

        Optional<Link> updatedLink = linkService.getByShortUrl(link.getShortUrl());

        assertTrue(updatedLink.isPresent());
        assertEquals(20, updatedLink.get().getLimit());
    }

    @Test
    void updateLimit_ShouldNotUpdateLimit_WhenUserIsNotOwner() {
        Link link = linkService.create(testUrl, testUserUuid, 10);
        boolean isUpdated = linkService.updateLimit(link.getShortUrl(), anotherUserUuid, 20);

        assertFalse(isUpdated);

        Optional<Link> notUpdatedLink = linkService.getByShortUrl(link.getShortUrl());

        assertTrue(notUpdatedLink.isPresent());
        assertEquals(10, notUpdatedLink.get().getLimit());
    }

    @Test
    void getByShortUrl_ShouldReturnEmpty_WhenLinkIsExpired() {
        Link link = linkService.create(testUrl, testUserUuid, 1);
        String shortUrl = link.getShortUrl();

        Clock futureClock = Clock.offset(clock, java.time.Duration.ofHours(25));
        try (LinkServiceImpl futureService = new LinkServiceImpl(futureClock)) {
            futureService.linkStorage.put(shortUrl, link);

            Optional<Link> expiredLink = futureService.getByShortUrl(shortUrl);

            assertTrue(expiredLink.isEmpty(), "Ссылка должна была истечь и удалиться");
        }
    }
}
