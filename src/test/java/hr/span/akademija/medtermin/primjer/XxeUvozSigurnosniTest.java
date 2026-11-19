package hr.span.akademija.medtermin.primjer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import hr.span.akademija.medtermin.ApiTestBaza;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.TestPropertySource;

/** Sigurnosni ugovor: XML s DOCTYPE se odbija, obican XML ostaje podrzan. */
@Tag("sigurnosno")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:xxe-sigurnosni;DB_CLOSE_DELAY=-1")
class XxeUvozSigurnosniTest extends ApiTestBaza {

    @TempDir
    Path privremenaMapa;

    @Test
    @DisplayName("XML s vanjskim entitetom vraca 400, ne otkriva datoteku i ne upisuje termin")
    void odbijaVanjskiEntitetBezUpisaTermina() throws Exception {
        String oznaka = "XXE_TEST_" + UUID.randomUUID();
        Path datoteka = privremenaMapa.resolve("xxe-proba.txt");
        Files.writeString(datoteka, oznaka, StandardCharsets.UTF_8);
        String token = prijava("dr.horvat", "Ovlasteni12345!");
        Odgovor prije = get("/api/termini", token);
        assertThat(prije.status()).isEqualTo(200);
        assertThat(prije.json().isArray()).isTrue();

        // U zahtjev ide samo URI datoteke; njezin sadrzaj moze procitati samo posluzitelj.
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE termini [
                  <!ENTITY proba SYSTEM "%s">
                ]>
                <termini>
                  <termin>
                    <lijecnikId>1</lijecnikId>
                    <pocetak>2026-12-01T09:00</pocetak>
                    <kapacitet>1</kapacitet>
                    <napomena>&proba;</napomena>
                  </termin>
                </termini>
                """.formatted(datoteka.toUri().toASCIIString().replace("&", "&amp;"));

        Odgovor uvoz = posalji("POST", "/api/termini/uvoz", token, "application/xml", xml);
        assertThat(uvoz.status())
                .as("XML s DOCTYPE mora biti odbijen; odgovor: %s", uvoz.tijelo())
                .isEqualTo(400);
        assertThat(uvoz.tijelo()).doesNotContain(oznaka);

        Odgovor poslije = get("/api/termini", token);
        assertThat(poslije.status()).isEqualTo(200);
        assertThat(poslije.json()).as("Odbijeni uvoz ne smije promijeniti popis termina")
                .isEqualTo(prije.json());
        assertThat(poslije.tijelo()).doesNotContain(oznaka);
    }

    @Test
    @DisplayName("Obican XML se uvozi i termin je vidljiv pod vracenim ID-jem")
    void obicanXmlIDaljeRadi() {
        String token = prijava("dr.horvat", "Ovlasteni12345!");
        String oznaka = "SIGURAN_XML_" + UUID.randomUUID();
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <termini>
                  <termin>
                    <lijecnikId>1</lijecnikId>
                    <pocetak>2026-12-01T09:00</pocetak>
                    <kapacitet>1</kapacitet>
                    <napomena>%s</napomena>
                  </termin>
                </termini>
                """.formatted(oznaka);
        Odgovor uvoz = posalji("POST", "/api/termini/uvoz", token, "application/xml", xml);
        assertThat(uvoz.status()).as("Status uvoza; odgovor: %s", uvoz.tijelo()).isEqualTo(200);
        assertThat(uvoz.json().path("uvezeno").asInt()).isEqualTo(1);
        JsonNode termini = uvoz.json().path("termini");
        assertThat(termini.isArray()).isTrue();
        assertThat(termini.size()).isEqualTo(1);
        JsonNode uvezeni = termini.get(0);
        long id = uvezeni.path("id").asLong();
        assertThat(id).isPositive();
        assertThat(uvezeni.path("napomena").asText())
                .as("Obicna napomena mora biti sacuvana")
                .isEqualTo(oznaka);

        Odgovor popis = get("/api/termini", token);
        assertThat(popis.status()).as("Odgovor popisa: %s", popis.tijelo()).isEqualTo(200);
        assertThat(popis.json().isArray()).isTrue();
        JsonNode pronadeni = null;
        for (JsonNode termin : popis.json()) {
            if (termin.path("id").asLong() == id) {
                pronadeni = termin;
                break;
            }
        }
        assertThat(pronadeni).as("Uvezeni termin ID=%s mora postojati u GET /api/termini", id).isNotNull();
        assertThat(pronadeni.path("napomena").asText()).isEqualTo(oznaka);
        assertThat(pronadeni.path("lijecnikId").asLong()).isEqualTo(1);
    }
}
