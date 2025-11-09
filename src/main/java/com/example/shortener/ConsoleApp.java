package com.example.shortener;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleApp {

    private final LinkService linkService;
    private final String userUuid;
    private static final int DEFAULT_LIMIT = Configuration.getDefaultLimit();

    public ConsoleApp(LinkService linkService) {
        this.linkService = linkService;
        this.userUuid = UUID.randomUUID().toString();
    }

    public void run() {
        System.out.println("Добро пожаловать в сервис сокращения ссылок!");
        System.out.println("Ваш уникальный идентификатор (UUID): " + userUuid);
        printHelp();

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
            case "update":
                handleUpdateCommand(parts);
                break;
            case "help":
                printHelp();
                break;
            default:
                handleRedirect(input);
                break;
        }
    }

    private void printHelp() {
        System.out.println("\nКОМАНДЫ:");
        System.out.println("  create <url> [limit] - создать короткую ссылку (лимит необязателен)");
        System.out.println("  info <short_url>    - посмотреть информацию о ссылке");
        System.out.println("  update <short_url> <new_limit> - обновить лимит переходов");
        System.out.println("  delete <short_url>  - удалить ссылку");
        System.out.println("  <short_url>         - перейти по короткой ссылке");
        System.out.println("  help                - показать этот список команд");
        System.out.println("  exit                - выход из программы\n");
    }

    private void handleDeleteCommand(String[] parts) {
        if (parts.length != 2) {
            System.out.println("Ошибка: неверный формат. Используйте: delete <short_url>");
            return;
        }
        String shortUrl = parts[1];
        if (linkService.delete(shortUrl, userUuid)) {
            System.out.println("Ссылка " + shortUrl + " успешно удалена.");
        } else {
            System.out.println("Ошибка: ссылка не найдена или у вас нет прав на ее удаление.");
        }
    }

    private void handleUpdateCommand(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Ошибка: неверный формат. Используйте: update <short_url> <new_limit>");
            return;
        }

        String shortUrl = parts[1];
        int newLimit;

        try {
            newLimit = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: лимит должен быть числом.");
            return;
        }

        if (linkService.updateLimit(shortUrl, userUuid, newLimit)) {
            System.out.println("Лимит для ссылки " + shortUrl + " обновлен на " + newLimit + ".");
        } else {
            System.out.println("Ошибка: ссылка не найдена или у вас нет прав на ее редактирование.");
        }
    }

    private void handleCreateCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Ошибка: неверный формат. Используйте: create <url> [limit]");
            return;
        }

        String url = parts[1];
        if (!isValidUrl(url)) {
            System.out.println("Ошибка: введен невалидный URL. Убедитесь, что он начинается с http:// или https://");
            return;
        }

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

    private boolean isValidUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        try (LinkServiceImpl linkService = new LinkServiceImpl()) {
            ConsoleApp app = new ConsoleApp(linkService);
            app.run();
        } catch (Exception e) {
            System.err.println("Произошла критическая ошибка: " + e.getMessage());
        }
    }
}
