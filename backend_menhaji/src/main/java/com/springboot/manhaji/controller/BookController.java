package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.BookResponse;
import com.springboot.manhaji.entity.Book;
import com.springboot.manhaji.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
public class BookController {

    private final BookRepository bookRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookResponse>>> getAllBooks() {
        List<Book> books = bookRepository.findByIsActiveTrue();

        log.debug("[Books] Total books returned: {}", books.size());
        books.forEach(b -> log.debug("[Books] id={}, subject={}, book_name={}, cover_image={}",
                b.getBookId(), b.getSubject(), b.getBookName(), b.getCoverImage()));

        List<BookResponse> response = mapToResponse(books);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/grade/{grade}")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getBooksByGrade(@PathVariable Integer grade) {
        List<Book> books = bookRepository.findByGradeAndIsActiveTrue(grade);

        log.debug("[Books] Grade {} — total books returned: {}", grade, books.size());
        books.forEach(b -> log.debug("[Books] id={}, subject={}, book_name={}, cover_image={}",
                b.getBookId(), b.getSubject(), b.getBookName(), b.getCoverImage()));

        // Warn on any null cover_image
        books.stream()
                .filter(b -> b.getCoverImage() == null || b.getCoverImage().isBlank())
                .forEach(b -> log.warn("[Books] book_id={} ('{}') has NULL/empty cover_image", b.getBookId(), b.getBookName()));

        List<BookResponse> response = mapToResponse(books);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private List<BookResponse> mapToResponse(List<Book> books) {
        return books.stream()
                .map(b -> BookResponse.builder()
                        .bookId(b.getBookId())
                        .subject(b.getSubject())
                        .bookName(b.getBookName())
                        .coverImage(b.getCoverImage())
                        .grade(b.getGrade())
                        .semester(b.getSemester())
                        .build())
                .toList();
    }
}
