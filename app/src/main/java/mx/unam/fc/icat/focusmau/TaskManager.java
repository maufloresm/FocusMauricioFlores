package mx.unam.fc.icat.focusmau;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {

    // Clase interna para representar una tarea
    public static class Task {
        private final int id;
        private String title;
        private String description;
        private boolean done;

        public Task(int id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.done = false;
        }

        // Getters
        public int getId() {
            return id;
        }
        public String getTitle() {
            return title;
        }
        public String getDescription() {
            return description;
        }
        public boolean isDone() {
            return done;
        }

        // Setters
        public void setTitle(String title) {
            this.title = title;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public void setDone(boolean done) {
            this.done = done;
        }
    }

    private final List<Task> tasks;
    private int nextId;

    public TaskManager() {
        this.tasks = new ArrayList<>();
        this.nextId = 1;
    }

    // C: CREATE
    public Task addTask(String title, String description) {
        Task task = new Task(nextId, title, description);
        tasks.add(task);
        nextId++;
        return task;
    }

    // R: READ (enlistar todas)
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks); // copia para no exponer la lista interna
    }

    // R: READ (por id)
    public Task getTaskById(int id) {
        for (Task t : tasks) {
            if (t.getId() == id) return t;
        }
        return null;
    }

    // U: UPDATE
    public boolean updateTask(int id, String newTitle, String newDescription, boolean done) {
        Task t = getTaskById(id);
        if (t == null) return false;

        t.setTitle(newTitle);
        t.setDescription(newDescription);
        t.setDone(done);
        return true;
    }

    // D: DELETE
    public boolean deleteTask(int id) {
        Task t = getTaskById(id);
        if (t == null) return false;

        return tasks.remove(t);
    }
}