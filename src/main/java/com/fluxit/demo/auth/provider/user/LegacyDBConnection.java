package com.fluxit.demo.auth.provider.user;

import org.keycloak.component.ComponentModel;

import java.sql.Connection;
import java.sql.DriverManager;

public class LegacyDBConnection {

    private static final String URL = "jdbc:postgresql://localhost:5432/keycloak";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "postgres";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
