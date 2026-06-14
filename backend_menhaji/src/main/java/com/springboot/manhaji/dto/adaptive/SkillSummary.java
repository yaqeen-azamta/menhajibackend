package com.springboot.manhaji.dto.adaptive;

import lombok.*;

/** Snapshot of a student's performance on one sub-skill. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillSummary {

    private String subSkill;
    private double accuracy;        // 0–100
    private int    totalAttempts;
    private int    currentDifficulty; // 1–5
}
