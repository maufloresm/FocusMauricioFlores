# Práctica 2 - Lógica y UI del Pomodoro Timer

## Descripción

Esta es una aplicación de productividad diseñada para ayudarte a gestionar mejor tu tiempo mediante la técnica Pomodoro. 
La idea es trabajar sin distracciones en bloques de 25 minutos (sesiones de enfoque), alternando con descansos cortos de 5 minutos y un descanso largo de 15 minutos cada vez que completes 4 sesiones.

---

## Preguntas

### 1. ¿Cuál fue el mayor reto al gestionar el CountDownTimer y como evitaste que se crearan multiples instancias al presionar el boton repetidamente?

El mayor reto fue que `CountDownTimer` no es reutilizable: una vez iniciado, no se puede pausar ni reiniciar directamente. Cada llamada a `startTimer()` necesita crear una nueva instancia, lo que significa que si el usuario presiona el boton varias veces rapido, se acumularian multiples timers corriendo en paralelo, mostrando comportamientos raros en la pantalla.

La solucion fue cancelar siempre la instancia anterior antes de crear una nueva. Al inicio de `startTimer()` se verifica si `countDownTimer != null` y se llama a `cancel()` antes de instanciar el nuevo. Esto garantiza que en cualquier momento solo existe un timer activo.
Además, el tiempo restante se guarda en la variable `timeLeftMs` en cada tick, lo que permite reanudar desde donde se pauso sin perder el progreso.

---

### 2. ¿Por qué es preferible usar un LinearLayout con addView para los puntos de progreso en lugar de declarar 4 ImageViews estaticos en el XML?

Porque declarar 4 `ImageViews` estaticos en el XML funcionaria unicamente si el numero de sesiones fuera siempre fijo e inamovible. El enfoque con `LinearLayout` y `addView` es preferible por ya que el número de puntos puede cambiar en el futuro si se modifica `SESSIONS_BEFORE_LONG_BREAK`sin tocar el XML. La logica de cuantos puntos mostrar, que drawable asignar y que margenes aplicar esta centralizada en el metodo `updateSessionDots()`, lo que facilita el mantenimiento. Con `ImageViews` estaticos habria que manejar la visibilidad y el drawable de cada uno por separado con referencias individuales, aumentando el codigo repetitivo.

---

### 3. Si quisieramos añadir una funcion para que el usuario personalice sus propios tiempos de enfoque, ¿qué parte de tu logica actual tendria que cambiar y como lo abordarias?

Actualmente las duraciones estan definidas como constantes estaticas en la clase:
```java
private static final long FOCUS_DURATION = 25 * 60 * 1000L;
private static final long SHORT_BREAK    =  5 * 60 * 1000L;
private static final long LONG_BREAK     = 15 * 60 * 1000L;
```
Para permitir la personalización, bastaría con reemplazar las constantes por variables de instancia que se alimenten de una pantalla de configuración y se gestionen con SharedPreferences desde el onCreate(), sin necesidad de alterar la lógica estructural de los métodos y ciclos de sesión existentes.

---

### 4. ¿Cómo harian para que el tiempo del temporizador se mantenga si el usuario minimiza la app?

Para mantener el temporizador al minimizar la app, la solución ideal es implementar un Foreground Service con una notificación persistente que corra en segundo plano, aunque una alternativa más sencilla sería guardar el timestamp de salida en SharedPreferences durante el onPause() y recalcular el tiempo restante al volver a la app en onResume().
