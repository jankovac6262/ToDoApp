package org.example.example.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

// DTO pre ciastocny update ulohy (PATCH).
// Na rozdiel od TaskRequestDto su tu polia nullable a bez povinnej validacie:
// null = "toto pole neposielam, nemen ho", hodnota = "toto pole zmen".
// Preto je "completed" typu Boolean (wrapper), nie boolean (primitiv) -
// primitiv by sa vzdy default-ol na false a nedalo by sa rozlisit "neposlane" od "false".
public record TaskPatchDto(
        String title,
        Boolean completed,
        // @Future -> aj cez PATCH sa da nastavit iba termin v buducnosti.
        // Na null sa @Future nevztahuje (null = "pole neposielam"), takze PATCH
        // meniaci iba "completed" prejde bez problemu.
        @Future(message = "Termin musi byt v buducnosti")
        LocalDateTime dueAt,

        // Integer (wrapper): null = "prioritu neposielam, nemen ju".
        // @Min/@Max sa na null nevztahuju, rovnako ako @Future vyssie.
        @Min(value = 0, message = "Priorita musi byt od 0 do 5")
        @Max(value = 5, message = "Priorita musi byt od 0 do 5")
        Integer priority
) {
}
