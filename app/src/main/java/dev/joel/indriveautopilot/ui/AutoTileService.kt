package dev.joel.indriveautopilot.ui

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.preference.PreferenceManager

class AutoTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.state = if (isAutoOn()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    override fun onClick() {
        super.onClick()
        toggleAuto()
        qsTile?.state = if (isAutoOn()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    private fun isAutoOn(): Boolean {
        val p = PreferenceManager.getDefaultSharedPreferences(this)
        return p.getBoolean("autoOpenFeedItems", false)
    }

    private fun toggleAuto() {
        val p = PreferenceManager.getDefaultSharedPreferences(this)
        val cur = p.getBoolean("autoOpenFeedItems", false)
        p.edit().putBoolean("autoOpenFeedItems", !cur).apply()
    }
}