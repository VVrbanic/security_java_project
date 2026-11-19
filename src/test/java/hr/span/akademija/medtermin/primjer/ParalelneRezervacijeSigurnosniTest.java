package hr.span.akademija.medtermin.primjer;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@Tag("sigurnosno")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:paralelne-rezervacije-test;DB_CLOSE_DELAY=-1")
class ParalelneRezervacijeSigurnosniTest extends ApiTestBaza {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("Dva paralelna zahtjeva ne mogu rezervirati isto posljednje mjesto")
    void samoJedanKorisnikMozeRezerviratiPosljednjeMjesto() throws Exception {
        String ana = prijava("ana", "Ana12345!");
        String marko = prijava("marko", "Marko12345!");
        String lijecnik = prijava("dr.horvat", "Ovlasteni12345!");

        ExecutorService izvrsitelj = Executors.newFixedThreadPool(2);
        try {
            // Vise pokusaja smanjuje ovisnost o rasporedu dretvi; svaki koristi nov termin.
            for (int pokusaj = 1; pokusaj <= 5; pokusaj++) {
                long terminId = noviTermin(lijecnik);
                CountDownLatch spremni = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);

                Future<Odgovor> prvi = izvrsitelj.submit(
                        () -> rezervirajNakonSignala(ana, terminId, spremni, start));
                Future<Odgovor> drugi = izvrsitelj.submit(
                        () -> rezervirajNakonSignala(marko, terminId, spremni, start));
                assertThat(spremni.await(5, TimeUnit.SECONDS)).as("Oba zahtjeva moraju biti spremna").isTrue();
                start.countDown();

                Odgovor odgovorA = prvi.get(35, TimeUnit.SECONDS);
                Odgovor odgovorB = drugi.get(35, TimeUnit.SECONDS);
                assertThat(List.of(odgovorA.status(), odgovorB.status()))
                        .as("Pokusaj %s, termin %s: Ana=%s %s; Marko=%s %s", pokusaj, terminId,
                                odgovorA.status(), odgovorA.tijelo(), odgovorB.status(), odgovorB.tijelo())
                        .containsExactlyInAnyOrder(201, 409);

                assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM rezervacije WHERE termin_id = ?", Integer.class, terminId))
                        .as("Smije postojati samo jedna rezervacija termina %s", terminId).isEqualTo(1);
                assertThat(jdbc.queryForObject(
                        "SELECT zauzeto FROM termini WHERE id = ?", Integer.class, terminId))
                        .as("Zauzetost mora ostati 1 za kapacitet 1").isEqualTo(1);
            }
        } finally {
            izvrsitelj.shutdownNow();
            izvrsitelj.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private Odgovor rezervirajNakonSignala(String token, long terminId,
                                          CountDownLatch spremni, CountDownLatch start) throws InterruptedException {
        spremni.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("Nije primljen signal za paralelno slanje zahtjeva.");
        }
        return postJson("/api/rezervacije", token,
                Map.of("terminId", terminId, "napomena", "Test paralelne rezervacije", "oib", "48291736504"));
    }

    private long noviTermin(String token) {
        String xml = """
                <termini><termin>
                  <lijecnikId>1</lijecnikId>
                  <pocetak>2027-02-01T08:00</pocetak>
                  <kapacitet>1</kapacitet>
                  <napomena>Test utrke zahtjeva</napomena>
                </termin></termini>
                """;
        Odgovor odgovor = posalji("POST", "/api/termini/uvoz", token, "application/xml", xml);
        assertThat(odgovor.status()).as("Uvoz testnog termina: %s", odgovor.tijelo()).isEqualTo(200);
        assertThat(odgovor.json().path("uvezeno").asInt()).isEqualTo(1);
        long id = odgovor.json().path("termini").path(0).path("id").asLong();
        assertThat(id).isPositive();
        return id;
    }
}
