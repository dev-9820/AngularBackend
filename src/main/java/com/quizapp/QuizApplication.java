package com.quizapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class QuizApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(QuizApplication.class, args);
        System.out.println("Mongo URI: " + ctx.getEnvironment().getProperty("spring.data.mongodb.uri"));
    }
}
