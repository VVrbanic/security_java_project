package hr.span.akademija.medtermin.primjer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import hr.span.akademija.medtermin.ApiTestBaza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class RezervacijeControllerTest extends ApiTestBaza {

    /** Termin 7 je rezerviran za posebne provjere i ne koristi se u ovim testovima. */
    private static final long IZUZETI_TERMIN = 7;

    @Test
    @DisplayName("Ana ne smije obrisati Markovu rezervaciju")
    void neSmijeObrisatiTuduRezervaciju() {
        String markovToken = prijava("marko", "Marko12345!");
        String aninToken = prijava("ana", "Ana12345!");

        // Potvrđujemo da Markova rezervacija postoji prije pokušaja.
        Odgovor prije = get("/api/rezervacije/moje", markovToken);
        assertThat(prije.status()).isEqualTo(200);
        assertThat(prije.json().findValuesAsText("id")).contains("2");

        // Ana pokušava obrisati Markovu rezervaciju.
        Odgovor brisanje = delete("/api/rezervacije/2", aninToken);

        assertThat(brisanje.status())
                .as("Brisanje tuđe rezervacije mora biti odbijeno")
                .isIn(403, 404);

        // Rezervacija mora ostati dostupna vlasniku.
        Odgovor poslije = get("/api/rezervacije/moje", markovToken);
        assertThat(poslije.status()).isEqualTo(200);
        assertThat(poslije.json().findValuesAsText("id")).contains("2");
    }

    @Test
    @DisplayName("HTML potvrda prikazuje napomenu kao tekst, bez izvršivog HTML-a")
    void ispisKodiraNapomenu() {
        String token = prijava("ana", "Ana12345!");
        String skripta = "<script>alert('XSS')</script>";
        Odgovor rezervacija = rezerviraj(token, slobodanTermin(token), skripta);
        assertThat(rezervacija.status()).isEqualTo(201);

        long id = rezervacija.json().get("id").asLong();
        Odgovor potvrda = get("/ispis/rezervacije/" + id, token);

        assertThat(potvrda.status()).isEqualTo(200);
        assertThat(potvrda.tijelo())
                .contains("&lt;script&gt;alert(")
                .doesNotContain(skripta);
    }

    private Odgovor rezerviraj(String token, long terminId, String napomena) {
        return postJson("/api/rezervacije", token,
                Map.of("terminId", terminId, "napomena", napomena, "oib", "48291736504"));
    }

    private long slobodanTermin(String token) {
        for (JsonNode termin : get("/api/termini", token).json()) {
            if (termin.get("id").asLong() != IZUZETI_TERMIN && termin.get("slobodno").asInt() > 0) {
                return termin.get("id").asLong();
            }
        }
        return noviTermin(5);
    }
    private long noviTermin(int kapacitet) {
        String lijecnik = prijava("dr.horvat", "Ovlasteni12345!");
        String xml = "<termini><termin><lijecnikId>1</lijecnikId><pocetak>2027-02-01T08:00</pocetak>"
                + "<kapacitet>" + kapacitet + "</kapacitet><napomena>Dodatni termin</napomena></termin></termini>";
        Odgovor odgovor = posalji("POST", "/api/termini/uvoz", lijecnik, "application/xml", xml);
        assertThat(odgovor.status()).isEqualTo(200);
        return odgovor.json().get("termini").get(0).get("id").asLong();
    }
}