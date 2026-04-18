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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import mx.unam.fc.icat.focusmau.model.Session;
import mx.unam.fc.icat.focusmau.model.SessionManager;


// Actividad principal que actua como Controlador en el patron MVC.
// Gestiona el ciclo de vida del temporizador Pomodoro, los estados de sesion
// y delega la persistencia al SessionManager (Modelo).
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

    // Preferencias guardadas para detectar cambios al volver a MainActivity
    private String lastAppliedLang;
    private String lastAppliedTheme;

    // Estado del timer
    private enum Mode { FOCUS, SHORT_BREAK, LONG_BREAK }
    private Mode currentMode = Mode.FOCUS;
    private int focusSessions = 0;
    private boolean isRunning = false;

    private CountDownTimer countDownTimer;
    private long timeLeftMs;

    // Sesion activa para registrar en SQLite
    private Session currentSession;
    private SessionManager sessionManager;

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
    private ProgressBar progressBarTimer;

    // Inicializa actividad, vistas, modelo y eventos
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Aplica idioma guardado antes de inflar la vista
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        lastAppliedLang = prefs.getString(getString(R.string.lang_preference_key), "en");
        lastAppliedTheme = prefs.getString(getString(R.string.theme_preference_key), "light");
        applyLanguage(lastAppliedLang);
        applyTheme(lastAppliedTheme);

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa el modelo de persistencia
        sessionManager = new SessionManager(this);

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
        progressBarTimer = findViewById(R.id.progressBarTimer);

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
            if (wasRunning) startTimer();
        } else {
            timeLeftMs = getDurationForMode(currentMode);
            updateTimerDisplay(timeLeftMs);
            updateSessionDots();
            updateSessionsCounter();
            updateSessionStatus();
        }

        btnStartStop.setOnClickListener(v -> {
            if (isRunning) pauseTimer();
            else startTimer();
        });

        // Detiene el timer y reinicia el tiempo del modo actual
        btnReset.setOnClickListener(v -> resetTimer());

        // Salta al siguiente estado ignorando el tiempo restante
        btnSkip.setOnClickListener(v -> skipToNext());

        // Listener individual por chip para mayor confiabilidad
        chipFocus.setOnClickListener(v -> onChipClicked(Mode.FOCUS));
        chipBreak.setOnClickListener(v -> onChipClicked(Mode.SHORT_BREAK));
        chipRest.setOnClickListener(v -> onChipClicked(Mode.LONG_BREAK));
        // Navega al historial de sesiones
        ImageButton btnStats = findViewById(R.id.btnStats);
        btnStats.setOnClickListener(v -> startActivity(new Intent(this, mx.unam.fc.icat.focusmau.view.SessionHistoryActivity.class)));

        // Navega a las preferencias
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, mx.unam.fc.icat.focusmau.view.PreferencesActivity.class)));
    }

    // Logica del CountDownTimer

    // Inicia o reanuda el temporizador y registra el inicio de la sesion
    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        // Crea el registro de la sesion actual al iniciarla
        currentSession = new Session();
        currentSession.setType(getModeLabel(currentMode));
        currentSession.setDuration(getModeMinutes(currentMode));
        currentSession.setDate(new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(new Date()));
        currentSession.setStartTime(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));

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
                onSessionFinished(true);
            }
        }.start();

        isRunning = true;
        btnStartStop.setText(R.string.btn_stop);
    }

    // Pausa el temporizador y actualiza boton
    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isRunning = false;
        btnStartStop.setText(R.string.btn_start);
    }

    // Detiene el timer, guarda la sesion como incompleta y reinicia al tiempo inicial
    private void resetTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        // Guarda la sesion interrumpida si habia una activa
        if (currentSession != null && isRunning) {
            currentSession.setCompleted(false);
            sessionManager.saveSession(currentSession);
            currentSession = null;
        }
        isRunning = false;
        timeLeftMs = getDurationForMode(currentMode);
        updateTimerDisplay(timeLeftMs);
        btnStartStop.setText(R.string.btn_start);
    }

    // Salta al siguiente modo guardando la sesion actual como incompleta
    private void skipToNext() {
        if (countDownTimer != null) countDownTimer.cancel();
        // Guarda la sesion interrumpida si habia una activa
        if (currentSession != null && isRunning) {
            currentSession.setCompleted(false);
            sessionManager.saveSession(currentSession);
            currentSession = null;
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
                    .setPositiveButton(getString(R.string.dialog_chip_confirm), (dialog, which) -> {
                        // Guarda la sesion interrumpida antes de cambiar
                        if (currentSession != null) {
                            currentSession.setCompleted(false);
                            sessionManager.saveSession(currentSession);
                            currentSession = null;
                        }
                        switchToMode(selectedMode);
                    })
                    .setNegativeButton(getString(R.string.dialog_chip_cancel), (dialog, which) ->
                            syncChipSelection())
                    .show();
        } else {
            switchToMode(selectedMode);
        }
    }

    // Guarda la sesion completada o interrumpida y transiciona al siguiente modo
    private void onSessionFinished(boolean completed) {
        if (currentSession != null) {
            currentSession.setCompleted(completed);
            sessionManager.saveSession(currentSession);
            currentSession = null;
        }

        if (currentMode == Mode.FOCUS) {
            focusSessions++;
            updateSessionDots();
            updateSessionsCounter();
            notifyUser(getString(R.string.toast_focus_done));
            if (focusSessions % SESSIONS_BEFORE_LONG_BREAK == 0) {
                switchToMode(Mode.LONG_BREAK);
            } else {
                switchToMode(Mode.SHORT_BREAK);
            }
        } else {
            notifyUser(getString(R.string.toast_break_done));
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

        if (mode == Mode.SHORT_BREAK || mode == Mode.LONG_BREAK) {
            showRandomMotivationalPhrase();
        } else {
            tvMotivationalQuote.setText("");
        }

        startTimer();
    }

    // Helpers

    // Obtiene la duracion en ms segun el modo
    private long getDurationForMode(Mode mode) {
        switch (mode) {
            case SHORT_BREAK: return SHORT_BREAK;
            case LONG_BREAK: return LONG_BREAK;
            default: return FOCUS_DURATION;
        }
    }

    // Obtiene la duracion en minutos segun el modo (para guardar en DB)
    private int getModeMinutes(Mode mode) {
        switch (mode) {
            case SHORT_BREAK: return 5;
            case LONG_BREAK: return 15;
            default: return 25;
        }
    }

    // Obtiene la etiqueta de texto del modo (para guardar en DB)
    private String getModeLabel(Mode mode) {
        switch (mode) {
            case SHORT_BREAK: return getString(R.string.status_short_break);
            case LONG_BREAK: return getString(R.string.status_long_break);
            default: return getString(R.string.status_focus);
        }
    }

    // Formatea el texto del timer a MM:SS y actualiza la barra de progreso
    private void updateTimerDisplay(long ms) {
        long minutes = ms / 1000 / 60;
        long seconds = (ms / 1000) % 60;
        tvTimerDisplay.setText(String.format("%02d:%02d", minutes, seconds));

        // Actualiza el ProgressBar en base al tiempo restante
        if (progressBarTimer != null) {
            progressBarTimer.setMax((int) getDurationForMode(currentMode));
            progressBarTimer.setProgress((int) ms);
        }
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
        if (dotsToFill == 0 && focusSessions > 0) dotsToFill = SESSIONS_BEFORE_LONG_BREAK;

        int dotSizePx = (int) (12 * getResources().getDisplayMetrics().density);
        int dotMarginPx = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < SESSIONS_BEFORE_LONG_BREAK; i++) {
            ImageView dot = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSizePx, dotSizePx);
            params.setMargins(dotMarginPx, 0, dotMarginPx, 0);
            dot.setLayoutParams(params);
            dot.setImageResource(i < dotsToFill
                    ? R.drawable.dot_session_completed
                    : R.drawable.dot_session_empty);
            sessionDotsContainer.addView(dot);
        }
    }

    // Elige y muestra una frase aleatoria del array de recursos (soporta traduccion)
    private void showRandomMotivationalPhrase() {
        String[] phrases = getResources().getStringArray(R.array.motivational_phrases);
        int index = new Random().nextInt(phrases.length);
        tvMotivationalQuote.setText(phrases[index]);
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

    // Al volver de PreferencesActivity, detecta si cambio el idioma o el tema
    // y recrea la actividad para aplicar los cambios correctamente.
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String currentLang = prefs.getString(getString(R.string.lang_preference_key), "en");
        String currentTheme = prefs.getString(getString(R.string.theme_preference_key), "light");

        boolean langChanged = !currentLang.equals(lastAppliedLang);
        boolean themeChanged = !currentTheme.equals(lastAppliedTheme);

        if (langChanged || themeChanged) {
            lastAppliedLang = currentLang;
            lastAppliedTheme = currentTheme;
            recreate();
        }
    }

    // Aplica el modo oscuro o claro segun la preferencia del usuario.
    private void applyTheme(String themeValue) {
        switch (themeValue) {
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    // Aplica el idioma seleccionado actualizando el Locale de la aplicacion.
    private void applyLanguage(String langCode) {
        java.util.Locale locale = new java.util.Locale(langCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(
                config, getBaseContext().getResources().getDisplayMetrics());
    }

    // Guarda el estado del timer ante cambios de configuracion
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
        if (countDownTimer != null) countDownTimer.cancel();
    }
}