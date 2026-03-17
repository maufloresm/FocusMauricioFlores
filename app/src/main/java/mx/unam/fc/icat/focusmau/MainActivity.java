package mx.unam.fc.icat.focusmau;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Duraciones en milisegundos
    private static final long FOCUS_DURATION = 25 * 60 * 1000L;
    private static final long SHORT_BREAK = 5 * 60 * 1000L;
    private static final long LONG_BREAK = 15 * 60 * 1000L;
    private static final int SESSIONS_BEFORE_LONG_BREAK = 4;

    // Frases motivacionales mostradas al iniciar un descanso
    private static final String[] MOTIVATIONAL_PHRASES = {
            "¡Excelente trabajo! Mereces este descanso",
            "Descansa, tu cerebro lo agradecerá",
            "Un paso más hacia tu meta",
            "El descanso es parte del éxito",
            "¡Sigue así, vas increíble!",
            "Respira, recarga y vuelve con todo",
            "La constancia construye grandes cosas"
    };

    // Estado del timer
    private enum Mode { FOCUS, SHORT_BREAK, LONG_BREAK }
    private Mode currentMode = Mode.FOCUS;
    private int focusSessions = 0;
    private boolean isRunning = false;

    private CountDownTimer countDownTimer;
    private long timeLeftMs;

    // Vistas
    private TextView tvTimerDisplay;
    private TextView tvSessionsCompleted;
    private TextView tvMotivationalQuote;
    private Button btnStartStop;
    private ImageButton btnReset;
    private ImageButton btnSkip;
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
        tvSessionsCompleted = findViewById(R.id.tvSessionsCompleted);
        tvMotivationalQuote = findViewById(R.id.tvMotivationalQuote);
        btnStartStop = findViewById(R.id.btnStartStop);
        btnReset = findViewById(R.id.btnReset);
        btnSkip = findViewById(R.id.btnSkip);
        sessionDotsContainer = findViewById(R.id.sessionDotsContainer);

        // Tiempo inicial según el modo
        timeLeftMs = getDurationForMode(currentMode);
        updateTimerDisplay(timeLeftMs);
        updateSessionDots();
        updateSessionsCounter();

        btnStartStop.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        // Detiene el timer y reinicia el tiempo del modo actual
        btnReset.setOnClickListener(v -> resetTimer());

        // Salta al siguiente estado ignorando el tiempo restante
        btnSkip.setOnClickListener(v -> skipToNext());
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

    // Detiene el timer y reinicia al tiempo inicial del modo actual
    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        timeLeftMs = getDurationForMode(currentMode);
        updateTimerDisplay(timeLeftMs);
        btnStartStop.setText(R.string.btn_start);
    }

    // Salta al siguiente modo sin esperar a que termine el timer
    private void skipToNext() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;

        if (currentMode == Mode.FOCUS) {
            focusSessions++;
            updateSessionDots();
            updateSessionsCounter();
            if (focusSessions % SESSIONS_BEFORE_LONG_BREAK == 0) {
                switchToMode(Mode.LONG_BREAK);
            } else {
                switchToMode(Mode.SHORT_BREAK);
            }
        } else {
            switchToMode(Mode.FOCUS);
        }
    }

    // Maneja el fin de la sesión actual
    private void onSessionFinished() {
        if (currentMode == Mode.FOCUS) {
            focusSessions++;
            updateSessionDots();
            updateSessionsCounter();
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

    // Cambia de modo, muestra frase si es descanso y arranca el timer
    private void switchToMode(Mode mode) {
        currentMode = mode;
        timeLeftMs = getDurationForMode(mode);
        updateTimerDisplay(timeLeftMs);

        // Muestra frase motivacional solo al iniciar un descanso
        if (mode == Mode.SHORT_BREAK || mode == Mode.LONG_BREAK) {
            showRandomMotivationalPhrase();
        } else {
            tvMotivationalQuote.setText("");
        }

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

    // Actualiza el contador de sesiones completadas en pantalla
    private void updateSessionsCounter() {
        tvSessionsCompleted.setText(getString(R.string.label_sessions_completed_count, focusSessions));
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

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(dotSizePx, dotSizePx);
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

    // Elige y muestra una frase aleatoria del array
    private void showRandomMotivationalPhrase() {
        int index = new Random().nextInt(MOTIVATIONAL_PHRASES.length);
        tvMotivationalQuote.setText(MOTIVATIONAL_PHRASES[index]);
    }

    // Muestra un Toast y activa la vibración al terminar una sesión
    private void notifyUser(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
            }
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