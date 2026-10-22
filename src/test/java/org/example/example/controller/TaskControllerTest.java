package org.example.example.controller;

import org.example.example.dto.TaskPatchDto;
import org.example.example.entity.Task;
import org.example.example.exception.TaskNotFoundException;
import org.example.example.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// WEB-VRSTVOVY TEST (slice test) = nastartuje IBA webovu cast Springu: kontrolery,
// JSON (de)serializaciu, validaciu (@Valid) a @RestControllerAdvice.
// Service a repository sa vobec nevytvaraju - TaskService nahradime mockom.
// Testujeme teda to, co je zodpovednostou controllera: spravne URL, HTTP metody,
// status kody, tvar JSON-u a to, ze chyby sa premenia na 400/404.
// Je rychlejsi ako @SpringBootTest, lebo nenacitava celu aplikaciu ani DB.
@WebMvcTest(TaskController.class)
class TaskControllerTest {

    // MockMvc = simuluje HTTP requesty bez skutocneho servera (bez otvoreneho portu)
    @Autowired
    private MockMvc mockMvc;

    // @MockitoBean = do Spring kontextu vlozi mock namiesto skutocneho TaskService
    // (v Spring Boot 3.4- sa to volalo @MockBean)
    @MockitoBean
    private TaskService taskService;

    private Task task(Long id, String title, boolean completed, LocalDateTime dueAt) {
        Task task = new Task(title);
        task.setId(id);
        task.setCompleted(completed);
        task.setDueAt(dueAt);
        return task;
    }

    // ---------- GET ----------

    @Test
    void getAllTasks_returns200AndJsonArray() throws Exception {
        when(taskService.getAllTasks()).thenReturn(List.of(
                task(1L, "Prva", false, null),
                task(2L, "Druha", true, LocalDateTime.of(2099, 1, 1, 10, 0))));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Prva"))
                .andExpect(jsonPath("$[0].completed").value(false))
                .andExpect(jsonPath("$[0].dueAt").isEmpty())
                .andExpect(jsonPath("$[1].completed").value(true))
                .andExpect(jsonPath("$[1].dueAt").value("2099-01-01T10:00:00"));
    }

    @Test
    void getTaskById_existing_returns200() throws Exception {
        when(taskService.getTaskById(1L)).thenReturn(task(1L, "Prva", false, null));

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Prva"));
    }

