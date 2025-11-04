package com.example.shortener;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleApp {

    private final LinkService linkService;
    private final String userUuid;
    private static final int DEFAULT_LIMIT = 100;

    public ConsoleApp() {
        this.linkService = new LinkServiceImpl();
        this.userUuid = UUID.randomUUID().toString();
    }

    public void run() {
        System.out.println("Добро пожаловать в сервис сокращения ссылок!");
        System.out.println("Ваш уникальный идентификатор (UUID): " + userUuid);
        System.out.println("\nКОМАНДЫ:");
        System.out.println("  create <url> [limit] - создать короткую ссылку (лимит необязателен)");
        System.out.println("  info <short_url>    - посмотреть информацию о ссылке");
        System.out.println("  delete <short_url>  - удалить ссылку");
        System.out.println("  <short_url>         - перейти по короткой ссылке");
        System.out.println("  exit                - выход из программы\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("До свидания!");
                    break;
                }
                processCommand(input);
            }
        }
    }

    public void processCommand(String input) {
        if (input == null || input.isBlank()) {
            return;
        }

        String[] parts = input.split("\\s+", 3);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "create":
                handleCreateCommand(parts);
                break;
            case "info":
                handleInfoCommand(parts);
                break;
            case "delete":
                handleDeleteCommand(parts);
                break;
            default:
                handleRedirect(input);
                break;
        }
    }

    private void handleDeleteCommand(String[] parts) {
        if (parts.length != 2) {
            System.out.println("Ошибка: неверный формат. Используйте: delete <short_url>");
            return;
        }
        String shortUrl = parts[1];
        if (linkService.delete(shortUrl)) {
            System.out.println("Ссылка " + shortUrl + " успешно удалена.");
        } else {
            System.out.println("Ошибка: ссылка не найдена.");
        }
    }

    private void handleCreateCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Ошибка: неверный формат. Используйте: create <url> [limit]");
            return;
        }

        String url = parts[1];
        int limit = DEFAULT_LIMIT;

        if (parts.length == 3) {
            try {
                limit = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: лимит должен быть числом.");
                return;
            }
        }

        Link createdLink = linkService.create(url, userUuid, limit);
        System.out.println("Создана короткая ссылка: " + createdLink.getShortUrl() + " с лимитом " + limit + " переходов.");
    }

    private void handleInfoCommand(String[] parts) {
        if (parts.length != 2) {
            System.out.println("Ошибка: неверный формат. Используйте: info <short_url>");
            return;
        }
        String shortUrl = parts[1];
        linkService.getByShortUrl(shortUrl).ifPresentOrElse(
            link -> {
                System.out.println("Информация о ссылке " + link.getShortUrl() + ":");
                System.out.println("  Оригинал: " + link.getOriginalUrl());
                System.out.println("  Создана: " + link.getCreatedAt());
                System.out.println("  Лимит переходов: " + link.getLimit());
                System.out.println("  Использовано: " + link.getVisitCount());
                System.out.println("  Осталось: " + (link.getLimit() - link.getVisitCount()));
            },
            () -> System.out.println("Ошибка: ссылка не найдена или ее срок жизни истек.")
        );
    }

    private void handleRedirect(String shortUrl) {
        linkService.getOriginalUrlAndRegisterVisit(shortUrl).ifPresentOrElse(
            originalUrl -> {
                System.out.println("Переход на: " + originalUrl);
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI(originalUrl));
                        System.out.println("Команда на открытие браузера отправлена.");
                    } catch (IOException | URISyntaxException e) {
                        System.out.println("Произошла ошибка при попытке открыть ссылку: " + e.getMessage());
                    }
                } else {
                    System.out.println("Автоматическое открытие браузера не поддерживается. Пожалуйста, откройте ссылку вручную.");
                }
            },
            () -> System.out.println("Ошибка: ссылка не найдена, истек срок ее жизни или превышен лимит.")
        );
    }

    public static void main(String[] args) {
        ConsoleApp app = new ConsoleApp();
        app.run();
    }
}
