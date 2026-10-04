package org.example.example.dto;

import jakarta.validation.constraints.Future;

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
        LocalDateTime dueAt
) {
}
