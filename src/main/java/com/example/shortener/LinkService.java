package com.example.shortener;

import java.util.Optional;

public interface LinkService {
    Link create(String originalUrl, String userUuid, int limit);
    Optional<Link> getByShortUrl(String shortUrl);
    Optional<String> getOriginalUrlAndRegisterVisit(String shortUrl);
    boolean delete(String shortUrl);
}
