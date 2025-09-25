package com.quizapp.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "quizzes")
public class Quiz {
    @Id
    private String id;
    private String title;
    private String difficulty;
    private List<Question> questions;
    private Integer timeLimit;
    private String createdBy;

    @Data
    public static class Question {
        private String question;
        private List<String> options;
        private String correctAnswer;
    }
}
