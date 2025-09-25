package com.quizapp.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String email;
    private String password;
    private String role = "user";
    private List<Score> scores = new ArrayList<>();
    private List<AScore> ascores = new ArrayList<>();

    @Data
    public static class Score {
        private String quizId;
        private Integer score;
        private byte[] pdf;
    }

    @Data
    public static class AScore {
        private String quizId;
        private Integer score;
    }
}
