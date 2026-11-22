package hr.span.akademija.medtermin.primjer;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import org.junit.jupiter.api.Test;

class CorsSigurnosniTest extends ApiTestBaza {

    private static final String NEDOPUSTENI_ORIGIN = "https://zlo.example";

    @Test
    void nedopusteniOriginNeDobivaCORSZaglavlja() {
        Odgovor odgovor = posalji("GET", "/api/auth/me", null, null, null,
                "Origin", NEDOPUSTENI_ORIGIN);

        assertThat(odgovor.status()).isEqualTo(401);
        assertThat(odgovor.zaglavlje("Access-Control-Allow-Origin")).isNull();
        assertThat(odgovor.zaglavlje("Access-Control-Allow-Credentials")).isNull();
    }

    @Test
    void preflightSNedopustenogOriginaJeOdbijen() {
        Odgovor odgovor = posalji("OPTIONS", "/api/ja", null, null, null,
                "Origin", NEDOPUSTENI_ORIGIN,
                "Access-Control-Request-Method", "GET");

        assertThat(odgovor.status()).isEqualTo(403);
        assertThat(odgovor.zaglavlje("Access-Control-Allow-Origin")).isNull();
        assertThat(odgovor.zaglavlje("Access-Control-Allow-Credentials")).isNull();
    }

    @Test
    // Dokazuje da Spring Security odbija zahtjev bez sesije/tokena.
    void apiOdbijaNeprijavljenogKorisnika() {
        Odgovor odgovor = get("/api/auth/me", null);

        assertThat(odgovor.status()).isEqualTo(401);
    }
}
