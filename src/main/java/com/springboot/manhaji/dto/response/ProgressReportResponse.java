package com.springboot.manhaji.dto.response;

import com.springboot.manhaji.entity.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressReportResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String summary;
    private RiskLevel riskLevel;
    private LocalDateTime generatedAt;
}
