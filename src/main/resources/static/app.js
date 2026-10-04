const API_URL = "/api/tasks";

const form = document.getElementById("task-form");
const titleInput = document.getElementById("task-title");
const dueInput = document.getElementById("task-due");
const list = document.getElementById("task-list");
const deleteAllBtn = document.getElementById("delete-all-btn");

// Naformátuje ISO dátum ("2026-09-20T14:30:00") na "20.9.2026 14:30"
function formatDueDate(dueAt) {
    const date = new Date(dueAt);
    return date.toLocaleDateString("sk-SK") + " " +
        date.toLocaleTimeString("sk-SK", { hour: "2-digit", minute: "2-digit" });
}

// Načíta všetky úlohy z backendu a prekreslí zoznam
async function loadTasks() {
    try {
        const response = await fetch(API_URL);
        if (!response.ok) throw new Error("Nepodarilo sa načítať úlohy");
        const tasks = await response.json();
        renderTasks(tasks);
        console.log(tasks);
    } catch (err) {
        list.innerHTML = `<li class="error">${err.message}</li>`;
    }
}

function renderTasks(tasks) {
    list.innerHTML = "";

    if (tasks.length === 0) {
        list.innerHTML = `<li class="empty">Zatiaľ žiadne úlohy</li>`;
        return;
    }

    tasks.forEach(task => {
        
            const li = document.createElement("li");
            li.className = task.completed ? "completed" : "";
    
            const checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.checked = task.completed;
            checkbox.addEventListener("change", () => toggleCompleted(task));
    
            const textWrap = document.createElement("div");
            textWrap.className = "task-text";
    
            const span = document.createElement("span");
            span.textContent = task.title;
            textWrap.appendChild(span);
    
            if (task.dueAt) {
                const due = document.createElement("span");
                due.className = "due-date";
                if (!task.completed && new Date(task.dueAt) < new Date()) {
                    due.classList.add("overdue");
                }
                due.textContent = "Termín: " + formatDueDate(task.dueAt);
                textWrap.appendChild(due);
            }
    
            const deleteBtn = document.createElement("button");
            deleteBtn.textContent = "Zmazať";
            deleteBtn.className = "btn-danger";
            deleteBtn.addEventListener("click", () => deleteTask(task.id));
    
            li.append(checkbox, textWrap, deleteBtn);
            list.appendChild(li);
        
    });
}

// Vytvorí novú úlohu (POST) a znovu načíta zoznam
// dueAt: hodnota z <input type="datetime-local"> je "YYYY-MM-DDTHH:mm",
// čo backend (LocalDateTime) vie priamo naparsovať; prázdny input -> null
async function createTask(title, dueAt) {
    await fetch(API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title, completed: false, dueAt: dueAt || null })
    });
    await loadTasks();
}

// Prepne stav "completed" - PATCH mení iba toto pole, netreba posielať aj title
async function toggleCompleted(task) {
    await fetch(`${API_URL}/${task.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ completed: !task.completed })
    });
    await loadTasks();
}

async function deleteTask(id) {
    await fetch(`${API_URL}/${id}`, { method: "DELETE" });
    await loadTasks();
}

async function deleteAllTasks() {
    await fetch(API_URL, { method: "DELETE" });
    await loadTasks();
}

form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const title = titleInput.value.trim();
    if (!title) return;
    await createTask(title, dueInput.value);
    titleInput.value = "";
    dueInput.value = "";
    titleInput.focus();
});

deleteAllBtn.addEventListener("click", async () => {
    if (!confirm("Naozaj zmazať všetky úlohy?")) return;
    await deleteAllTasks();
});

loadTasks();
