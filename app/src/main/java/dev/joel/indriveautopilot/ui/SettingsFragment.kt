package dev.joel.indriveautopilot.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import dev.joel.indriveautopilot.R

class SettingsFragment : PreferenceFragmentCompat() {

    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        requireContext().contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit().putString("logFolderUri", uri.toString()).apply()
        findPreference<Preference>("pickLogFolder")?.summary = uri.toString()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Rquerido por NewXShaderPreferences (LSPosed)
        preferenceManager.sharedPreferencesMode = android.content.Context.MODE_WORLD_READABLE
        requireContext().getSharedPreferences("${requireContext().packageName}_preferences",
            android.content.Context.MODE_WORLD_READABLE)

        // Carpeta de logs (SAF)
        findPreference<Preference>("pickLogFolder")?.setOnPreferenceClickListener {
            pickFolder.launch(null)
            true
        }

        // Exportar hoy: simplemente abre el picker para seleccionar el CSV manualmente (depende del proveedor)
        findPreference<Preference>("exportToday")?.setOnPreferenceClickListener {
            // Abrir la carpeta elegida
            val uriStr = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("logFolderUri", null)
            if (uriStr != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(uriStr)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(intent)
            }
            true
        }

        // Reset totales
        findPreference<Preference>("resetTotals")?.setOnPreferenceClickListener {
            val sp = PreferenceManager.getDefaultSharedPreferences(requireContext())
            // No borra archivos, solo marcar acción (simple): podrías borrar totals.json manualmente
            // Por simplicidad, aquí no implementamos borrado físico; se puede añadir si lo deseas
            true
        }
    }
}
