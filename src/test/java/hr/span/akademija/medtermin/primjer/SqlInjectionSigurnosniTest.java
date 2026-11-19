package hr.span.akademija.medtermin.primjer;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SqlInjectionSigurnosniTest extends ApiTestBaza {

    @Test
    void odbijaSqlIzrazeUObaParametraSortiranja() {
        String token = prijava("ana", "Ana12345!");
        for (String unos : new String[] {"prezime, id", "(SELECT 1)", "prezime--", "' OR '1'='1"}) {
            assertThat(get("/api/lijecnici?sort=" + enc(unos), token).status())
                    .as("sort=%s", unos).isEqualTo(400);
        }
        for (String unos : new String[] {"asc, id", "desc--", "asc; SELECT 1", "' OR '1'='1"}) {
            assertThat(get("/api/lijecnici?smjer=" + enc(unos), token).status())
                    .as("smjer=%s", unos).isEqualTo(400);
        }
        Odgovor odgovor = get("/api/lijecnici?sort=prezime&smjer=asc", token);
        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.json().size()).isEqualTo(6);
    }

    @Test
    void sqlUnosNeZaobilaziPrijavu() {
        for (String unos : new String[] {"ana'--", "' OR '1'='1'--", "ana' OR 1=1--"}) {
            Odgovor odgovor = postJson("/api/auth/login", null,
                    Map.of("username", unos, "password", "Ana12345!"));
            assertThat(odgovor.status()).as("username=%s", unos).isEqualTo(401);
            assertThat(odgovor.json().has("token")).isFalse();
        }
        Odgovor odgovor = postJson("/api/auth/login", null,
                Map.of("username", "ana", "password", "' OR '1'='1'--"));
        assertThat(odgovor.status()).isEqualTo(401);
        assertThat(odgovor.json().has("token")).isFalse();
        assertThat(prijava("ana", "Ana12345!")).isNotBlank();
    }
}
