package org.jellyfin.mobile.bergafin.update

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Atualizações do Bergafin, pelas releases de https://github.com/Agelinena/Bergafin-android.
 * Confere ao abrir o app e a cada 12 horas; quando há versão nova, avisa por notificação,
 * e a tela de atualização baixa e instala o APK.
 */
object BergafinUpdater {
    private const val WORK_NOW = "bergafin-update-check"
    private const val WORK_PERIODIC = "bergafin-update-periodic"

    /** Chamado ao abrir o app. */
    fun schedule(context: Context) {
        val app = context.applicationContext
        // Build de debug tem outro ID; a release instalaria um segundo app
        if (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) return

        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val workManager = WorkManager.getInstance(app)
        workManager.enqueueUniqueWork(
            WORK_NOW,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<UpdateCheckWorker>().setConstraints(constraints).build(),
        )
        workManager.enqueueUniquePeriodicWork(
            WORK_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS).setConstraints(constraints).build(),
        )
    }

    /** Checagem pedida pelo usuário: avisa também quando não há nada novo. */
    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setInputData(workDataOf(UpdateCheckWorker.KEY_MANUAL to true))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.REPLACE, request)
    }
}
