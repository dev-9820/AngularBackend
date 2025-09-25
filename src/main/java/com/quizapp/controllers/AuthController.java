package com.quizapp.controllers;

import com.quizapp.models.User;
import com.quizapp.models.Quiz;
import com.quizapp.models.Aquiz;
import com.quizapp.repositories.UserRepository;
import com.quizapp.repositories.QuizRepository;
import com.quizapp.repositories.AquizRepository;
import com.quizapp.services.JwtUtil;
import com.quizapp.services.MailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    @Autowired private UserRepository userRepo;
    @Autowired private QuizRepository quizRepo;
    @Autowired private AquizRepository aquizRepo;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private MailService mailService;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<?> signup(@RequestBody User payload) {
        if (payload.getUsername() == null || payload.getPassword() == null || payload.getEmail() == null)
            return ResponseEntity.badRequest().body(Map.of("message", "username, email and password required"));

        if (userRepo.findByUsername(payload.getUsername()).isPresent())
            return ResponseEntity.badRequest().body(Map.of("message", "username already exists"));

        payload.setPassword(passwordEncoder.encode(payload.getPassword()));
        User saved = userRepo.save(payload);
        return ResponseEntity.status(201).body(Map.of("message", "User created.", "id", saved.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        Optional<User> ou = userRepo.findByUsername(username);
        if (ou.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","User not found"));
        User user = ou.get();
        if (!passwordEncoder.matches(password, user.getPassword()))
            return ResponseEntity.status(401).body(Map.of("message","Invalid password"));

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return ResponseEntity.ok(Map.of("token", token, "role", user.getRole(), "user", user));
    }

    @GetMapping("/students")
    public ResponseEntity<?> getStudents() {
        List<User> users = userRepo.findAll();
        List<Map<String, Object>> students = new ArrayList<>();
        for (User u : users) {
            if ("user".equals(u.getRole())) {
                Map<String,Object> m = new HashMap<>();
                m.put("username", u.getUsername());
                m.put("_id", u.getId());
                m.put("scores", u.getScores());
                students.add(m);
            }
        }
        if (students.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","No users found"));
        return ResponseEntity.ok(students);
    }

    @GetMapping("/profile")
public ResponseEntity<?> getProfile(HttpServletRequest request) {
    String userId = (String) request.getAttribute("userId");
    if (userId == null) return ResponseEntity.status(401).body(Map.of("message","Unauthorized"));
    Optional<User> ou = userRepo.findById(userId);
    if (ou.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","User not found"));
    User u = ou.get();

    List<Map<String,Object>> formattedScores = new ArrayList<>();
    for (User.Score s : u.getScores()) {
        Map<String,Object> e = new HashMap<>();
        e.put("quizId", s.getQuizId());
        e.put("score", s.getScore());
        e.put("pdf", s.getPdf() != null ? Base64.getEncoder().encodeToString(s.getPdf()) : null);

        // Try to fetch quiz details from both collections
        quizRepo.findById(s.getQuizId()).ifPresent(quiz -> {
            e.put("quizTitle", quiz.getTitle());
            // add more quiz fields as needed
        });

        aquizRepo.findById(s.getQuizId()).ifPresent(aquiz -> {
            e.put("quizTitle", aquiz.getTitle());
            // add more aquiz fields as needed
        });

        formattedScores.add(e);
    }

    return ResponseEntity.ok(Map.of(
            "username", u.getUsername(),
            "scores", formattedScores
    ));
}

    @PostMapping(value = "/score", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitScore(
            @RequestParam("quizId") String quizId,
            @RequestParam("score") Integer score,
            @RequestPart(value = "pdf", required = false) MultipartFile pdf,
            HttpServletRequest request
    ) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message","Unauthorized"));

        Optional<User> ou = userRepo.findById(userId);
        if (ou.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","User not found"));
        User user = ou.get();

        Optional<Quiz> quiz = quizRepo.findById(quizId);
        Optional<Aquiz> aquiz = aquizRepo.findById(quizId);
        if (quiz.isEmpty() && aquiz.isEmpty()) return ResponseEntity.status(404).body(Map.of("message","Quiz not found"));

        byte[] pdfBytes = null;
        try { if (pdf != null && !pdf.isEmpty()) pdfBytes = pdf.getBytes(); } catch (Exception ignored) {}

        boolean updated = false;
        for (User.Score s : user.getScores()) {
            if (s.getQuizId().equals(quizId)) {
                s.setScore(score);
                if (pdfBytes != null) s.setPdf(pdfBytes);
                updated = true; break;
            }
        }
        if (!updated) {
            User.Score ns = new User.Score();
            ns.setQuizId(quizId);
            ns.setScore(score);
            ns.setPdf(pdfBytes);
            user.getScores().add(ns);
        }
        userRepo.save(user);

        // send email (best-effort)
        try {
            String title = quiz.isPresent() ? quiz.get().getTitle() : aquiz.get().getTitle();
            mailService.sendSimpleMail(
    user.getEmail(),
    "Your Quiz Result for " + title,
    "Hello " + user.getUsername() + ",\n\n" +
    "You completed the quiz \"" + title + "\" and scored " + score + " points!\n\n" +
    "Best regards,\nQuizApp Team"
);
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Map.of("message","Score submitted successfully", "score", score));
    }
}
