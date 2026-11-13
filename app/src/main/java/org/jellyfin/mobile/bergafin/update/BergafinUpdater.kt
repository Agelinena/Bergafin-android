package org.jellyfin.mobile.bergafin.update

import android.content.Context
import android.content.pm.ApplicationInfo
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.mobile.R
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Atualizações do Bergafin, pelas releases de https://github.com/Agelinena/Bergafin-android.
 *
 * - Ao abrir o app: confere e, se houver versão nova, mostra uma janela "Atualizar / Depois"
 *   (não depende da permissão de notificação).
 * - Com o app fechado: um trabalho a cada 12 horas avisa por notificação.
 * - Configurações do app → Bergafin → "Procurar atualização": confere na hora.
 */
object BergafinUpdater {
    private const val WORK_PERIODIC = "bergafin-update-periodic"
    private const val PREFS = "bergafin_update"
    private const val KEY_DISMISSED_CODE = "dismissed_code"
    private const val KEY_DISMISSED_AT = "dismissed_at"
    private const val SNOOZE_MS = 24L * 60 * 60 * 1000

    /** Chamado ao abrir o app (MainActivity). */
    fun schedule(activity: ComponentActivity) {
        val app = activity.applicationContext
        // Build de debug tem outro ID; a release instalaria um segundo app
        if (isDebuggable(app)) return

        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            WORK_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS).setConstraints(constraints).build(),
        )
        check(activity, manual = false)
    }

    /** Botão "Procurar atualização": sempre mostra o resultado. */
    fun checkNow(activity: ComponentActivity) = check(activity, manual = true)

    private fun check(activity: ComponentActivity, manual: Boolean) {
        if (manual) {
            Toast.makeText(activity, R.string.bergafin_update_checking, Toast.LENGTH_SHORT).show()
        }
        activity.lifecycleScope.launch {
            val release = try {
                withContext(Dispatchers.IO) { ReleaseSource.latest() }
            } catch (e: Exception) {
                Timber.w(e, "Bergafin: não deu para procurar atualização")
                if (manual) Toast.makeText(activity, R.string.bergafin_update_check_failed, Toast.LENGTH_LONG).show()
                return@launch
            }

            val installed = ReleaseSource.installedVersionCode(activity)
            if (release == null || release.versionCode <= installed) {
                if (manual) Toast.makeText(activity, R.string.bergafin_update_up_to_date, Toast.LENGTH_LONG).show()
                return@launch
            }
            if (!manual && isSnoozed(activity, release)) return@launch
            showDialog(activity, release)
        }
    }

    private fun showDialog(activity: ComponentActivity, release: Release) {
        if (activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.bergafin_update_available_title, release.versionName))
            .setMessage(R.string.bergafin_update_dialog_text)
            .setPositiveButton(R.string.bergafin_update_action) { _, _ ->
                activity.startActivity(UpdateActivity.intent(activity, release))
            }
            .setNegativeButton(R.string.bergafin_update_later) { _, _ ->
                prefs(activity).edit {
                    putLong(KEY_DISMISSED_CODE, release.versionCode)
                    putLong(KEY_DISMISSED_AT, System.currentTimeMillis())
                }
            }
            .show()
    }

    /** "Depois" adia o aviso daquela versão por 24 horas. */
    private fun isSnoozed(context: Context, release: Release): Boolean {
        val prefs = prefs(context)
        return prefs.getLong(KEY_DISMISSED_CODE, 0) == release.versionCode &&
            System.currentTimeMillis() - prefs.getLong(KEY_DISMISSED_AT, 0) < SNOOZE_MS
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun isDebuggable(context: Context) =
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
