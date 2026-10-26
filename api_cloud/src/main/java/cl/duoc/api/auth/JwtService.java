package cl.duoc.api.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    @Value("${security.jwt.secret:changeme}")
    private String secret;

    private byte[] getSecretBytes() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            return padded;
        }
        return bytes;
    }

    public String generateToken(User user) {
        try {
            // BASELINE-VULN [SEC-REQ-003]: duracion de sesion excesiva (30 dias) sin renovacion
            Instant now = Instant.now();
            Instant expiration = now.plus(30, ChronoUnit.DAYS);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(user.getId()))
                    .claim("email", user.getEmail())
                    .claim("role", user.getRole())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiration))
                    .build();

            MACSigner signer = new MACSigner(getSecretBytes());
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar token JWT", e);
        }
    }

    public JWTClaimsSet parseAndValidateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            MACVerifier verifier = new MACVerifier(getSecretBytes());
            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Firma JWT no valida");
            }
            return signedJWT.getJWTClaimsSet();
        } catch (Exception e) {
            throw new RuntimeException("Token invalido: " + e.getMessage(), e);
        }
    }
}
