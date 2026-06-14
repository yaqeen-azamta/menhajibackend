package com.springboot.manhaji.dto.adaptive;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** One student answer inside AdaptiveSubmitRequest. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveAnswerItem {

    /** Matches AdaptiveQuizItem.index from the GET response. */
    @NotNull(message = "questionIndex is required")
    @Min(value = 0, message = "questionIndex must be >= 0")
    private Integer questionIndex;

    /** Text answer (MCQ option text, 'صح'/'خطأ', or typed short answer). */
    private String answer;

    /** Transcribed speech — used instead of answer for voice SHORT_ANSWER questions. */
    private String spokenText;
}
