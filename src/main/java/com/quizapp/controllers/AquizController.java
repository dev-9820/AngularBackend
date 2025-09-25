package com.quizapp.controllers;

import com.quizapp.models.Aquiz;
import com.quizapp.models.User;
import com.quizapp.repositories.AquizRepository;
import com.quizapp.repositories.UserRepository;
import com.quizapp.services.MailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/aquizzes")
@CrossOrigin(origins = "http://localhost:5173")
public class AquizController {
    @Autowired private AquizRepository aquizRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private MailService mailService;

    @PostMapping("/a")
    public ResponseEntity<?> create(@RequestBody Aquiz payload, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        payload.setCreatedBy(userId);
        Aquiz saved = aquizRepo.save(payload);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping("/a")
    public ResponseEntity<?> getAll(HttpServletRequest request) {
        List<Aquiz> quizzes = aquizRepo.findAll();
        String userId = (String) request.getAttribute("userId");
        Optional<User> ou = userRepo.findById(userId);
        List<Map<String,Object>> out = new ArrayList<>();
        for (Aquiz q : quizzes) {
            boolean hasAttempted = false;
            if (ou.isPresent()) {
                for (User.AScore s : ou.get().getAscores()) {
                    if (s.getQuizId().equals(q.getId())) { hasAttempted = true; break; }
                }
            }
            Map<String,Object> m = new HashMap<>();
            m.put("id", q.getId()); m.put("title", q.getTitle()); m.put("difficulty", q.getDifficulty()); m.put("timeLimit", q.getTimeLimit());
            m.put("hasAttempted", hasAttempted);
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/a/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        Optional<Aquiz> o = aquizRepo.findById(id);
        if (o.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","Quiz not found"));
        return ResponseEntity.ok(o.get());
    }

    @DeleteMapping("/a/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        Optional<Aquiz> o = aquizRepo.findById(id);
        if (o.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","Quiz not found"));
        aquizRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Quiz deleted successfully"));
    }

    @PostMapping("/a/submit")
    public ResponseEntity<?> submit(@RequestParam String quizId, @RequestParam Integer score, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message","Unauthorized"));
        Optional<User> ou = userRepo.findById(userId);
        if (ou.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","User not found"));
        Optional<Aquiz> aq = aquizRepo.findById(quizId);
        if (aq.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","Quiz not found"));
        User user = ou.get();
        boolean updated = false;
        for (User.AScore s : user.getAscores()) {
            if (s.getQuizId().equals(quizId)) { s.setScore(score); updated = true; break; }
        }
        if (!updated) { User.AScore ns = new User.AScore(); ns.setQuizId(quizId); ns.setScore(score); user.getAscores().add(ns); }
        userRepo.save(user);
        try { mailService.sendSimpleMail(user.getEmail(), "Your Quiz Result for " + aq.get().getTitle(), "Congrats " + user.getUsername() + ", you scored " + score); } catch (Exception ignored) {}
        return ResponseEntity.status(201).body(Map.of("message","Quiz submitted and result emailed successfully"));
    }

    @GetMapping("/a/{id}/leaderboard")
    public ResponseEntity<?> leaderboard(@PathVariable String id) {
        List<User> users = userRepo.findAll();
        List<Map<String,Object>> out = new ArrayList<>();
        for (User u : users) {
            for (User.AScore s : u.getAscores()) {
                if (id.equals(s.getQuizId())) out.add(Map.of("username", u.getUsername(), "score", s.getScore()));
            }
        }
        out.sort((a,b) -> ((Integer)b.get("score")).compareTo((Integer)a.get("score")));
        if (out.size() > 10) out = out.subList(0,10);
        return ResponseEntity.ok(out);
    }
}