    @Test
    void getTaskById_missing_returns404WithErrorBody() throws Exception {
        when(taskService.getTaskById(99L)).thenThrow(new TaskNotFoundException(99L));

        // Overujeme, ze GlobalExceptionHandler premenil vynimku na 404 a JSON s pozadovanou strukturou
        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Task with id 99 not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ---------- POST ----------

    @Test
    void createTask_validBody_returns201AndCreatedTask() throws Exception {
        // Mock vrati to, co dostal, len s dogenerovanym id (simuluje INSERT)
        when(taskService.createTask(any(Task.class))).thenAnswer(inv -> { //  inv.getArgument(0) je prvý argument, s ktorým sa metóda zavolala

            Task t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Nova uloha", "completed": false, "dueAt": "2099-01-01T10:00:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Nova uloha"))
                .andExpect(jsonPath("$.dueAt").value("2099-01-01T10:00:00"));
    }

    @Test
    void createTask_withoutOptionalFields_usesDefaults() throws Exception {
        when(taskService.createTask(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        // completed a dueAt v JSON-e chybaju -> completed = false (default primitivu), dueAt = null
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Len nazov\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.dueAt").isEmpty())
                // chybajuca priorita -> 0 (compact konstruktor v TaskRequestDto)
                .andExpect(jsonPath("$.priority").value(0));
    }

    @Test
    void createTask_withPriority_returnsItInResponse() throws Exception {
        when(taskService.createTask(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        // Overuje celu cestu pola: JSON -> TaskRequestDto -> Task -> TaskResponseDto -> JSON
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Dolezita\", \"priority\": 4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value(4));
    }

    @Test
    void createTask_priorityOutOfRange_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Uloha\", \"priority\": 6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.priority").value("Priorita musi byt od 0 do 5"));

        verifyNoInteractions(taskService);
    }

    @Test
    void createTask_blankTitle_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validacia vstupnych dat zlyhala"))
                .andExpect(jsonPath("$.fields.title").value("Title nesmie byt prazdny"));

        // Nevalidne data sa nesmu dostat ani do service
        verifyNoInteractions(taskService);
    }

    @Test
    void createTask_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists());
    }

    @Test
    void createTask_dueAtInPast_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Uloha\", \"dueAt\": \"2000-01-01T10:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.dueAt").value("Termin musi byt v buducnosti"));

        verifyNoInteractions(taskService);
    }

    @Test
    void createTask_malformedJson_returns400() throws Exception {
        // Toto nezachytava nas handler, ale default Spring - test len hlida, ze klient dostane 400, nie 500
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ toto nie je json"))
                .andExpect(status().isBadRequest());
    }

    // ---------- PUT ----------

    @Test
    void updateTask_validBody_returns200() throws Exception {
        when(taskService.updateTask(eq(1L), any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(1);
            t.setId(1L);
            return t;
        });

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Upravena\", \"completed\": true, \"priority\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Upravena"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.priority").value(3));
    }

    @Test
    void updateTask_missing_returns404() throws Exception {
        when(taskService.updateTask(eq(99L), any(Task.class))).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(put("/api/tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Uloha\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTask_blankTitle_returns400() throws Exception {
        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists());
    }

    // ---------- PATCH ----------

    @Test
    void patchTask_partialBody_returns200() throws Exception {
        when(taskService.patchTask(eq(1L), any(TaskPatchDto.class)))
                .thenReturn(task(1L, "Uloha", true, null));

        mockMvc.perform(patch("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        // Overime aj, co presne controller poslal do service: len completed, ostatne null
        verify(taskService).patchTask(1L, new TaskPatchDto(null, true, null,null));
    }

    @Test
    void patchTask_blankTitleRejectedByService_returns400() throws Exception {
        // Prazdny title pri PATCH nekontroluje @Valid ale service (IllegalArgumentException),
        // preto tu simulujeme jej vynimku a overujeme mapovanie na 400
        when(taskService.patchTask(eq(1L), any(TaskPatchDto.class)))
                .thenThrow(new IllegalArgumentException("Title nesmie byt prazdny"));

        mockMvc.perform(patch("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title nesmie byt prazdny"));
    }

    @Test
    void patchTask_dueAtInPast_returns400() throws Exception {
        mockMvc.perform(patch("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dueAt\": \"2000-01-01T10:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.dueAt").exists());

        verify(taskService, never()).patchTask(any(), any());
    }

    @Test
    void patchTask_priorityOutOfRange_returns400() throws Exception {
        mockMvc.perform(patch("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.priority").exists());

        verify(taskService, never()).patchTask(any(), any());
    }

    @Test
    void patchTask_missing_returns404() throws Exception {
        when(taskService.patchTask(eq(99L), any(TaskPatchDto.class))).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(patch("/api/tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\": true}"))
                .andExpect(status().isNotFound());
    }

    // ---------- DELETE ----------

    @Test
    void deleteTask_existing_returns204WithEmptyBody() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(taskService).deleteTask(1L);
    }

    @Test
    void deleteTask_missing_returns404() throws Exception {
        // Pri metode vracajucej void sa vynimka nastavuje cez doThrow(...).when(...)
        doThrow(new TaskNotFoundException(99L)).when(taskService).deleteTask(99L);

        mockMvc.perform(delete("/api/tasks/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAllTasks_returns204() throws Exception {
        mockMvc.perform(delete("/api/tasks"))
                .andExpect(status().isNoContent());

        verify(taskService).deleteAllTasks();
    }
}
