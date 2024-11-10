package com.vulnmonitor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseServiceTest {
	 private static final String DB_URL = "";
	    private static final String DB_USER = "";
	    private static final String DB_PASSWORD = "";

	    public static void main(String[] args) {
	        try (Connection _ = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
	            System.out.println("Database connected successfully.");
	        } catch (SQLException e) {
	            System.out.println("Error connecting to database: " + e.getMessage());
	            e.printStackTrace();
	        }
	    }
}
