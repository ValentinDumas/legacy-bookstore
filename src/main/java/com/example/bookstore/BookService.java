// BookService.java
package com.example.bookstore;

import java.util.*;
import java.sql.*;
import java.io.*;

public class BookService {
    
    // Static dependencies
    private static BookRepository bookRepository = new BookRepository();
    private static AuthorRepository authorRepository = new AuthorRepository();
    
    public List<Book> findAllBooks() throws SQLException {
        List<Book> books = bookRepository.findAll();
        
        // Mixed responsibilities - caching logic
        cacheBooks(books);
        
        // Mixed responsibilities - enrichment
        for (Book book : books) {
            enrichBookData(book);
        }
        
        return books;
    }
    
    public Book findBookById(Long id) throws SQLException {
        if (id == null || id <= 0) {
            return null;
        }
        
        Book book = bookRepository.findById(id);
        if (book != null) {
            enrichBookData(book);
            updateViewCount(book);
        }
        
        return book;
    }
    
    public Book saveBook(Book book) throws SQLException {
        // Business validation mixed with persistence logic
        validateBook(book);
        
        // Author handling with direct repository calls
        if (!authorRepository.existsByName(book.getAuthor())) {
            authorRepository.createAuthor(book.getAuthor());
        }
        
        // Generate internal code
        book.setInternalCode(generateInternalCode(book));
        
        Book savedBook = bookRepository.save(book);
        
        // Mixed responsibilities - inventory management
        updateInventory(savedBook);
        
        // Mixed responsibilities - recommendation engine update
        updateRecommendations(savedBook);
        
        return savedBook;
    }
    
    public Book updateBook(Book book) throws SQLException {
        validateBook(book);
        
        Book updatedBook = bookRepository.update(book);
        
        // Clear cache after update (tightly coupled caching)
        clearBookCache();
        
        return updatedBook;
    }
    
    public void deleteBook(Long id) throws SQLException {
        Book book = findBookById(id);
        if (book != null) {
            // Cascade delete logic mixed in service
            authorRepository.decrementBookCount(book.getAuthor());
            bookRepository.deleteById(id);
            
            // Clear cache
            clearBookCache();
            
            // Update recommendations
            removeFromRecommendations(book);
        }
    }
    
    // Private methods with mixed responsibilities
    private void validateBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("Author is required");
        }
        if (book.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
    }
    
    private void enrichBookData(Book book) {
        // Direct external API call (hard to test)
        try {
            book.setGenre(fetchGenreFromExternalAPI(book.getIsbn()));
            book.setDescription(fetchDescriptionFromExternalAPI(book.getIsbn()));
        } catch (Exception e) {
            // Default values on error
            book.setGenre("Unknown");
            book.setDescription("No description available");
        }
    }
    
    private String fetchGenreFromExternalAPI(String isbn) throws Exception {
        // Simulated external API call
        Thread.sleep(50);
        return "Fiction"; // Hardcoded for simulation
    }
    
    private String fetchDescriptionFromExternalAPI(String isbn) throws Exception {
        // Simulated external API call
        Thread.sleep(50);
        return "A fascinating book about...";
    }
    
    private void updateViewCount(Book book) {
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/bookstore", "root", "password");
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE books SET view_count = view_count + 1 WHERE id = ?");
            stmt.setLong(1, book.getId());
            stmt.executeUpdate();
            conn.close();
        } catch (SQLException e) {
            // Ignoring SQL exceptions
            System.err.println("Failed to update view count");
        }
    }
    
    private String generateInternalCode(Book book) {
        return book.getAuthor().substring(0, 2).toUpperCase() + 
               book.getTitle().substring(0, 2).toUpperCase() + 
               System.currentTimeMillis() % 10000;
    }
    
    private void cacheBooks(List<Book> books) {
        try {
            FileWriter fw = new FileWriter("book_cache.txt");
            for (Book book : books) {
                fw.write(book.getId() + "," + book.getTitle() + "," + book.getAuthor() + "\n");
            }
            fw.close();
        } catch (IOException e) {
            System.err.println("Failed to cache books");
        }
    }
    
    private void clearBookCache() {
        try {
            new File("book_cache.txt").delete();
        } catch (Exception e) {
            System.err.println("Failed to clear cache");
        }
    }
    
    private void updateInventory(Book book) {
        // Hardcoded inventory logic
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/inventory", "root", "password");
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO inventory (book_id, quantity) VALUES (?, 10)");
            stmt.setLong(1, book.getId());
            stmt.executeUpdate();
            conn.close();
        } catch (SQLException e) {
            System.err.println("Failed to update inventory");
        }
    }
    
    private void updateRecommendations(Book book) {
        // Complex recommendation logic that should be separate
        try {
            List<Book> similarBooks = findSimilarBooks(book);
            FileWriter fw = new FileWriter("recommendations.txt", true);
            fw.write(book.getId() + ":");
            for (Book similar : similarBooks) {
                fw.write(similar.getId() + ",");
            }
            fw.write("\n");
            fw.close();
        } catch (Exception e) {
            System.err.println("Failed to update recommendations");
        }
    }
    
    private void removeFromRecommendations(Book book) {
        // Complex file manipulation
        try {
            List<String> lines = new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader("recommendations.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith(book.getId() + ":")) {
                    lines.add(line);
                }
            }
            br.close();
            
            FileWriter fw = new FileWriter("recommendations.txt");
            for (String l : lines) {
                fw.write(l + "\n");
            }
            fw.close();
        } catch (Exception e) {
            System.err.println("Failed to remove from recommendations");
        }
    }
    
    private List<Book> findSimilarBooks(Book book) throws SQLException {
        // Inefficient similarity algorithm
        List<Book> allBooks = bookRepository.findAll();
        List<Book> similar = new ArrayList<>();
        
        for (Book other : allBooks) {
            if (!other.getId().equals(book.getId()) && 
                other.getAuthor().equals(book.getAuthor())) {
                similar.add(other);
            }
        }
        
        return similar.subList(0, Math.min(3, similar.size()));
    }
}