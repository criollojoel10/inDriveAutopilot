package dev.joel.indriveautopilot.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dev.joel.indriveautopilot.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs_autopilot, rootKey)
    }
}