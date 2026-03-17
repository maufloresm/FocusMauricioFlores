package mx.unam.fc.icat.focusmau;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Duraciones en milisegundos
    private static final long FOCUS_DURATION  = 25 * 60 * 1000L;
    private static final long SHORT_BREAK     =  5 * 60 * 1000L;
    private static final long LONG_BREAK      = 15 * 60 * 1000L;
    private static final int  SESSIONS_BEFORE_LONG_BREAK = 4;

    // Estado del timer
    private enum Mode { FOCUS, SHORT_BREAK, LONG_BREAK }
    private Mode    currentMode    = Mode.FOCUS;
    private int     focusSessions  = 0;   // Sesiones completadas
    private boolean isRunning      = false;

    private CountDownTimer countDownTimer;
    private long           timeLeftMs;    // Tiempo restante

    // Vistas
    private TextView tvTimerDisplay;
    private Button   btnStartStop;

    // Inicializa actividad, vistas y eventos
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvTimerDisplay = findViewById(R.id.tvTimerDisplay);
        btnStartStop   = findViewById(R.id.btnStartStop);

        // Tiempo inicial según el modo
        timeLeftMs = getDurationForMode(currentMode);
        updateTimerDisplay(timeLeftMs);

        btnStartStop.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });
    }

    // Lógica del CountDownTimer

    // Inicia o reanuda el temporizador
    private void startTimer() {
        // Evita múltiples timers simultáneos
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(timeLeftMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMs = millisUntilFinished;
                updateTimerDisplay(timeLeftMs);
            }

            @Override
            public void onFinish() {
                isRunning = false;
                timeLeftMs = 0;
                updateTimerDisplay(0);
                onSessionFinished();
            }
        }.start();

        isRunning = true;
        btnStartStop.setText(R.string.btn_stop);
    }

    // Pausa el temporizador y actualiza botón
    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        btnStartStop.setText(R.string.btn_start);
    }

    // Maneja el fin de la sesión actual
    private void onSessionFinished() {
        if (currentMode == Mode.FOCUS) {
            focusSessions++;
            // Tras 4 enfoques, descanso largo
            if (focusSessions % SESSIONS_BEFORE_LONG_BREAK == 0) {
                switchToMode(Mode.LONG_BREAK);
            } else {
                switchToMode(Mode.SHORT_BREAK);
            }
        } else {
            // Tras descanso, vuelve a enfoque
            switchToMode(Mode.FOCUS);
        }
    }

    // Cambia de modo y arranca el timer
    private void switchToMode(Mode mode) {
        currentMode = mode;
        timeLeftMs  = getDurationForMode(mode);
        updateTimerDisplay(timeLeftMs);
        startTimer();
    }

    // Helpers

    // Obtiene la duración según el modo
    private long getDurationForMode(Mode mode) {
        switch (mode) {
            case SHORT_BREAK: return SHORT_BREAK;
            case LONG_BREAK:  return LONG_BREAK;
            default:          return FOCUS_DURATION;
        }
    }

    // Formatea y actualiza el texto a MM:SS
    private void updateTimerDisplay(long ms) {
        long minutes = ms / 1000 / 60;
        long seconds = (ms / 1000) % 60;
        tvTimerDisplay.setText(String.format("%02d:%02d", minutes, seconds));
    }

    // Libera recursos al salir
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}