package org.jellyfin.mobile.bergafin

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.mobile.webapp.WebappFunctionChannel
import org.koin.android.ext.android.get
import timber.log.Timber

/**
 * Login pelo Authentik no app.
 *
 * 1. No Bergafin-web, o botão "Entrar com Authentik" chama NativeShell.Bergafin.login
 *    (assets/native/nativeshell.js), que abre /Bergafin/Login?mode=app no navegador do sistema.
 * 2. O plugin Bergafin Auth volta para bergafin://login?ticket=… (intent-filter da MainActivity).
 * 3. Aqui o ticket vai para o web (#/login?bergafinTicket=…), que troca por uma sessão
 *    usando o id e o nome deste aparelho.
 */
object BergafinLogin {
    private const val SCHEME = "bergafin"
    private const val HOST = "login"
    private val TICKET = Regex("^[A-Za-z0-9_-]{20,200}$")

    /** Trata o intent se for a volta do Authentik. */
    fun handle(activity: ComponentActivity, intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != SCHEME || data.host != HOST) return
        val ticket = data.getQueryParameter("ticket")?.takeIf(TICKET::matches) ?: run {
            Timber.w("Bergafin: volta do Authentik sem ticket válido")
            return
        }
        // Não tratar o mesmo intent de novo (ex.: rotação de tela)
        intent.data = null

        val channel = activity.get<WebappFunctionChannel>()
        val js = "window.location.hash = '#/login?bergafinTicket=$ticket';"
        activity.lifecycleScope.launch {
            // O web pode ainda estar carregando (app aberto do zero pelo navegador)
            repeat(RETRIES) {
                if (channel.call(js)) return@launch
                delay(RETRY_DELAY_MS)
            }
            Timber.w("Bergafin: o web não ficou pronto para receber o ticket")
        }
    }

    private const val RETRIES = 120
    private const val RETRY_DELAY_MS = 500L
}
