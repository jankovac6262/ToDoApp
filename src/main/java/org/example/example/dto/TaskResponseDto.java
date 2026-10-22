package org.example.example.dto;

import java.time.LocalDateTime;

// DTO pre vystupne data - presne to, co appka posiela klientovi ako JSON odpoved.
// Oddelene od entity Task, aby zmeny v DB tabulke (napr. pridanie interneho pola)
// automaticky neunikli do API odpovede.
public record TaskResponseDto(
        Long id,
        String title,
        boolean completed,
        LocalDateTime dueAt,
        int priority
) {
}
