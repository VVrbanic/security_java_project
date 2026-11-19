package hr.span.akademija.medtermin.primjer;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

public class NalaziControllerTest extends ApiTestBaza {

    @Test
    @DisplayName("Neispravan format ne smije zaobići provjeru vlasništva nalaza")
    void neispravanFormatNeOtkrivaTudiNalaz() {
        String token = prijava("ana", "Ana12345!");

        // Kontrola: Markov nalaz nije dostupan Ani uz valjan format.
        assertThat(get("/api/nalazi/2?format=json", token).status())
                .isEqualTo(404);

        // Promjena formata ne smije promijeniti prava pristupa.
        Odgovor odgovor = get("/api/nalazi/2?format=xyz", token);

        assertThat(odgovor.status())
                .as("Tuđi nalaz mora ostati nedostupan i uz neispravan format")
                .isEqualTo(404);
    }
}
