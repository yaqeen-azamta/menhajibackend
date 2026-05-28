package com.springboot.manhaji.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parents")
@Getter
@Setter
@NoArgsConstructor
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String parentName;

    @Column
    private String phone;
}
