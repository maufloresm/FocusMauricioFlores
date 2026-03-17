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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Duraciones en milisegundos
    private static final long FOCUS_DURATION = 25 * 60 * 1000L;
    private static final long SHORT_BREAK = 5 * 60 * 1000L;
    private static final long LONG_BREAK = 15 * 60 * 1000L;
    private static final int SESSIONS_BEFORE_LONG_BREAK = 4;

    // Claves para guardar y restaurar el estado de la instancia
    private static final String KEY_TIME_LEFT = "key_time_left";
    private static final String KEY_FOCUS_SESSIONS = "key_focus_sessions";
    private static final String KEY_CURRENT_MODE = "key_current_mode";
    private static final String KEY_IS_RUNNING = "key_is_running";

    // Frases motivacionales mostradas al iniciar un descanso
    private static final String[] MOTIVATIONAL_PHRASES = {
            "Excelente trabajo! Mereces este descanso",
            "Descansa, tu cerebro lo agradecera",
            "Un paso mas hacia tu meta",
            "El descanso es parte del exito",
            "Sigue asi, vas increible!",
            "Respira, recarga y vuelve con todo",
            "La constancia construye grandes cosas"
    };

    // Estado del timer
    private enum Mode { FOCUS, SHORT_BREAK, LONG_BREAK }
    private Mode currentMode = Mode.FOCUS;
    private int focusSessions = 0;
    private boolean isRunning = false;
    private boolean isChipListenerActive = true;

    private CountDownTimer countDownTimer;
    private long timeLeftMs;

    // Vistas
    private TextView tvTimerDisplay;
    private TextView tvSessionStatus;
    private TextView tvSessionsCompleted;
    private TextView tvMotivationalQuote;
    private Button btnStartStop;
    private ImageButton btnReset;
    private ImageButton btnSkip;
    private LinearLayout sessionDotsContainer;
    private ChipGroup chipGroupMode;
    private Chip chipFocus;
    private Chip chipBreak;
    private Chip chipRest;

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
        tvSessionStatus = findViewById(R.id.tvSessionStatus);
        tvSessionsCompleted = findViewById(R.id.tvSessionsCompleted);
        tvMotivationalQuote = findViewById(R.id.tvMotivationalQuote);
        btnStartStop = findViewById(R.id.btnStartStop);
        btnReset = findViewById(R.id.btnReset);
        btnSkip = findViewById(R.id.btnSkip);
        sessionDotsContainer = findViewById(R.id.sessionDotsContainer);
        chipGroupMode = findViewById(R.id.chipGroupMode);
        chipFocus = findViewById(R.id.chipFocus);
        chipBreak = findViewById(R.id.chipBreak);
        chipRest = findViewById(R.id.chipRest);

        // Restaura el estado si existe, si no inicializa por defecto
        if (savedInstanceState != null) {
            timeLeftMs = savedInstanceState.getLong(KEY_TIME_LEFT);
            focusSessions = savedInstanceState.getInt(KEY_FOCUS_SESSIONS);
            currentMode = Mode.values()[savedInstanceState.getInt(KEY_CURRENT_MODE)];
            boolean wasRunning = savedInstanceState.getBoolean(KEY_IS_RUNNING);

            updateTimerDisplay(timeLeftMs);
            updateSessionDots();
            updateSessionsCounter();
            updateSessionStatus();
            syncChipSelection();

            // Reanuda el timer si estaba corriendo antes de la rotacion
            if (wasRunning) startTimer();
        } else {
            // Estado inicial por defecto
            timeLeftMs = getDurationForMode(currentMode);
            updateTimerDisplay(timeLeftMs);
            updateSessionDots();
            updateSessionsCounter();
            updateSessionStatus();
        }

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

        // Asigna listener individual a cada chip para mayor confiabilidad
        chipFocus.setOnClickListener(v -> onChipClicked(Mode.FOCUS));
        chipBreak.setOnClickListener(v -> onChipClicked(Mode.SHORT_BREAK));
        chipRest.setOnClickListener(v -> onChipClicked(Mode.LONG_BREAK));
    }

    // Lógica del CountDownTimer

    // Inicia o reanuda el temporizador
    private void startTimer() {
        // Evita multiples timers simultaneos
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

    // Pausa el temporizador y actualiza boton
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

    // Maneja el tap en un chip y pide confirmacion si hay sesion activa
    private void onChipClicked(Mode selectedMode) {
        if (selectedMode == currentMode) return;

        if (isRunning) {
            syncChipSelection();
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_chip_title))
                    .setMessage(getString(R.string.dialog_chip_message))
                    .setPositiveButton(getString(R.string.dialog_chip_confirm), (dialog, which) ->
                            switchToMode(selectedMode))
                    .setNegativeButton(getString(R.string.dialog_chip_cancel), (dialog, which) ->
                            syncChipSelection())
                    .show();
        } else {
            switchToMode(selectedMode);
        }
    }

    // Maneja el fin de la sesion actual
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

    // Cambia de modo, actualiza UI y arranca el timer
    private void switchToMode(Mode mode) {
        currentMode = mode;
        timeLeftMs = getDurationForMode(mode);
        updateTimerDisplay(timeLeftMs);
        updateSessionStatus();
        syncChipSelection();

        // Muestra frase motivacional solo al iniciar un descanso
        if (mode == Mode.SHORT_BREAK || mode == Mode.LONG_BREAK) {
            showRandomMotivationalPhrase();
        } else {
            tvMotivationalQuote.setText("");
        }

        startTimer();
    }

    // Helpers

    // Obtiene la duracion segun el modo
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

    // Actualiza el texto de estado segun el modo activo
    private void updateSessionStatus() {
        switch (currentMode) {
            case FOCUS:
                tvSessionStatus.setText(getString(R.string.status_focus));
                break;
            case SHORT_BREAK:
                tvSessionStatus.setText(getString(R.string.status_short_break));
                break;
            case LONG_BREAK:
                tvSessionStatus.setText(getString(R.string.status_long_break));
                break;
        }
    }

    // Actualiza el contador de sesiones completadas en pantalla
    private void updateSessionsCounter() {
        tvSessionsCompleted.setText(getString(R.string.label_sessions_completed_count, focusSessions));
    }

    // Recrea los puntos de sesion segun las sesiones completadas
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

    // Sincroniza la seleccion visual del chip con el modo actual
    private void syncChipSelection() {
        chipFocus.setChecked(currentMode == Mode.FOCUS);
        chipBreak.setChecked(currentMode == Mode.SHORT_BREAK);
        chipRest.setChecked(currentMode == Mode.LONG_BREAK);
    }

    // Muestra un Toast y activa la vibracion al terminar una sesion
    private void notifyUser(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    // Guarda el estado del timer ante cambios de configuracion como rotacion de pantalla
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_TIME_LEFT, timeLeftMs);
        outState.putInt(KEY_FOCUS_SESSIONS, focusSessions);
        outState.putInt(KEY_CURRENT_MODE, currentMode.ordinal());
        outState.putBoolean(KEY_IS_RUNNING, isRunning);
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