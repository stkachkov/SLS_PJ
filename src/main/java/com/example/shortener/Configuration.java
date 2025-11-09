package com.example.shortener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Configuration {

    private static final Properties properties = new Properties();
    private static final String CONFIG_FILE = "config.properties";

    static {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            System.err.println("Ошибка: не удалось загрузить файл конфигурации '" + CONFIG_FILE + "'. Будут использованы значения по умолчанию.");
            // Устанавливаем значения по умолчанию, если файл не найден
            properties.setProperty("link.expiration-seconds", "86400 ");
            properties.setProperty("link.default-limit", "100");
        }
    }

    public static long getExpirationSeconds() {
        return Long.parseLong(properties.getProperty("link.expiration-seconds"));
    }

    public static int getDefaultLimit() {
        return Integer.parseInt(properties.getProperty("link.default-limit"));
    }
}
