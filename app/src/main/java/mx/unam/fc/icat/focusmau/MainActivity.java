package mx.unam.fc.icat.focusmau;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Duraciones en milisegundos
    private static final long FOCUS_DURATION = 25 * 60 * 1000L;
    private static final long SHORT_BREAK =  5 * 60 * 1000L;
    private static final long LONG_BREAK = 15 * 60 * 1000L;
    private static final int  SESSIONS_BEFORE_LONG_BREAK = 4;

    // Estado del timer
    private enum Mode { FOCUS, SHORT_BREAK, LONG_BREAK }
    private Mode currentMode = Mode.FOCUS;
    private int focusSessions = 0;
    private boolean isRunning = false;

    private CountDownTimer countDownTimer;
    private long timeLeftMs;

    // Vistas
    private TextView tvTimerDisplay;
    private Button btnStartStop;
    private LinearLayout sessionDotsContainer;

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
        btnStartStop = findViewById(R.id.btnStartStop);
        sessionDotsContainer = findViewById(R.id.sessionDotsContainer);

        // Tiempo inicial según el modo
        timeLeftMs = getDurationForMode(currentMode);
        updateTimerDisplay(timeLeftMs);
        updateSessionDots();

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
            updateSessionDots();
            notifyUser(getString(R.string.toast_focus_done));
            // Tras 4 enfoques, descanso largo
            if (focusSessions % SESSIONS_BEFORE_LONG_BREAK == 0) {
                switchToMode(Mode.LONG_BREAK);
            } else {
                switchToMode(Mode.SHORT_BREAK);
            }
        } else {
            notifyUser(getString(R.string.toast_break_done));
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
            case LONG_BREAK: return LONG_BREAK;
            default: return FOCUS_DURATION;
        }
    }

    // Formatea y actualiza el texto a MM:SS
    private void updateTimerDisplay(long ms) {
        long minutes = ms / 1000 / 60;
        long seconds = (ms / 1000) % 60;
        tvTimerDisplay.setText(String.format("%02d:%02d", minutes, seconds));
    }

    // Recrea los puntos de sesión según las sesiones completadas
    private void updateSessionDots() {
        sessionDotsContainer.removeAllViews();

        int dotsToFill = focusSessions % SESSIONS_BEFORE_LONG_BREAK;
        if (dotsToFill == 0 && focusSessions > 0) {
            dotsToFill = SESSIONS_BEFORE_LONG_BREAK;
        }

        int dotSizePx = (int) (12 * getResources().getDisplayMetrics().density);
        int dotMarginPx = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < SESSIONS_BEFORE_LONG_BREAK; i++) {
            ImageView dot = new ImageView(this);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSizePx, dotSizePx);
            params.setMargins(dotMarginPx, 0, dotMarginPx, 0);
            dot.setLayoutParams(params);

            if (i < dotsToFill) {
                dot.setImageResource(R.drawable.dot_session_completed);
            } else {
                dot.setImageResource(R.drawable.dot_session_empty);
            }

            sessionDotsContainer.addView(dot);
        }
    }

    // Muestra un Toast y activa la vibración al terminar una sesión
    private void notifyUser(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
        }
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