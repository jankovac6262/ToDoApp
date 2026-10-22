package org.example.example.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;



// DTO pre vstupne data pri vytvoreni/aktualizacii ulohy (POST/PUT).
// Java record = strucny nemenny (immutable) datovy nosic - automaticky generuje
// konstruktor, gettery (title(), completed()), equals/hashCode/toString.
// Validacne anotacie sa vyhodnotia, ked je DTO oznacene @Valid v kontroleri.
public record TaskRequestDto(

        @NotBlank(message = "Title nesmie byt prazdny")
        String title,




        // Boolean (wrapper), nie boolean: Jackson 3 (Spring Boot 4) odmietne chybajuce pole
        // pri primitive ("Cannot map null into type boolean") a klient by dostal necitatelne 400.
        // Chybajuce "completed" povazujeme za false (pozri compact konstruktor nizsie).
        Boolean completed,

        @Future(message = "Termin musi byt v buducnosti")
        LocalDateTime dueAt,

        // Priorita 0 (ziadna) az 5 (najvyssia). Integer (wrapper) z rovnakeho dovodu ako completed:
        // chybajuce pole povazujeme za 0 (pozri compact konstruktor nizsie).
        @Min(value = 0, message = "Priorita musi byt od 0 do 5")
        @Max(value = 5, message = "Priorita musi byt od 0 do 5")
        Integer priority
) {
    // Compact konstruktor recordu - spusti sa pri kazdom vytvoreni instancie,
    // parametre mozno pred priradenim do poli upravit
    public TaskRequestDto {
        if (completed == null) {
            completed = false;
        }
        if (priority == null) {
            priority = 0;
        }
    }
}
