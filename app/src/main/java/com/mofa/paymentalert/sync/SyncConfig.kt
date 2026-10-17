package com.mofa.paymentalert.sync

import android.content.Context

enum class DeviceRole { NONE, RECEIVER, OWNER }

class SyncConfig(context: Context) {

    private val prefs = context.getSharedPreferences("sync_config", Context.MODE_PRIVATE)

    var role: DeviceRole
        get() = runCatching {
            DeviceRole.valueOf(prefs.getString(KEY_ROLE, null) ?: DeviceRole.NONE.name)
        }.getOrDefault(DeviceRole.NONE)
        set(value) {
            prefs.edit().putString(KEY_ROLE, value.name).apply()
        }

    var shopCode: String?
        get() = prefs.getString(KEY_SHOP_CODE, null)
        set(value) {
            prefs.edit().putString(KEY_SHOP_CODE, value).apply()
        }

    var alertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALERTS_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ALERTS_ENABLED, value).apply()
        }

    var historyBackfillDone: Boolean
        get() = prefs.getBoolean(KEY_HISTORY_BACKFILL, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HISTORY_BACKFILL, value).apply()
        }

    companion object {
        private const val KEY_ROLE = "role"
        private const val KEY_SHOP_CODE = "shop_code"
        private const val KEY_ALERTS_ENABLED = "alerts_enabled"
        private const val KEY_HISTORY_BACKFILL = "history_backfill_done"
    }
}
