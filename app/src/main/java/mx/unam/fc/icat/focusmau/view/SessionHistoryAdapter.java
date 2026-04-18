package mx.unam.fc.icat.focusmau.view;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;
import mx.unam.fc.icat.focusmau.R;
import mx.unam.fc.icat.focusmau.model.Session;

/**
 * Adaptador para gestionar y reciclar las vistas del historial de sesiones.
 * Vincula cada objeto Session con su representacion visual en el RecyclerView.
 */
public class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.SessionViewHolder> {

    /**
     * ViewHolder que mantiene referencias a los widgets de cada item
     * para evitar llamadas repetitivas a findViewById.
     */
    class SessionViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSessionType;
        private TextView tvSessionDate;
        private TextView tvSessionTime;
        private TextView tvSessionDuration;
        private Chip     chipStatus;

        SessionViewHolder(View itemView) {
            super(itemView);
            tvSessionType     = itemView.findViewById(R.id.tvSessionType);
            tvSessionDate     = itemView.findViewById(R.id.tvSessionDate);
            tvSessionTime     = itemView.findViewById(R.id.tvSessionTime);
            tvSessionDuration = itemView.findViewById(R.id.tvSessionDuration);
            chipStatus        = itemView.findViewById(R.id.chipSessionStatus);
        }
    }

    private final List<Session> dataset;
    private final Resources     resources;

    /**
     * Constructor del adaptador.
     * @param sessions Lista inicial de sesiones a mostrar.
     * @param res      Referencia a los recursos de la aplicacion.
     */
    public SessionHistoryAdapter(List<Session> sessions, Resources res) {
        this.dataset   = new ArrayList<>();
        this.resources = res;
        if (sessions != null) this.dataset.addAll(sessions);
    }

    /**
     * Infla el layout XML para cada entrada del RecyclerView.
     */
    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_session_entry, parent, false);
        return new SessionViewHolder(view);
    }

    /**
     * Vincula los datos de la sesion con los widgets del ViewHolder.
     * Aplica retroalimentacion visual segun si la sesion fue completada o interrumpida.
     * @param holder   ViewHolder con las referencias a los widgets.
     * @param position Posicion del elemento en el dataset.
     */
    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        Session session = dataset.get(position);
        holder.tvSessionType.setText(session.getType());
        holder.tvSessionDate.setText(session.getDate());
        holder.tvSessionTime.setText(session.getStartTime());
        holder.tvSessionDuration.setText(session.getDuration() + " min");

        if (session.isCompleted()) {
            holder.chipStatus.setText(R.string.status_completed);
            holder.chipStatus.setChipBackgroundColorResource(R.color.color_surface);
            holder.chipStatus.setTextColor(resources.getColor(R.color.color_primary, null));
        } else {
            holder.chipStatus.setText(R.string.status_interrupted);
            holder.chipStatus.setChipBackgroundColorResource(R.color.color_border);
            holder.chipStatus.setTextColor(resources.getColor(R.color.color_text_secondary, null));
        }
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    /**
     * Reemplaza el dataset actual y notifica al RecyclerView para redibujar.
     * @param newSessions Nueva lista de sesiones a mostrar.
     */
    public void updateData(List<Session> newSessions) {
        dataset.clear();
        if (newSessions != null) dataset.addAll(newSessions);
        notifyDataSetChanged();
    }
}