package org.example.example.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Entita = trieda, ktora reprezentuje riadok v databazovej tabulke (JPA/Hibernate).
// @Entity -> Hibernate bude tuto triedu sledovat a mapovat na DB tabulku
@Entity
// @Table(name = "tasks") -> explicitne urcuje nazov tabulky v DB (ak by chybalo, pouzil by sa nazov triedy "Task")
@Table(name = "tasks")
public class Task {

    // @Id -> oznacuje primarny kluc tabulky
    @Id
    // @GeneratedValue -> hodnotu id generuje databaza sama (auto-increment), netreba ju nastavovat rucne
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column(nullable = false) -> stlpec v DB nesmie byt NULL (title je povinny udaj)
    @Column(nullable = false)
    private String title;

    // bez anotacie -> Hibernate automaticky vytvori stlpec "completed" typu boolean
    private boolean completed;


    // Termin, dokedy ma byt uloha hotova. LocalDateTime = datum + cas bez casovej zony.
    // Pole je nullable (nema @Column(nullable = false)) -> uloha nemusi mat termin vobec.
    // @Column(name = "due_at") -> explicitny nazov stlpca v DB.
    @Column(name = "due_at")
    private LocalDateTime dueAt;


    public Task() {




    }

    public Task(String title) {
        this.title = title;
        this.completed = false;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    // Gettery a settery
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }




}
