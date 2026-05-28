package com.springboot.manhaji.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardEntryResponse {
    private int rank;
    private Long studentId;
    private String studentName;
    private String avatarId;
    private int totalPoints;
    private int completedLessons;
    private boolean isCurrentUser;
}
