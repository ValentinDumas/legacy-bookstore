// BookController.java
package com.example.bookstore;


import org.springframework.web.bind.annotation.*;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    // Static dependency - hard to mock
    private static BookService bookService = new BookService();

    @GetMapping
    public List<Book> getAllBooks() throws Exception {
        // Direct file I/O in controller
        logRequest("GET /api/books");

        List<Book> books = bookService.findAllBooks();

        // Business logic mixed in controller
        for (Book book : books) {
            if (book.getPrice() > 50.0) {
                book.setDiscountApplied(true);
                book.setDiscountedPrice(book.getPrice() * 0.9);
            }
        }

        return books;
    }

    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) throws Exception {
        logRequest("GET /api/books/" + id);

        Book book = bookService.findBookById(id);
        if (book == null) {
            throw new RuntimeException("Book not found with id: " + id);
        }

        // Direct database call in controller
        Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/bookstore", "root", "password");
        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE books SET view_count = view_count + 1 WHERE id = ?");
        stmt.setLong(1, id);
        stmt.executeUpdate();
        conn.close();

        return book;
    }

    @PostMapping
    public Book createBook(@RequestBody Book book) throws Exception {
        logRequest("POST /api/books");

        // Validation mixed with business logic
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        if (book.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        // Generate ISBN using current timestamp (bad practice)
        book.setIsbn("ISBN-" + System.currentTimeMillis());

        Book savedBook = bookService.saveBook(book);

        // Email notification hardcoded
        sendEmailNotification("New book added: " + book.getTitle());

        return savedBook;
    }

    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book book) throws Exception {
        logRequest("PUT /api/books/" + id);

        Book existingBook = bookService.findBookById(id);
        if (existingBook == null) {
            throw new RuntimeException("Book not found");
        }

        // Manual field copying
        if (book.getTitle() != null) {
            existingBook.setTitle(book.getTitle());
        }
        if (book.getAuthor() != null) {
            existingBook.setAuthor(book.getAuthor());
        }
        if (book.getPrice() > 0) {
            existingBook.setPrice(book.getPrice());
        }

        return bookService.updateBook(existingBook);
    }

    @DeleteMapping("/{id}")
    public void deleteBook(@PathVariable Long id) throws Exception {
        logRequest("DELETE /api/books/" + id);

        Book book = bookService.findBookById(id);
        if (book == null) {
            throw new RuntimeException("Book not found");
        }

        bookService.deleteBook(id);

        // Audit log with direct file write
        FileWriter fw = new FileWriter("audit.log", true);
        fw.write(new Date() + " - Book deleted: " + book.getTitle() + "\n");
        fw.close();
    }

    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam String query) throws Exception {
        logRequest("GET /api/books/search?query=" + query);

        List<Book> allBooks = bookService.findAllBooks();
        List<Book> results = new ArrayList<>();

        // Inefficient search logic in controller
        for (Book book : allBooks) {
            if (book.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    book.getAuthor().toLowerCase().contains(query.toLowerCase())) {
                results.add(book);
            }
        }

        return results;
    }

    @GetMapping("/reports/sales")
    public Map<String, Object> getSalesReport() throws Exception {
        logRequest("GET /api/books/reports/sales");

        Map<String, Object> report = new HashMap<>();
        List<Book> books = bookService.findAllBooks();

        // Complex business logic in controller
        double totalRevenue = 0;
        int totalBooks = books.size();
        Map<String, Integer> authorCounts = new HashMap<>();

        for (Book book : books) {
            totalRevenue += book.getPrice() * (book.getViewCount() * 0.1); // Assumed conversion rate

            String author = book.getAuthor();
            authorCounts.put(author, authorCounts.getOrDefault(author, 0) + 1);
        }

        report.put("totalRevenue", totalRevenue);
        report.put("totalBooks", totalBooks);
        report.put("averagePrice", totalRevenue / totalBooks);
        report.put("topAuthors", authorCounts);

        return report;
    }

    // Hardcoded logging method
    private void logRequest(String request) {
        try {
            FileWriter fw = new FileWriter("requests.log", true);
            fw.write(new Date() + " - " + request + "\n");
            fw.close();
        } catch (IOException e) {
            // Swallowing exceptions
            System.err.println("Failed to log request");
        }
    }

    // Hardcoded email method
    private void sendEmailNotification(String message) {
        // Simulated email sending with hardcoded SMTP
        System.out.println("Sending email: " + message);
        try {
            Thread.sleep(100); // Simulated delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
