package hr.span.akademija.medtermin.primjer;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AuthControllerTest extends ApiTestBaza {

    @Test
    @DisplayName("Predvidljiv token ne smije omogućiti reset lozinke")
    void predvidljivTokenNeSmijePromijenitiLozinku() throws Exception {
        String username = noviKorisnik("reset", "PocetnaLozinka123!");
        LocalDate datumPrije = LocalDate.now();

        Odgovor zahtjev = postJson(
                "/api/auth/reset-zahtjev",
                null,
                Map.of("username", username)
        );
        assertThat(zahtjev.status()).isEqualTo(202);

        // Obuhvaća i slučaj da zahtjev prijeđe preko ponoći.
        LocalDate datumPoslije = LocalDate.now();

        for (LocalDate datum = datumPrije;
             !datum.isAfter(datumPoslije);
             datum = datum.plusDays(1)) {

            String predvidljiviToken = HexFormat.of().formatHex(
                    MessageDigest.getInstance("MD5").digest(
                            (username + datum)
                                    .getBytes(StandardCharsets.UTF_8)
                    )
            );

            Odgovor reset = postJson(
                    "/api/auth/reset",
                    null,
                    Map.of(
                            "token", predvidljiviToken,
                            "novaLozinka", "PromijenjenaLozinka123!"
                    )
            );

            assertThat(reset.status())
                    .as("Token izračunat iz korisničkog imena i datuma mora biti odbijen")
                    .isEqualTo(400);
        }

        // Neuspjeli pokušaj reseta ne smije promijeniti lozinku.
        assertThat(prijava(username, "PocetnaLozinka123!")).isNotBlank();
    }
}