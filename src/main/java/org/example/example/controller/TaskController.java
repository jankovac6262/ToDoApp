package org.example.example.controller;

import jakarta.validation.Valid;
import org.example.example.dto.TaskPatchDto;
import org.example.example.dto.TaskRequestDto;
import org.example.example.dto.TaskResponseDto;
import org.example.example.entity.Task;
import org.example.example.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controller = vstupna vrstva REST API - prijima HTTP requesty a vracia HTTP odpovede.
// Neobsahuje biznis logiku, iba deleguje na TaskService.
// @RestController -> @Controller + @ResponseBody, vysledok kazdej metody sa
// automaticky serializuje do JSON (Jackson) namiesto hladania HTML view
@RestController
// @RequestMapping -> spolocny prefix URL pre vsetky endpointy nizsie (napr. /{id} = /api/tasks/{id})
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    // Constructor injection - Spring automaticky doda instanciu TaskService
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // GET /api/tasks -> vrati zoznam vsetkych uloh ako DTO, status 200 OK (default)
    @GetMapping
    public List<TaskResponseDto> getAllTasks() {
        return taskService.getAllTasks().stream()
                .map(this ::toResponseDto)
                .toList();
    }

    // GET /api/tasks/{id} -> vrati jednu ulohu
    // @PathVariable vytiahne hodnotu {id} z URL a namapuje ju na parameter id
    @GetMapping("/{id}")
    public TaskResponseDto getTaskById(@PathVariable Long id) {
        return toResponseDto(taskService.getTaskById(id));
    }

    // POST /api/tasks -> vytvori novu ulohu
    // @RequestBody deserializuje JSON telo requestu do TaskRequestDto
    // @Valid spusti validaciu podla anotacii v TaskRequestDto (napr. @NotBlank na title)
    // - ak validacia zlyha, Spring vyhodi MethodArgumentNotValidException este pred
    //   telom metody a tu handluje GlobalExceptionHandler (vrati 400 Bad Request)
    @PostMapping
    // @ResponseStatus prepisuje default 200 OK na 201 Created (REST konvencia pri vytvarani zdroja)
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponseDto createTask(@Valid @RequestBody TaskRequestDto request) {
        Task task = new Task(request.title());
        task.setCompleted(request.completed());
        task.setDueAt(request.dueAt());
        return toResponseDto(taskService.createTask(task));
    }

    // PUT /api/tasks/{id} -> aktualizuje existujucu ulohu (id z URL + nove data z tela)
    @PutMapping("/{id}")
    public TaskResponseDto updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequestDto request) {
        Task updatedData = new Task(request.title());
        updatedData.setCompleted(request.completed());
        updatedData.setDueAt(request.dueAt());
        return toResponseDto(taskService.updateTask(id, updatedData));
    }

    // PATCH /api/tasks/{id} -> ciastocna aktualizacia (napr. iba prepnutie "completed"
    // bez potreby posielat aj title, na rozdiel od PUT vyssie)
    @PatchMapping("/{id}")
    public TaskResponseDto patchTask(@PathVariable Long id, @Valid @RequestBody TaskPatchDto request) {
        return toResponseDto(taskService.patchTask(id, request));
    }  

    // DELETE /api/tasks/{id} -> zmaze ulohu
    // ResponseEntity<Void> umoznuje explicitne vratit status bez tela odpovede
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        // 204 No Content - operacia uspesna, ale niet co vratit
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/tasks (bez {id}) -> zmaze VSETKY ulohy naraz
    // Musi mat inu cestu ako metoda vyssie, inak by Spring nevedel rozlisit,
    // ktory endpoint ma volat (kolizia mapovania)
    @DeleteMapping
    public ResponseEntity<Void> deleteAllTasks() {
        taskService.deleteAllTasks();
        return ResponseEntity.noContent().build();
    }

    // Pomocna mapovacia metoda: entita Task (interny tvar dat) -> TaskResponseDto (verejny tvar dat pre API)
    private TaskResponseDto toResponseDto(Task task) {
        return new TaskResponseDto(task.getId(), task.getTitle(), task.isCompleted(),task.getDueAt());
    }
}
