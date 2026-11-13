package org.jellyfin.mobile.bergafin.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Compara a release "latest" do GitHub com a versão instalada e avisa por notificação. */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val manual = inputData.getBoolean(KEY_MANUAL, false)

        val release = try {
            ReleaseSource.latest()
        } catch (e: Exception) {
            Timber.w(e, "Bergafin: não deu para procurar atualização")
            if (manual) UpdateNotifications.showCheckFailed(context)
            return@withContext Result.success()
        }

        if (release != null && release.versionCode > ReleaseSource.installedVersionCode(context)) {
            UpdateNotifications.showAvailable(context, release)
        } else if (manual) {
            UpdateNotifications.showUpToDate(context)
        }
        Result.success()
    }

    companion object {
        const val KEY_MANUAL = "manual"
    }
}
