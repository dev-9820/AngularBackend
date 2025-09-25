package com.quizapp.controllers;

import com.quizapp.models.Quiz;
import com.quizapp.models.User;
import com.quizapp.repositories.QuizRepository;
import com.quizapp.repositories.UserRepository;
import com.quizapp.services.MailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = "http://localhost:5173")
public class QuizController {
    @Autowired private QuizRepository quizRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private MailService mailService;

    @PostMapping("")
    public ResponseEntity<?> createQuiz(@RequestBody Quiz payload, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        payload.setCreatedBy(userId);
        Quiz saved = quizRepo.save(payload);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping("")
    public ResponseEntity<?> getQuizzes(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        List<Quiz> quizzes = quizRepo.findAll();
        Optional<User> ou = userRepo.findById(userId);
        List<Map<String,Object>> out = new ArrayList<>();
        for (Quiz q : quizzes) {
            boolean hasAttempted = false;
            if (ou.isPresent()) {
                for (User.Score s : ou.get().getScores()) {
                    if (s.getQuizId().equals(q.getId())) { hasAttempted = true; break; }
                }
            }
            Map<String,Object> m = new HashMap<>();
            m.put("id", q.getId()); m.put("title", q.getTitle()); m.put("difficulty", q.getDifficulty()); m.put("timeLimit", q.getTimeLimit()); m.put("questions", q.getQuestions());
            m.put("hasAttempted", hasAttempted);
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getQuizById(@PathVariable String id) {
        Optional<Quiz> oq = quizRepo.findById(id);
        if (oq.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","Quiz not found"));
        return ResponseEntity.ok(oq.get());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuiz(@PathVariable String id, @RequestBody Map<String,Object> body) {
        Optional<Quiz> oq = quizRepo.findById(id);
        if (oq.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","Quiz not found"));
        Quiz q = oq.get();
        if (body.containsKey("title")) q.setTitle((String) body.get("title"));
        if (body.containsKey("questions")) q.setQuestions((List<com.quizapp.models.Quiz.Question>) body.get("questions"));
        if (body.containsKey("timeLimit")) q.setTimeLimit((Integer) body.get("timeLimit"));
        quizRepo.save(q);
        return ResponseEntity.ok(q);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable String id) {
        Optional<Quiz> oq = quizRepo.findById(id);
        if (oq.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","Quiz not found"));
        quizRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Quiz deleted successfully"));
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<?> getLeaderboard(@PathVariable String id) {
        List<User> users = userRepo.findAll();
        List<Map<String,Object>> list = new ArrayList<>();
        for (User u : users) {
            for (User.Score s : u.getScores()) {
                if (id.equals(s.getQuizId())) {
                    list.add(Map.of("username", u.getUsername(), "score", s.getScore()));
                }
            }
        }
        list.sort((a,b) -> ((Integer)b.get("score")).compareTo((Integer)a.get("score")));
        if (list.size() > 10) list = list.subList(0,10);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestParam("quizId") String quizId,
                                        @RequestParam("score") Integer score,
                                        @RequestPart(value = "pdf", required = false) MultipartFile pdf,
                                        HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message","Unauthorized"));
        Optional<User> ou = userRepo.findById(userId);
        if (ou.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","User not found"));
        User user = ou.get();
        Optional<Quiz> qOpt = quizRepo.findById(quizId);
        if (qOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","Quiz not found"));

        byte[] pdfBytes = null;
        try { if (pdf != null && !pdf.isEmpty()) pdfBytes = pdf.getBytes(); } catch (Exception ignored) {}

        boolean updated = false;
        for (User.Score s : user.getScores()) {
            if (s.getQuizId().equals(quizId)) { s.setScore(score); if (pdfBytes != null) s.setPdf(pdfBytes); updated = true; break; }
        }
        if (!updated) { User.Score ns = new User.Score(); ns.setQuizId(quizId); ns.setScore(score); ns.setPdf(pdfBytes); user.getScores().add(ns); }
        userRepo.save(user);

        try { mailService.sendSimpleMail(user.getEmail(), "Your Quiz Result for " + qOpt.get().getTitle(), "Congrats " + user.getUsername() + ", you scored " + score); } catch (Exception ignored) {}

        return ResponseEntity.status(201).body(Map.of("message","Quiz submitted and result emailed successfully"));
    }
}
