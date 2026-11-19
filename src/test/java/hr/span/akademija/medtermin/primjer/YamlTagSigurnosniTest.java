package hr.span.akademija.medtermin.primjer;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import org.junit.jupiter.api.Test;

class YamlTagSigurnosniTest extends ApiTestBaza {

    @Test
    void odbijaJavaTipUPostavkama() {
        String token = prijava("ana", "Ana12345!");
        Odgovor odgovor = posalji("POST", "/api/postavke", token, "application/yaml",
                "podsjetnik: true\nsatiPrije: 24\nkanal: !!java.lang.String email\n");

        assertThat(odgovor.status()).isEqualTo(400);
    }
}
