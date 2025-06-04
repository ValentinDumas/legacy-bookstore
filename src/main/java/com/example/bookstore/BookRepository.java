// BookRepository.java
package com.example.bookstore;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookRepository {

    // Hardcoded connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bookstore";
    private static final String USER = "root";
    private static final String PASS = "password";

    public List<Book> findAll() throws SQLException {
        List<Book> books = new ArrayList<>();

        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM books ORDER BY title");

        while (rs.next()) {
            Book book = mapResultSetToBook(rs);
            books.add(book);
        }

        rs.close();
        stmt.close();
        conn.close();

        return books;
    }

    public Book findById(Long id) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM books WHERE id = ?");
        stmt.setLong(1, id);
        ResultSet rs = stmt.executeQuery();

        Book book = null;
        if (rs.next()) {
            book = mapResultSetToBook(rs);
        }

        rs.close();
        stmt.close();
        conn.close();

        return book;
    }

    public Book save(Book book) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO books (title, author, price, isbn, internal_code) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);

        stmt.setString(1, book.getTitle());
        stmt.setString(2, book.getAuthor());
        stmt.setDouble(3, book.getPrice());
        stmt.setString(4, book.getIsbn());
        stmt.setString(5, book.getInternalCode());

        int affectedRows = stmt.executeUpdate();
        if (affectedRows == 0) {
            throw new SQLException("Creating book failed, no rows affected.");
        }

        ResultSet generatedKeys = stmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            book.setId(generatedKeys.getLong(1));
        }

        stmt.close();
        conn.close();

        return book;
    }

    public Book update(Book book) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE books SET title = ?, author = ?, price = ? WHERE id = ?");

        stmt.setString(1, book.getTitle());
        stmt.setString(2, book.getAuthor());
        stmt.setDouble(3, book.getPrice());
        stmt.setLong(4, book.getId());

        int affectedRows = stmt.executeUpdate();
        if (affectedRows == 0) {
            throw new SQLException("Updating book failed, no rows affected.");
        }

        stmt.close();
        conn.close();

        return book;
    }

    public void deleteById(Long id) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM books WHERE id = ?");
        stmt.setLong(1, id);

        int affectedRows = stmt.executeUpdate();
        if (affectedRows == 0) {
            throw new SQLException("Deleting book failed, no rows affected.");
        }

        stmt.close();
        conn.close();
    }

    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getLong("id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setPrice(rs.getDouble("price"));
        book.setIsbn(rs.getString("isbn"));
        book.setInternalCode(rs.getString("internal_code"));
        book.setViewCount(rs.getInt("view_count"));
        return book;
    }
}