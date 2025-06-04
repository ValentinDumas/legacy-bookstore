// AuthorRepository.java
package com.example.bookstore;

import java.sql.*;

public class AuthorRepository {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bookstore";
    private static final String USER = "root";
    private static final String PASS = "password";
    
    public boolean existsByName(String name) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM authors WHERE name = ?");
        stmt.setString(1, name);
        ResultSet rs = stmt.executeQuery();
        
        boolean exists = false;
        if (rs.next()) {
            exists = rs.getInt(1) > 0;
        }
        
        rs.close();
        stmt.close();
        conn.close();
        
        return exists;
    }
    
    public void createAuthor(String name) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO authors (name, book_count) VALUES (?, 1)");
        stmt.setString(1, name);
        stmt.executeUpdate();
        
        stmt.close();
        conn.close();
    }
    
    public void decrementBookCount(String name) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        PreparedStatement stmt = conn.prepareStatement(
            "UPDATE authors SET book_count = book_count - 1 WHERE name = ?");
        stmt.setString(1, name);
        stmt.executeUpdate();
        
        stmt.close();
        conn.close();
    }
}