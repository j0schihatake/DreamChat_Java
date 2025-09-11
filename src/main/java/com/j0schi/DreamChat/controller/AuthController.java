package com.j0schi.DreamChat.controller;

import com.j0schi.DreamChat.model.AuthRequest;
import com.j0schi.DreamChat.model.AuthResponse;
import com.j0schi.DreamChat.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/request")
    public ResponseEntity<AuthResponse> requestAuth(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.handleAuthRequest(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<AuthResponse> checkAuthStatus(@RequestParam String phoneNumber) {
        try {
            AuthResponse response = authService.checkAuthStatus(phoneNumber);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, e.getMessage()));
        }
    }
}
