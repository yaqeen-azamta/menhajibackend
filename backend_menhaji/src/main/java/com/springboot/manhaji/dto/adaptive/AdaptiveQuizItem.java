package com.springboot.manhaji.dto.adaptive;

import lombok.*;

import java.util.List;

/**
 * One question returned to the client.
 * correctAnswer is intentionally absent — leaking it would trivially let clients cheat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveQuizItem {

    /** 0-based index — the client sends this back when submitting answers. */
    private int    index;
    private String type;
    private String questionText;
    private List<String> options;   // null for SHORT_ANSWER
    private String subSkill;
    private int    difficultyLevel;
}
