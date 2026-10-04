package org.example.example.service;

import org.example.example.dto.TaskPatchDto;
import org.example.example.entity.Task;
import org.example.example.exception.TaskNotFoundException;
import org.example.example.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;


// Service = vrstva biznis logiky, sedi medzi kontrolerom a repository.
// Kontroler sa nema priamo pytat databazy - vsetka logika (napr. co robit,
// ked entita neexistuje) patri sem.
// @Service -> oznacuje triedu ako Spring bean tejto vrstvy (specializacia @Component),
// vdaka comu ju Spring vie automaticky vytvorit a injektnut inde (napr. do TaskController).
@Service
public class TaskService {

    private final TaskRepository taskRepository;

    // Constructor injection - Spring automaticky doda instanciu TaskRepository
    // (anotacia @Autowired tu nie je potrebna, lebo trieda ma iba jeden konstruktor)
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // Vrati vsetky ulohy z DB zoradene podla terminu (najblizsi termin hore,
    // ulohy bez terminu uplne dole) - pozri JPQL dotaz v TaskRepository.
    // Radenie patri do backendu, nie do frontendu: kazdy klient tak dostane
    // data uz v spravnom poradi a nemusi si ho riesit sam.
    public List<Task> getAllTasks() {
        return taskRepository.findAllOrderedByDueAt();
    }

    // Najde ulohu podla id; ak neexistuje, findById() vrati prazdny Optional
    // a orElseThrow vyhodi vlastnu vynimku TaskNotFoundException
    public Task getTaskById(Long id) {

        return this.taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
   }

    // Ulozi novu ulohu do DB (INSERT); save() zaroven vrati ulozenu entitu s vygenerovanym id
    public Task createTask(Task task ) {
        return  this.taskRepository.save(task);
    }

    // Najde existujucu ulohu, prepise jej udaje novymi hodnotami a znovu ulozi (UPDATE)
    public Task updateTask(Long id , Task updatedTask) {

        Task task = this.taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        task.setTitle(updatedTask.getTitle());
        task.setCompleted(updatedTask.isCompleted());
        task.setDueAt(updatedTask.getDueAt());

        return this.taskRepository.save(task);

    }

    // Ciastocne aktualizuje ulohu - meni iba polia, ktore su v patchi vyplnene (nie null).
    // Na rozdiel od updateTask() sa tu neprepisuje cely objekt naraz.
    public Task patchTask(Long id, TaskPatchDto patch) {
        Task task = this.taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));

        if (patch.title() != null) {
            if (patch.title().isBlank()) {
                throw new IllegalArgumentException("Title nesmie byt prazdny");
            }
            task.setTitle(patch.title());
        }

        if (patch.completed() != null) {
            task.setCompleted(patch.completed());
        }

        // POZOR na hranicu PATCH semantiky: null tu znamena "pole som neposlal, nemen ho",
        // takze cez PATCH sa termin uz existujucej ulohe NEDA zrusit - iba prepisat na iny.
        // Na zrusenie terminu treba pouzit PUT s dueAt = null (ten prepisuje cely objekt).
        if (patch.dueAt() != null) {
            task.setDueAt(patch.dueAt());
        }

        return this.taskRepository.save(task);
    }

    // Zmaze ulohu podla id; najprv overi existsById(), aby vedela vyhodit
    // zrozumitelnu TaskNotFoundException namiesto ticheho no-op spravania
    public void deleteTask(Long id) {
        if (!this.taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        this.taskRepository.deleteById(id);
    }

    // Zmaze vsetky ulohy naraz; deleteAll() je zdedena z JpaRepository, netreba ju definovat
    public void deleteAllTasks() {
        this.taskRepository.deleteAll();
    }

}
