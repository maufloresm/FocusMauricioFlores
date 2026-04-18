package mx.unam.fc.icat.focusmau.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Gestiona la persistencia de sesiones Pomodoro en una base de datos SQLite local.
 * Actua como unico punto de acceso a los datos (patron repositorio simple).
 */
public class SessionManager extends SQLiteOpenHelper {

    private static final String DATABASE_NAME    = "FocusMau.db";
    private static final int    DATABASE_VERSION  = 1;

    // Nombres de tabla y columnas
    public static final String TABLE_SESSIONS    = "sessions";
    public static final String COLUMN_ID         = "id";
    public static final String COLUMN_TYPE       = "type";
    public static final String COLUMN_DATE       = "date";
    public static final String COLUMN_START      = "startTime";
    public static final String COLUMN_DURATION   = "duration";
    public static final String COLUMN_COMPLETED  = "completed";

    /**
     * Constructor que inicializa el helper con el nombre y version de la base de datos.
     * @param context Contexto de la aplicacion necesario para acceder al sistema de archivos.
     */
    public SessionManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Crea la tabla de sesiones la primera vez que se abre la base de datos.
     * @param db Instancia de la base de datos recien creada.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_SESSIONS + " ("
                + COLUMN_ID        + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TYPE      + " TEXT, "
                + COLUMN_DATE      + " TEXT, "
                + COLUMN_START     + " TEXT, "
                + COLUMN_DURATION  + " INTEGER, "
                + COLUMN_COMPLETED + " INTEGER"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    /**
     * Guarda una sesion en la base de datos en un hilo secundario para no
     * bloquear el hilo principal de la interfaz de usuario.
     * @param session Objeto Session con los datos a persistir.
     */
    public void saveSession(Session session) {
        new Thread(() -> {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(COLUMN_TYPE,      session.getType());
                values.put(COLUMN_DATE,      session.getDate());
                values.put(COLUMN_START,     session.getStartTime());
                values.put(COLUMN_DURATION,  session.getDuration());
                values.put(COLUMN_COMPLETED, session.isCompleted() ? 1 : 0);
                db.insert(TABLE_SESSIONS, null, values);
                db.close();
            } catch (Exception e) {
                Log.e("SessionManager", "Error al guardar sesion: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Recupera todas las sesiones ordenadas por ID descendente (mas reciente primero).
     * @return Lista completa del historial de sesiones.
     */
    public List<Session> getAllSessions() {
        List<Session> sessionList = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.query(TABLE_SESSIONS, null, null, null, null, null, COLUMN_ID + " DESC");
            if (cursor.moveToFirst()) {
                do {
                    sessionList.add(cursorToSession(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            Log.e("SessionManager", "Error al recuperar sesiones: " + e.getMessage());
        }
        return sessionList;
    }

    /**
     * Recupera las sesiones registradas en la fecha actual.
     * @return Lista de sesiones del dia de hoy.
     */
    public List<Session> getSessionsToday() {
        List<Session> sessionList = new ArrayList<>();
        try {
            String today = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                    .format(new Date());
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.query(TABLE_SESSIONS, null,
                    COLUMN_DATE + " = ?", new String[]{today},
                    null, null, COLUMN_ID + " DESC");
            if (cursor.moveToFirst()) {
                do {
                    sessionList.add(cursorToSession(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            Log.e("SessionManager", "Error al filtrar sesiones de hoy: " + e.getMessage());
        }
        return sessionList;
    }

    /**
     * Recupera las sesiones registradas en los ultimos 7 dias.
     * @return Lista de sesiones de la semana actual.
     */
    public List<Session> getSessionsThisWeek() {
        List<Session> sessionList = new ArrayList<>();
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -7);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
            for (Session s : getAllSessions()) {
                try {
                    Date sessionDate = sdf.parse(s.getDate());
                    if (sessionDate != null && sessionDate.after(cal.getTime())) {
                        sessionList.add(s);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            Log.e("SessionManager", "Error al filtrar sesiones de la semana: " + e.getMessage());
        }
        return sessionList;
    }

    /**
     * Convierte una fila del cursor en un objeto Session.
     * @param cursor Cursor posicionado en la fila a convertir.
     * @return Objeto Session con los datos de la fila.
     */
    private Session cursorToSession(Cursor cursor) {
        Session session = new Session();
        session.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
        session.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
        session.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START)));
        session.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DURATION)));
        session.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED)) == 1);
        return session;
    }

    /**
     * Elimina y recrea la tabla al actualizar la version de la base de datos.
     * @param db         Instancia de la base de datos.
     * @param oldVersion Version anterior.
     * @param newVersion Nueva version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        onCreate(db);
        Log.d("SessionManager", "DB actualizada de v" + oldVersion + " a v" + newVersion);
    }
}