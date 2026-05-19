package com.project.artconnect.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Database configuration constants.
 */
public class DatabaseConfig {
    private static final Properties PROPERTIES = loadProperties();

    public static final String URL = readSetting(
            "artconnect.db.url",
            "ARTCONNECT_DB_URL",
            "jdbc:mysql://localhost:3306/artconnect?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Paris");
    public static final String USER = readSetting("artconnect.db.user", "ARTCONNECT_DB_USER", "root");
    public static final String PASSWORD = readSetting("artconnect.db.password", "ARTCONNECT_DB_PASSWORD", "root");

    private DatabaseConfig() {
    }

    private static String readSetting(String propertyName, String envName, String fallback) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        String fileValue = PROPERTIES.getProperty(propertyName);
        if (fileValue != null && !fileValue.isBlank()) {
            return fileValue;
        }

        return fallback;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("artconnect-db.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load artconnect-db.properties.", e);
        }
        return properties;
    }
}
