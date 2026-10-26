package cl.duoc.api.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            String encodedPassword = passwordEncoder.encode("Test1234!");
            userRepository.save(new User(null, "admin@cafe.test", encodedPassword, "Administrador Cafe", "+56911111111", "ADMIN"));
            userRepository.save(new User(null, "cliente1@cafe.test", encodedPassword, "Cliente Uno", "+56922222222", "CLIENTE"));
            userRepository.save(new User(null, "cliente2@cafe.test", encodedPassword, "Cliente Dos", "+56933333333", "CLIENTE"));
        }
    }
}
