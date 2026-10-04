package org.example.example.repository;

import org.example.example.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// Repository = vrstva zodpovedna za komunikaciu s databazou.
// Ziadna vlastna implementacia netreba - Spring Data JPA automaticky vygeneruje
// implementaciu tohto interface za behu (findAll, findById, save, deleteById, existsById...).
// <Task, Long> = typ entity, ktoru repository spravuje, a typ jej primarneho kluca (id).
// Ziadna anotacia (@Repository) tu nie je potrebna, lebo @SpringBootApplication
// cez @ComponentScan + JpaRepository rozhranie ju Spring rozpozna a vytvori bean automaticky.
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Vlastny dotaz v JPQL (pyta sa na ENTITU Task a jej pole dueAt, nie na tabulku/stlpec).
    // Preco nestaci odvodeny nazov findAllByOrderByDueAtAsc()? Ten by sa prelozil na
    // "ORDER BY due_at ASC" a vacsina databaz (aj H2) radi NULL hodnoty ako prve -
    // ulohy BEZ terminu by skoncili na vrchu zoznamu. "NULLS LAST" ich posunie na koniec.
    // ", t.id ASC" je tiebreaker: ulohy s rovnakym (alebo ziadnym) terminom maju
    // tak vzdy stabilne poradie a zoznam po refreshi nepreskakuje.
    @Query("SELECT t FROM Task t ORDER BY t.dueAt ASC NULLS LAST, t.id ASC")
    List<Task> findAllOrderedByDueAt();
}
