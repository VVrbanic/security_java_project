package hr.span.akademija.medtermin.lijecnici;

import hr.span.akademija.medtermin.platforma.PrijavljeniKorisnik;
import java.util.List;
import hr.span.akademija.medtermin.platforma.ApiException;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lijecnici")
public class LijecnikController {

    public record Lijecnik(long id, String ime, String prezime, String specijalizacija, String ordinacija) {
    }

    private static final RowMapper<Lijecnik> MAPPER = (rs, i) -> new Lijecnik(rs.getLong("id"),
            rs.getString("ime"), rs.getString("prezime"), rs.getString("specijalizacija"), rs.getString("ordinacija"));

    private final JdbcTemplate jdbc;

    public LijecnikController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Popis liječnika, npr. {@code GET /api/lijecnici?sort=prezime&smjer=asc}. */
    @GetMapping
    public List<Lijecnik> popis(@RequestParam(defaultValue = "prezime") String sort,
                                @RequestParam(defaultValue = "asc") String smjer,
                                PrijavljeniKorisnik korisnik) {
        String smjerNormaliziran = smjer.toLowerCase(Locale.ROOT);
        if (!smjerNormaliziran.equals("asc") && !smjerNormaliziran.equals("desc")) {
            throw ApiException.neispravno("Neispravan parametar sortiranja.");
        }
        boolean uzlazno = smjerNormaliziran.equals("asc");
        return switch (sort) {
            case "ime" -> uzlazno
                    ? jdbc.query("SELECT id, ime, prezime, specijalizacija, ordinacija FROM lijecnici ORDER BY ime ASC, id", MAPPER)
                    : jdbc.query("SELECT id, ime, prezime, specijalizacija, ordinacija FROM lijecnici ORDER BY ime DESC, id", MAPPER);
            case "prezime" -> uzlazno
                    ? jdbc.query("SELECT id, ime, prezime, specijalizacija, ordinacija FROM lijecnici ORDER BY prezime ASC, id", MAPPER)
                    : jdbc.query("SELECT id, ime, prezime, specijalizacija, ordinacija FROM lijecnici ORDER BY prezime DESC, id", MAPPER);
            case "specijalizacija" -> uzlazno
                    ? jdbc.query("SELECT id, ime, prezime, specijalizacija, ordinacija FROM lijecnici ORDER BY specijalizacija ASC, id", MAPPER)
                    : jdbc.query("SELECT id, ime, prezime, specijalizacija, ordinacija FROM lijecnici ORDER BY specijalizacija DESC, id", MAPPER);
            default -> throw ApiException.neispravno("Neispravan parametar sortiranja.");
        };
    }
}
