package com.springboot.manhaji.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProgressSummaryResponse {
    private int totalLessons;
    private int completedLessons;
    private int masteredLessons;
    private int inProgressLessons;
    private double overallMastery;     // Average mastery across all lessons
    private int totalPoints;
    private int currentStreak;
    private int totalQuizzesTaken;
    private double averageQuizScore;
    private List<SubjectProgressResponse> subjectProgress;
    private List<RecentActivityResponse> recentActivity;
}
