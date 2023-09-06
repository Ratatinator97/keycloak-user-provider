package com.fluxit.demo.auth.provider.user;

import org.keycloak.component.ComponentModel;

import java.sql.Connection;
import java.sql.DriverManager;

public class LegacyDBConnection {

    private static final String CONFIG_KEY_JDBC_URL = "config.key.jdbc.url";
    private static final String CONFIG_KEY_DB_USERNAME = "config.key.db.username";
    private static final String CONFIG_KEY_DB_PASSWORD = "config.key.db.password";

    public static Connection getConnection(ComponentModel config) {
        try {
            return DriverManager.getConnection(config.get(CONFIG_KEY_JDBC_URL), config.get(CONFIG_KEY_DB_USERNAME), config.get(CONFIG_KEY_DB_PASSWORD));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}