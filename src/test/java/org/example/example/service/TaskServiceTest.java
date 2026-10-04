package org.example.example.service;

import org.example.example.dto.TaskPatchDto;
import org.example.example.entity.Task;
import org.example.example.exception.TaskNotFoundException;
import org.example.example.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Locale.Category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// UNIT TEST = testuje jednu triedu izolovane, bez Springu a bez databazy.
// Vsetky zavislosti (TaskRepository) nahradime mockom - falosnym objektom, ktoremu
// v teste povieme, co ma vratit, a potom si mozeme overit, ako ho service volala.
// Vdaka tomu bezi test za milisekundy a zlyha len vtedy, ked je chyba v TaskService.
// @ExtendWith(MockitoExtension.class) -> JUnit si od Mockita vypyta @Mock / @InjectMocks
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    // @Mock -> falosny repository, nikdy nesiaha na DB
    @Mock
    private TaskRepository taskRepository;

    // @InjectMocks -> vytvori skutocny TaskService a do konstruktora mu vlozi vyssi mock
    @InjectMocks
    private TaskService taskService;

    private Task testTask;

    // Pomocna metoda, aby sa v kazdom teste neopakovalo skladanie ulohy
    private Task task(Long id, String title, boolean completed, LocalDateTime dueAt) {
        Task task = new Task(title);
        task.setId(id);
        task.setCompleted(completed);
        task.setDueAt(dueAt);
        return task;
    }








    // ---------- getAllTasks ----------

    @Test
    void getAllTasks_returnsTasksInOrderGivenByRepository() {
        List<Task> ordered = List.of(task(1L, "A", false, null), task(2L, "B", false, null));
        when(taskRepository.findAllOrderedByDueAt()).thenReturn(ordered);

        List<Task> result = taskService.getAllTasks();

        // Radenie robi repository (JPQL dotaz) - service ho nema menit ani prerabat
        assertThat(result).containsExactlyElementsOf(ordered);
    }

    // ---------- getTaskById ----------

    @Test
    void getTaskById_existingId_returnsTask() {
        Task existing = task(1L, "Kupit mlieko", false, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThat(taskService.getTaskById(1L)).isSameAs(existing);
    }

    @Test
    void getTaskById_missingId_throwsTaskNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task with id 99 not found");
    }

    // ---------- createTask ----------

    @Test
    void createTask_savesTaskAndReturnsSavedEntity() {
        Task input = task(null, "Nova uloha", false, null);
        Task saved = task(5L, "Nova uloha", false, null);
        when(taskRepository.save(input)).thenReturn(saved);

        Task result = taskService.createTask(input);

        // Vraciame to, co vratil save() (obsahuje vygenerovane id), nie povodny objekt
        assertThat(result).isSameAs(saved);
        assertThat(result.getId()).isEqualTo(5L);
    }

    // ---------- updateTask (PUT) ----------

    @Test
    void updateTask_overwritesAllFields() {
        LocalDateTime newDue = LocalDateTime.of(2099, 1, 1, 10, 0);
        Task existing = task(1L, "Stary nazov", false, LocalDateTime.of(2098, 1, 1, 10, 0));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.updateTask(1L, task(null, "Novy nazov", true, newDue));

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Novy nazov");
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getDueAt()).isEqualTo(newDue);
    }

    @Test
    void updateTask_withNullDueAt_clearsDeadline() {
        // PUT prepisuje cely objekt, takze dueAt = null je jediny sposob, ako termin zrusit
        Task existing = task(1L, "Uloha", false, LocalDateTime.of(2099, 1, 1, 10, 0));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.updateTask(1L, task(null, "Uloha", false, null));

        assertThat(result.getDueAt()).isNull();
    }

    @Test
    void updateTask_missingId_throwsAndDoesNotSave() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(99L, task(null, "X", false, null)))
                .isInstanceOf(TaskNotFoundException.class);

        // verify(..., never()) -> overime, ze sa metoda vobec nezavolala
        verify(taskRepository, never()).save(any());
    }

    // ---------- patchTask (PATCH) ----------

    @Test
    void patchTask_onlyCompleted_leavesOtherFieldsUntouched() {
        LocalDateTime due = LocalDateTime.of(2099, 1, 1, 10, 0);
        Task existing = task(1L, "Uloha", false, due);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.patchTask(1L, new TaskPatchDto(null, true, null));

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getTitle()).isEqualTo("Uloha");
        assertThat(result.getDueAt()).isEqualTo(due);
    }

    @Test
    void patchTask_onlyTitle_leavesOtherFieldsUntouched() {
        Task existing = task(1L, "Stary", true, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.patchTask(1L, new TaskPatchDto("Novy", null, null));

        assertThat(result.getTitle()).isEqualTo("Novy");
        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    void patchTask_withDueAt_replacesDeadline() {
        LocalDateTime newDue = LocalDateTime.of(2099, 6, 1, 8, 0);
        Task existing = task(1L, "Uloha", false, LocalDateTime.of(2099, 1, 1, 10, 0));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.patchTask(1L, new TaskPatchDto(null, null, newDue));

        assertThat(result.getDueAt()).isEqualTo(newDue);
    }

    @Test
    void patchTask_nullDueAt_cannotClearExistingDeadline() {
        // Tento test DOKUMENTUJE zname obmedzenie z komentara v TaskService:
        // v PATCH znamena null "pole neposielam", takze termin sa cez PATCH zrusit neda.
        // Ak niekedy zmenis navrh (napr. JsonNullable), tento test sa vedome upravi.
        LocalDateTime due = LocalDateTime.of(2099, 1, 1, 10, 0);
        Task existing = task(1L, "Uloha", false, due);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.patchTask(1L, new TaskPatchDto(null, null, null));

        assertThat(result.getDueAt()).isEqualTo(due);
    }

    @Test
    void patchTask_blankTitle_throwsIllegalArgumentAndDoesNotSave() {
        Task existing = task(1L, "Uloha", false, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> taskService.patchTask(1L, new TaskPatchDto("   ", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Title nesmie byt prazdny");

        verify(taskRepository, never()).save(any());
        // Nepodarena zmena nesmie pokazit ani entitu v pamati
        assertThat(existing.getTitle()).isEqualTo("Uloha");
    }

    @Test
    void patchTask_missingId_throwsTaskNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.patchTask(99L, new TaskPatchDto(null, true, null)))
                .isInstanceOf(TaskNotFoundException.class);
    }

    // ---------- deleteTask ----------

    @Test
    void deleteTask_existingId_deletesById() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.deleteTask(1L);

        verify(taskRepository).deleteById(1L);
    }

    @Test
    void deleteTask_missingId_throwsAndDeletesNothing() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).deleteById(any());
    }

    // ---------- deleteAllTasks ----------

    @Test
    void deleteAllTasks_delegatesToRepository() {
        taskService.deleteAllTasks();

        verify(taskRepository).deleteAll();
    }
}
