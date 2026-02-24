# Práctica: Android Studio + AVD + Git + TaskManager (CRUD)

## 1) Descripción breve de la práctica
En esta práctica configuré el entorno de desarrollo en **Android Studio** (SDK y emuladores AVD), trabajé con **Git** usando ramas y resolución de conflictos, y programé una clase **TaskManager** en Java para administrar una lista de **Tareas Sugeridas** con operaciones **CRUD** (crear, leer/listar, actualizar y eliminar) usando una estructura tipo lista.


## 2) Problemas con la aceleración de hardware o creación de AVD (y solución)
No tuve problemas realmente con la configuración de los emuladores pero si los sentí un poco lentos.


## 3) ¿Por qué elegiste ArrayList sobre otras opciones?
Elegí **ArrayList** porque se adapta muy bien a una lista de tareas en memoria:

- Permite agregar tareas fácilmente (`add`) y recorrer/listar con un `for`.
- Tiene acceso rápido por índice.
- Fácil de entender.


## 4) Si las tareas se guardaran en un servidor remoto, ¿qué cambiaría en el manejo de excepciones?
Si las tareas se guardaran en un servidor remoto, el manejo de excepciones tendría que considerar que ya no depende solo de memoria local, sino de red y servicios externos.

**Qué cambiaría:**
- Tendría que manejar errores de **red** y **conectividad**, por ejemplo:
  - Sin internet
  - Tiempo de espera (timeout)
  - Respuestas incompletas
- Tendría que manejar errores del **servidor**:
  - 500 (error interno)
  - 503 (servicio no disponible)
  - Respuestas inválidas (JSON mal formado, por ejemplo)
- Tendría que manejar errores de **autenticación/autorización**:
  - 401 (no autorizado)
  - 403 (prohibido)
- Las funciones CRUD probablemente serían **asíncronas** (no bloquear la app):
  - Usar callbacks, Futures/Promises, corrutinas (si fuera Kotlin), etc.
- En vez de fallar “silenciosamente”, debería:
  - **Registrar (log)** el error
  - Mostrar un **mensaje amigable** al usuario
  - Implementar **reintentos** o fallback (por ejemplo, guardar temporalmente en local y sincronizar después)
