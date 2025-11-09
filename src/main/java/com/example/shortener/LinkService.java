package com.example.shortener;

import java.util.Optional;

public interface LinkService extends AutoCloseable {
    Link create(String originalUrl, String userUuid, int limit);
    Optional<Link> getByShortUrl(String shortUrl);
    Optional<String> getOriginalUrlAndRegisterVisit(String shortUrl);
    boolean delete(String shortUrl, String userUuid);
    boolean updateLimit(String shortUrl, String userUuid, int newLimit);

    @Override
    default void close() {}
}