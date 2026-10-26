package cl.duoc.api.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // BASELINE-VULN [SEC-REQ-006]: el registro recibe la entidad User directamente permitiendo asignar roles arbitrarios
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("CLIENTE");
        }
        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        User savedUser = userRepository.save(user);
        // BASELINE-VULN [SEC-REQ-007]: la respuesta del registro expone la entidad User completa incluyendo el passwordHash
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        String password = loginRequest.getPassword() != null ? loginRequest.getPassword() : loginRequest.getPasswordHash();

        if (userOpt.isEmpty() || password == null || !passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
            // BASELINE-VULN [SEC-REQ-001]: falta de bloqueo por intentos fallidos de autenticacion
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales invalidas"));
        }

        User user = userOpt.get();
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", user.getRole()
        ));
    }

    // BASELINE-VULN [SEC-REQ-007]: el endpoint /me expone la entidad User completa incluyendo el passwordHash
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                var claims = jwtService.parseAndValidateToken(token);
                Long userId = Long.parseLong(claims.getSubject());
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    return ResponseEntity.ok(userOpt.get());
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token invalido o expirado"));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Cabecera Authorization no proporcionada"));
    }

    public static class LoginRequest {
        private String email;
        @JsonAlias({"passwordHash", "clave"})
        private String password;
        private String passwordHash;

        public LoginRequest() {
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }
    }
}
