package com.example.shortener;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LinkServiceImpl implements LinkService {

    private final Map<String, Link> linkStorage = new ConcurrentHashMap<>();
    private static final long EXPIRATION_SECONDS = 24 * 60 * 60; // 24 часа

    @Override
    public Link create(String originalUrl, String userUuid, int limit) {

        String shortUrl = NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NanoIdUtils.DEFAULT_ALPHABET, 8);

        Link newLink = new Link(originalUrl, shortUrl, userUuid, limit);

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
    public boolean delete(String shortUrl) {
        return linkStorage.remove(shortUrl) != null;
    }

    private boolean isExpired(Link link) {
        return link.getCreatedAt().plusSeconds(EXPIRATION_SECONDS).isBefore(LocalDateTime.now());
    }
}
