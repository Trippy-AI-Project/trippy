package pse.trippy.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.userservice.model.dto.LoginRequest;
import pse.trippy.userservice.model.dto.LoginResponse;
import pse.trippy.userservice.model.dto.RefreshTokenRequest;
import pse.trippy.userservice.model.dto.TokenResponse;
import pse.trippy.userservice.service.AuthService;

/**
 * REST controller for authentication endpoints.
 *
 * <p>All endpoints under {@code /auth} are public (no JWT required).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Authenticates the user and returns an access/refresh token pair. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /** Rotates the refresh token and returns a new access/refresh token pair. */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}
