package hr.span.akademija.medtermin.platforma;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/** Zaboravljena lozinka: token se šalje e-poštom, a njime se postavlja nova lozinka. */
@Service
public class ResetLozinkeService {

    private final JdbcTemplate jdbc;
    private final KorisnikService korisnici;
    private final PostaService posta;
    private final SecureRandom random = new SecureRandom();

    public ResetLozinkeService(JdbcTemplate jdbc, KorisnikService korisnici, PostaService posta) {
        this.jdbc = jdbc;
        this.korisnici = korisnici;
        this.posta = posta;
    }

    public void zatrazi(String username) {
        Optional<KorisnikService.Korisnik> korisnik = korisnici.pronadi(username);
        if (korisnik.isEmpty()) {
            return;
        }
        //dodano da se upisuje iskoristenost i kada istjece
        String token = generirajToken();
        LocalDateTime istjece = LocalDateTime.now().plusMinutes(15);
        jdbc.update( """
                INSERT INTO reset_tokeni (username, token, istjece, iskoristen)
                VALUES (?, ?, ?, FALSE)
                """,
                username,
                token,
                istjece);
        posta.posaljiUputeZaLozinku(korisnik.get(), token, istjece);
    }

    @Transactional
    public boolean postaviNovuLozinku(String token, String novaLozinka) {
        KorisnikService.provjeriLozinku(novaLozinka);
        if (token == null || token.isBlank()) {
            return false;
        }

        String vrijednostTokena = token.trim();

        List<String> vlasnici = jdbc.queryForList(
                "SELECT username FROM reset_tokeni WHERE token = ?", String.class, token.trim());
        if (vlasnici.isEmpty()) {
            return false;
        }
        //dodano da se provjera da se upisuje samo kada nije istekla i kada nije iskoristena
        int promijenjeno = jdbc.update(
                """
                UPDATE reset_tokeni
                SET iskoristen = TRUE
                WHERE token = ?
                  AND iskoristen = FALSE
                  AND istjece > ?
                """,
                vrijednostTokena,
                LocalDateTime.now()
        );

        if (promijenjeno == 0) {
            return false;
        }
        korisnici.promijeniLozinku(vlasnici.get(0), novaLozinka);
        return true;
    }

    private String generirajToken() {
        byte[] bajtovi = new byte[32];
        random.nextBytes(bajtovi);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bajtovi);
    }

}
