package hr.span.akademija.medtermin.platforma;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
/** Centralna konfiguracija Spring Security pravila za pristup i CORS. */
public class SigurnosnaKonfiguracija {

    @Bean
    SecurityFilterChain sigurnosniFilter(HttpSecurity http, AuthFilter authFilter) throws Exception {
        return http
                // API koristi vlastite tokene i kolačić, pa ne koristi Springov CSRF token.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // Eksplicitno javne su samo stranice i endpointi potrebni za autentikaciju.
                // Sve ostale rute zatvorene su dok se ne autentificira korisnik.
                .authorizeHttpRequests(pravila -> pravila
                        .requestMatchers("/", "/prijava", "/promjena-lozinke", "/api/auth/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                // Postojeći filter provjerava SESSION/Bearer tokene prije autorizacijskih pravila.
                .addFilterBefore(authFilter, org.springframework.security.web.context.SecurityContextHolderFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties postavke) {
        CorsConfiguration konfiguracija = new CorsConfiguration();
        // Origin-i dolaze iz app.cors.allowed-origins; nema wildcarda uz vjerodajnice.
        konfiguracija.setAllowedOrigins(postavke.dopusteniOrigini());
        konfiguracija.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        konfiguracija.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        konfiguracija.setAllowCredentials(true);
        konfiguracija.setMaxAge(3600L);

        // Springov CORS filter obrađuje i preflight OPTIONS zahtjeve prije autorizacije.
        UrlBasedCorsConfigurationSource izvor = new UrlBasedCorsConfigurationSource();
        izvor.registerCorsConfiguration("/**", konfiguracija);
        return izvor;
    }
}
