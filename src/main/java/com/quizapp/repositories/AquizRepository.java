package com.quizapp.repositories;

import com.quizapp.models.Aquiz;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AquizRepository extends MongoRepository<Aquiz, String> {}
