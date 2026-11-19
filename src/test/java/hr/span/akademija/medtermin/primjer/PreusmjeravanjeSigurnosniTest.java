/*A01:2025 – Broken Access Control
ako imamo stranicu http://localhost:8080/prijava?returnUrl=https%3A%2F%2Fhttps://example.com
i ulogiramo se on ce nas automatski prebaciti na https://example.com, ali ne pazimo na URL moguce je da nam
ukrade podatke*/
package hr.span.akademija.medtermin.primjer;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PreusmjeravanjeSigurnosniTest extends ApiTestBaza {

    @Test
    void vanjskaAdresaPreusmjeravaNaPocetnu() {
        provjeriPreusmjeravanje("https://example.com", "/");
    }

    @Test
    void adresaSDvijeKoseCrtePreusmjeravaNaPocetnu() {
        provjeriPreusmjeravanje("//example.com", "/");
    }

    @Test
    void lokalnaPutanjaOstajeDopustena() {
        provjeriPreusmjeravanje("/moji", "/moji");
    }

    private void provjeriPreusmjeravanje(
            String returnUrl, String ocekivanoOdrediste) {

        // Vlastiti korisnik: test ne ovisi o promjenama Anine lozinke.
        String lozinka = "Test12345!";
        String username = noviKorisnik("redirect", lozinka);

        Odgovor odgovor = postForma("/prijava", Map.of(
                "username", username,
                "password", lozinka,
                "returnUrl", returnUrl
        ));

        assertThat(odgovor.status()).isEqualTo(302);
        assertThat(odgovor.zaglavlje("Set-Cookie"))
                .startsWith("SESSION=");
        assertThat(odgovor.zaglavlje("Location"))
                .isEqualTo(ocekivanoOdrediste);
    }
}