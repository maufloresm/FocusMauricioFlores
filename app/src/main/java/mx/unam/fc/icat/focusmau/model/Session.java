package mx.unam.fc.icat.focusmau.model;

/**
 * Modelo que representa una sesion Pomodoro registrada.
 * Contiene el tipo, fecha, hora de inicio, duracion y estado de completitud.
 */
public class Session {
    private String type;       // Tipo: Enfoque, Descanso o Descanso largo
    private String date;       // Formato: EEE, dd MMM yyyy
    private String startTime;  // Formato: HH:mm
    private int duration;      // Duracion en minutos: 25, 5 o 15
    private boolean completed; // true si termino naturalmente, false si fue interrumpida

    public Session() {}

    public Session(String type, String date, String startTime, int duration, boolean completed) {
        this.type = type;
        this.date = date;
        this.startTime = startTime;
        this.duration = duration;
        this.completed = completed;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}