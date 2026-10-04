package org.example.example.exception;

// Vlastna (custom) unchecked vynimka - dedi z RuntimeException, takze ju nie je
// potrebne explicitne deklarovat cez "throws" ani obalovat try/catch.
// Pouziva sa v TaskService, ked sa hlada uloha podla id a v DB neexistuje.
// Zachytava ju GlobalExceptionHandler a premeni na HTTP 404 odpoved.
public class TaskNotFoundException extends RuntimeException {

    // Konstruktor rovno vytvori citatelnu chybovu spravu s konkretnym id
    public TaskNotFoundException(Long id) {
        super("Task with id " + id + " not found");
    }
}
