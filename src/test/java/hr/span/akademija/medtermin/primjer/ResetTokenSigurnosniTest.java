package hr.span.akademija.medtermin.primjer;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class ResetTokenSigurnosniTest extends ApiTestBaza {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Reset token smije se iskoristiti samo jednom")
    //vezano za ranjivost
    void iskoristeniTokenMoraBitiOdbijen() {
        String username = noviKorisnik("jednom", "PocetnaLozinka123!");
        String token = zatraziToken(username);

        Odgovor prviReset = reset(token, "PrvaNovaLozinka123!");
        assertThat(prviReset.status()).isEqualTo(200);

        // Isti token, bez novog zahtjeva za reset.
        Odgovor drugiReset = reset(token, "DrugaNovaLozinka123!");

        assertThat(drugiReset.status())
                .as("Već iskorišten token mora biti odbijen")
                .isEqualTo(400);

        // Drugi pokušaj nije smio promijeniti lozinku.
        assertThat(prijava(username, "PrvaNovaLozinka123!"))
                .isNotBlank();
    }

    @Test
    @DisplayName("Istekli reset token ne smije promijeniti lozinku")
    void istekliTokenMoraBitiOdbijen() {
        String username = noviKorisnik("istek", "PocetnaLozinka123!");
        String token = zatraziToken(username);

        // Simuliramo istek bez čekanja i bez korištenja tokena.
        int promijenjeno = jdbc.update(
                "UPDATE reset_tokeni SET istjece = ? WHERE username = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(1)),
                username
        );
        assertThat(promijenjeno).isEqualTo(1);

        Odgovor odgovor = reset(token, "NovaLozinka123!");

        assertThat(odgovor.status())
                .as("Istekli token mora biti odbijen")
                .isEqualTo(400);

        // Izvorna lozinka mora ostati važeća.
        assertThat(prijava(username, "PocetnaLozinka123!"))
                .isNotBlank();
    }

    private String zatraziToken(String username) {
        Odgovor odgovor = postJson(
                "/api/auth/reset-zahtjev",
                null,
                Map.of("username", username)
        );
        assertThat(odgovor.status()).isEqualTo(202);

        String token = jdbc.queryForObject(
                "SELECT token FROM reset_tokeni WHERE username = ?",
                String.class,
                username
        );
        assertThat(token).isNotBlank();
        return token;
    }

    private Odgovor reset(String token, String novaLozinka) {
        return postJson(
                "/api/auth/reset",
                null,
                Map.of("token", token, "novaLozinka", novaLozinka)
        );
    }
}