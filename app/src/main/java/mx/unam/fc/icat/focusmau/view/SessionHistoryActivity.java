package mx.unam.fc.icat.focusmau.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.List;
import mx.unam.fc.icat.focusmau.R;
import mx.unam.fc.icat.focusmau.model.Session;
import mx.unam.fc.icat.focusmau.model.SessionManager;

/**
 * Actividad que muestra el historial cronologico de sesiones Pomodoro.
 * Permite filtrar por periodo: Hoy, Esta semana o Todo el historial.
 */
public class SessionHistoryActivity extends AppCompatActivity {

    // Componentes de la interfaz de usuario
    private Toolbar         toolbar;
    private TextView        tvResultCount;
    private ConstraintLayout layoutEmpty;
    private RecyclerView    recyclerView;
    private ChipGroup       chipGroupFilter;
    private Chip            chipFilterToday;
    private Chip            chipFilterWeek;
    private Chip            chipFilterAll;

    // Logica y datos
    private SessionHistoryAdapter adapter;
    private SessionManager        sessionManager;

    /**
     * Inicializa la actividad, vincula vistas, configura toolbar,
     * RecyclerView, filtros y muestra el historial inicial.
     * @param savedInstanceState Estado previo de la actividad, si existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_session);
        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupFilterLogic();
        updateHistoryDisplay(sessionManager.getAllSessions());
    }

    /**
     * Vincula variables con componentes del XML incluyendo chips de filtrado.
     */
    private void bindViews() {
        toolbar          = findViewById(R.id.history_toolbar);
        tvResultCount    = findViewById(R.id.tvResultCount);
        layoutEmpty      = findViewById(R.id.layoutEmpty);
        recyclerView     = findViewById(R.id.recyclerViewHistory);
        chipGroupFilter  = findViewById(R.id.chipGroupFilter);
        chipFilterToday  = findViewById(R.id.chipFilterToday);
        chipFilterWeek   = findViewById(R.id.chipFilterWeek);
        chipFilterAll    = findViewById(R.id.chipFilterAll);
        sessionManager   = new SessionManager(this);
    }

    /**
     * Configura la Toolbar con boton de retroceso y titulo desde recursos.
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_history);
        }
    }

    /**
     * Inicializa el RecyclerView con su LayoutManager y el adaptador
     * vinculado al historial completo de sesiones.
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionHistoryAdapter(sessionManager.getAllSessions(), getResources());
        recyclerView.setAdapter(adapter);
    }

    /**
     * Configura los listeners de cada chip de filtrado.
     * Cada chip dispara una consulta distinta al SessionManager.
     */
    private void setupFilterLogic() {
        chipFilterToday.setOnClickListener(v ->
                updateHistoryDisplay(sessionManager.getSessionsToday()));
        chipFilterWeek.setOnClickListener(v ->
                updateHistoryDisplay(sessionManager.getSessionsThisWeek()));
        chipFilterAll.setOnClickListener(v ->
                updateHistoryDisplay(sessionManager.getAllSessions()));
    }

    /**
     * Actualiza la lista del RecyclerView, gestiona el empty state
     * y actualiza el contador de resultados.
     * @param sessions Lista de sesiones a mostrar.
     */
    private void updateHistoryDisplay(List<Session> sessions) {
        boolean isEmpty = (sessions == null || sessions.isEmpty());
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvResultCount.setText(getString(R.string.session_count,
                sessions != null ? sessions.size() : 0));
        adapter.updateData(sessions);
    }

    /**
     * Maneja el boton de retroceso de la Toolbar para cerrar la actividad.
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}