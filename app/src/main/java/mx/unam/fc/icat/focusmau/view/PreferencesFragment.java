package mx.unam.fc.icat.focusmau.view;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import mx.unam.fc.icat.focusmau.R;

/**
 * Fragmento que carga las preferencias del usuario desde el XML de recursos.
 * Usa PreferenceFragmentCompat para integrar con el sistema de SharedPreferences.
 */
public class PreferencesFragment extends PreferenceFragmentCompat {

    /**
     * Carga el archivo XML de preferencias al crear el fragmento.
     * @param savedInstanceState Estado previo del fragmento, si existe.
     * @param rootKey            Clave raiz de la pantalla de preferencias.
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}