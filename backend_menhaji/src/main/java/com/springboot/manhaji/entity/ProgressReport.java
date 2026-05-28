package com.springboot.manhaji.entity;

import com.springboot.manhaji.entity.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "progress_reports",
        indexes = {
                @Index(name = "idx_report_student", columnList = "student_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgressReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column
    private RiskLevel riskLevel;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @PrePersist
    protected void onCreate() {
        this.generatedAt = LocalDateTime.now();
    }
}
