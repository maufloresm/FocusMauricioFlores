# FocusBuddy

**Flores Muñoz Mauricio**
**No. de Cuenta: 320236589**

---

## Tareas realizadas

Se implemento una aplicacion Pomodoro completa para Android. Las tareas principales fueron: diseno de la interfaz con Material Components (cards, chips, RecyclerView) sin valores hardcoded. Motor de temporizador con `CountDownTimer` para los tres modos (foco 25 min, descanso corto 5 min, descanso largo 15 min). Persistencia del historial en SQLite mediante `SessionManager` con operaciones en hilo secundario. Pantalla de historial con filtros por periodo (hoy, esta semana, todo). Sistema de preferencias con `SharedPreferences` para cambiar tema e idioma en tiempo real. Feedback para el usuario con `VibrationEffect` al finalizar cada sesion. Además agregue un `ProgressBar` en la parte del timer para que se viera en tiempo real que tanto has avanzado en cada sesión.

Lo que mas costo trabajo fue sincronizar el `CountDownTimer` con las escrituras a SQLite, garantizando que cada sesion quedara registrada correctamente como completada o interrumpida sin bloquear la interfaz.

---

## Segunda version

Se agregaria soporte para notificaciones en segundo plano usando `WorkManager`, estadisticas visuales del historial con graficas por dia o semana, sonidos de ambiente configurables durante el foco, y la posibilidad de exportar el historial en CSV. En cuanto a arquitectura, se migraria de MVC a MVVM con `ViewModel`, `LiveData` y `Room` para un manejo del estado mas robusto y testeable.
