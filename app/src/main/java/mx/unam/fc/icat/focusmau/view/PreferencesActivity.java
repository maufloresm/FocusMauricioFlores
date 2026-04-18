package mx.unam.fc.icat.focusmau.view;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import java.util.Locale;
import mx.unam.fc.icat.focusmau.R;

/**
 * Actividad que gestiona las preferencias del usuario: tema y idioma.
 * Escucha cambios en SharedPreferences y los aplica en tiempo real.
 */
public class PreferencesActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences sharedPreferences;

    /**
     * Inicializa la actividad, configura la toolbar e inyecta el PreferencesFragment.
     * @param savedInstanceState Estado previo de la actividad, si existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = findViewById(R.id.preferences_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_preferences);
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preferences_content, new PreferencesFragment())
                .commit();
    }

    /**
     * Registra el listener de preferencias al volver la actividad al primer plano.
     */
    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Desregistra el listener al salir de la actividad para evitar fugas de memoria.
     */
    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Reacciona a cambios en cualquier preferencia del usuario.
     * Aplica el tema o idioma correspondiente segun la clave modificada.
     * @param sharedPreferences Instancia de SharedPreferences modificada.
     * @param key               Clave de la preferencia que cambio.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.theme_preference_key))) {
            String themeValue = sharedPreferences.getString(key, "light");
            applyTheme(themeValue);
        }
        if (key.equals(getString(R.string.lang_preference_key))) {
            String lang = sharedPreferences.getString(key, "en");
            applyLanguage(lang);
            recreate();
        }
    }

    /**
     * Aplica el modo oscuro o claro segun la preferencia del usuario.
     * @param themeValue Valores esperados: "light", "dark" o "system".
     */
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

    /**
     * Aplica el idioma seleccionado actualizando el Locale de la aplicacion.
     * @param langCode Codigo de idioma: "en" para ingles, "es" para espanol.
     */
    private void applyLanguage(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(
                config, getBaseContext().getResources().getDisplayMetrics());
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