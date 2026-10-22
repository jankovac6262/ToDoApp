package org.example.example.repository;

import org.example.example.entity.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// PERSISTENCNY TEST (slice test) = nastartuje IBA JPA cast: entity, repository a databazu.
// Ziadne controllery ani service. Testuje sa to, co repository skutocne robi s DB:
// ci funguje mapovanie entity na tabulku a ci nas vlastny JPQL dotaz vracia spravne poradie.
// Kazdy test bezi v transakcii, ktora sa po skonceni vrati spat (rollback),
// takze testy sa navzajom neovplyvnuju.
// Predvolene @DataJpaTest nahradi datasource z application.properties vlastnou embedded DB
// (tu H2, pretoze je na classpathe).
@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    private Task task(String title, LocalDateTime dueAt) {
        Task task = new Task(title);
        task.setDueAt(dueAt);
        return task;
    }

    @Test
    void saveAndFindById_persistsAllFieldsAndGeneratesId() {
        LocalDateTime due = LocalDateTime.of(2099, 1, 1, 10, 0);
        Task task = task("Uloha", due);
        task.setCompleted(true);
        task.setPriority(3);

        Task saved = taskRepository.save(task);

        // id vygenerovala DB (GenerationType.IDENTITY), my sme ho nenastavovali
        assertThat(saved.getId()).isNotNull();
        Task loaded = taskRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getTitle()).isEqualTo("Uloha");
        assertThat(loaded.isCompleted()).isTrue();
        assertThat(loaded.getDueAt()).isEqualTo(due);
        assertThat(loaded.getPriority()).isEqualTo(3);
    }

    @Test
    void save_withNullTitle_violatesNotNullConstraint() {
        // @Column(nullable = false) na title. saveAndFlush() posle INSERT do DB hned,
        // inak by sa chyba prejavila az pri commite (ktory sa v teste nikdy nestane - rollback)
        assertThatThrownBy(() -> taskRepository.saveAndFlush(new Task(null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAllOrderedByDueAt_ordersByDeadlineAscending() {
        taskRepository.save(task("neskor", LocalDateTime.of(2099, 6, 1, 10, 0)));
        taskRepository.save(task("skor", LocalDateTime.of(2099, 1, 1, 10, 0)));
        taskRepository.save(task("stred", LocalDateTime.of(2099, 3, 1, 10, 0)));

        List<Task> result = taskRepository.findAllOrderedByDueAt();

        assertThat(result).extracting(Task::getTitle).containsExactly("skor", "stred", "neskor");
    }

    @Test
    void findAllOrderedByDueAt_putsTasksWithoutDeadlineLast() {
        // Zamerne ulozime ulohy bez terminu ako PRVE, aby test nezavisel od poradia vkladania.
        // Bez "NULLS LAST" v dotaze by ich H2 radila na zaciatok.
        taskRepository.save(task("bez terminu 1", null));
        taskRepository.save(task("s terminom", LocalDateTime.of(2099, 1, 1, 10, 0)));
        taskRepository.save(task("bez terminu 2", null));

        List<Task> result = taskRepository.findAllOrderedByDueAt();

        assertThat(result).extracting(Task::getTitle)
                .containsExactly("s terminom", "bez terminu 1", "bez terminu 2");
    }

    @Test
    void findAllOrderedByDueAt_usesIdAsTiebreakerForSameDeadline() {
        LocalDateTime same = LocalDateTime.of(2099, 1, 1, 10, 0);
        Task first = taskRepository.save(task("prva", same));
        Task second = taskRepository.save(task("druha", same));
        Task third = taskRepository.save(task("tretia", same));
        Task fourth = taskRepository.save(task("stvrta", same));

        List<Task> result = taskRepository.findAllOrderedByDueAt();

        // Pri rovnakom termine rozhoduje id (vzostupne) -> poradie je stabilne
        assertThat(result).extracting(Task::getId)
                .containsExactly(first.getId(), second.getId(), third.getId(),fourth.getId());
    }

    @Test
    void findAllOrderedByDueAt_emptyTable_returnsEmptyList() {
        assertThat(taskRepository.findAllOrderedByDueAt()).isEmpty();
    }
}
