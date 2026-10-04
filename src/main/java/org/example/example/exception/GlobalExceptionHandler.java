package org.example.example.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// Globalny handler vynimiek pre cele REST API.
// @RestControllerAdvice -> Spring ju automaticky napoji na VSETKY @RestController
// triedy v projekte, netreba try/catch v kazdom kontroleri zvlast.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @ExceptionHandler urcuje, ktory typ vynimky tato metoda zachyti.
    // Ak niekde (napr. v TaskService, volanej z TaskController) vyleti
    // TaskNotFoundException, Spring ju presmeruje sem namiesto default 500 chyby.
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTaskNotFound(TaskNotFoundException ex) {
        // Rucne poskladane telo JSON odpovede s casom, statusom, nazvom chyby a spravou
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.NOT_FOUND.value(),
                "error", HttpStatus.NOT_FOUND.getReasonPhrase(),
                "message", ex.getMessage()
        );
        // Klientovi sa vrati HTTP 404 Not Found s vyssie zostavenym JSON telom
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // Zachyti IllegalArgumentException vyhodenu rucne v service vrstve
    // (napr. TaskService.patchTask, ked niekto poslal prazdny title).
    // Na rozdiel od MethodArgumentNotValidException sem chyba nepride z @Valid,
    // ale z vlastnej kontroly v biznis logike - preto ju treba zachytit samostatne.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "message", ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Zachyti zlyhanie validacie @Valid na @RequestBody DTO (napr. prazdny title).
    // Spring vyhodi MethodArgumentNotValidException este pred tym, ako sa vykona telo
    // metody v kontroleri - takze do TaskController/TaskService sa nevalidne data vobec nedostanu.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        // BindingResult obsahuje zoznam vsetkych poli, ktore zlyhali na validacii,
        // spolu so spravou z anotacie (napr. message = "Title nesmie byt prazdny")
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "message", "Validacia vstupnych dat zlyhala",
                "fields", fieldErrors
        );
        // Klientovi sa vrati HTTP 400 Bad Request s prehladom, ktore polia su nevalidne
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
