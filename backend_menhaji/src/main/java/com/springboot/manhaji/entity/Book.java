package com.springboot.manhaji.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Integer bookId;

    @Column(nullable = false, length = 50)
    private String subject;

    @Column(nullable = false)
    private Integer grade;

    @Column(nullable = false)
    private Integer semester;

    @Column(name = "book_name", nullable = false, length = 200)
    private String bookName;

    @Column
    private Integer year;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "cover_image")
    private String coverImage;
}
