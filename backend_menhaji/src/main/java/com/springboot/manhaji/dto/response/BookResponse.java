package com.springboot.manhaji.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    @JsonProperty("book_id")
    private Integer bookId;

    private String subject;

    @JsonProperty("book_name")
    private String bookName;

    @JsonProperty("cover_image")
    private String coverImage;

    private Integer grade;

    private Integer semester;
}
