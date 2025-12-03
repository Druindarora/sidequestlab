package dev.sidequestlab.backend.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final JdbcTemplate jdbcTemplate;

    public ProfileController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/me")
    public ProfileDto me() {
        // On va chercher le full_name du profil id 1
        String dbFullName = jdbcTemplate.queryForObject(
            "SELECT full_name FROM profile WHERE id_profile = 1",
            String.class
        );

        // Juste pour bien voir que ça vient de la DB
        String fullName = (dbFullName != null)
                ? dbFullName
                : "Unknown from backend";

        return new ProfileDto(
            fullName,
            "Développeur Java / Angular",
            "Résumé rapide de qui je suis."
        );
    }
}
