package com.quizapp.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "aquizzes")
public class Aquiz {
    @Id
    private String id;
    private String title;
    private String difficulty;
    private Integer timeLimit;
    private String createdBy;
}
