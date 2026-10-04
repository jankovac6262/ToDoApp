package org.example.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Hlavna trieda aplikacie - vstupny bod (entry point) celej Spring Boot appky.
// @SpringBootApplication = skratka pre 3 anotacie naraz:
//   - @Configuration      -> trieda moze definovat Spring bean-y
//   - @EnableAutoConfiguration -> Spring automaticky nakonfiguruje appku podla dependencies v pom.xml
//   - @ComponentScan      -> Spring prehlada balik org.example.example a podbaliky
//                            a najde vsetky @Component/@Service/@RestController/@Repository triedy
@SpringBootApplication
public class ExampleApplication {

    // Java main metoda - spusti sa ako prva pri starte appky
    public static void main(String[] args) {
        // Nastartuje cely Spring kontext (vytvori bean-y, embedded web server na porte 8080, atd.)
        SpringApplication.run(ExampleApplication.class, args);
    }

}
