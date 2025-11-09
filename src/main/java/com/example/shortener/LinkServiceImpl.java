package com.example.shortener;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LinkServiceImpl implements LinkService, AutoCloseable {

    final Map<String, Link> linkStorage = new ConcurrentHashMap<>();
    private static final long EXPIRATION_SECONDS = Configuration.getExpirationSeconds();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Clock clock;

    // Основной конструктор, используемый приложением
    public LinkServiceImpl() {
        this(Clock.systemDefaultZone());
    }

    // Конструктор для использования в тестах
    public LinkServiceImpl(Clock clock) {
        this.clock = clock;
        scheduler.scheduleAtFixedRate(this::purgeExpiredLinks, 1, 1, TimeUnit.HOURS);
    }

    private void purgeExpiredLinks() {
        linkStorage.values().removeIf(this::isExpired);
        System.out.println("Произведена плановая очистка истекших ссылок.");
    }

    @Override
    public Link create(String originalUrl, String userUuid, int limit) {

        String shortUrl = NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NanoIdUtils.DEFAULT_ALPHABET, 8);

        Link newLink = new Link(originalUrl, shortUrl, userUuid, limit, LocalDateTime.now(clock));

        linkStorage.put(shortUrl, newLink);

        return newLink;
    }

    @Override
    public Optional<Link> getByShortUrl(String shortUrl) {
        Optional<Link> linkOptional = Optional.ofNullable(linkStorage.get(shortUrl));

        if (linkOptional.isPresent()) {
            Link link = linkOptional.get();
            if (isExpired(link)) {
                linkStorage.remove(shortUrl);
                System.out.println("Уведомление: Ссылка " + shortUrl + " удалена по истечении срока жизни.");
                return Optional.empty();
            }
        }

        return linkOptional;
    }

    @Override
    public Optional<String> getOriginalUrlAndRegisterVisit(String shortUrl) {
        Optional<Link> linkOptional = getByShortUrl(shortUrl);
        if (linkOptional.isEmpty()) {
            return Optional.empty();
        }

        Link link = linkOptional.get();
        if (link.getVisitCount() < link.getLimit()) {
            link.incrementVisitCount();
            return Optional.of(link.getOriginalUrl());
        } else {
            linkStorage.remove(shortUrl);
            System.out.println("Уведомление: Лимит для ссылки " + shortUrl + " исчерпан, ссылка удалена.");
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String shortUrl, String userUuid) {
        Optional<Link> linkOptional = Optional.ofNullable(linkStorage.get(shortUrl));

        if (linkOptional.isPresent()) {
            Link link = linkOptional.get();
            if (link.getUserUuid().equals(userUuid)) {
                linkStorage.remove(shortUrl);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean updateLimit(String shortUrl, String userUuid, int newLimit) {
        Optional<Link> linkOptional = Optional.ofNullable(linkStorage.get(shortUrl));

        if (linkOptional.isPresent()) {
            Link link = linkOptional.get();
            if (link.getUserUuid().equals(userUuid)) {
                link.setLimit(newLimit);
                return true;
            }
        }
        return false;
    }

    private boolean isExpired(Link link) {
        return link.getCreatedAt().plusSeconds(EXPIRATION_SECONDS).isBefore(LocalDateTime.now(clock));
    }

    @Override
    public void close() {
        scheduler.shutdown();
    }
}
