package com.moola.fx.moneychanger.fxfileupload.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class DatabaseConfig {
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    static {
        loadYamlConfig();
    }

    private static void loadYamlConfig() {
        try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream("application.yml")) {
            if (in == null) {
                throw new RuntimeException("Cannot find application.yml in resources!");
            }
            Yaml yaml = new Yaml();
            Map<String, Object> yamlMap = yaml.load(in);

            // Traverse nested maps
            Map<String, Object> spring = (Map<String, Object>) yamlMap.get("spring");
            Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");

            dbUrl = (String) datasource.get("url");
            dbUser = (String) datasource.get("username");
            dbPassword = (String) datasource.get("password");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load database configuration from YAML", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}