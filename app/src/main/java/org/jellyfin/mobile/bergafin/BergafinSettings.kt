package org.jellyfin.mobile.bergafin

import androidx.fragment.app.Fragment
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.categoryHeader
import de.Maxr1998.modernpreferences.helpers.onClick
import de.Maxr1998.modernpreferences.helpers.pref
import org.jellyfin.mobile.BuildConfig
import org.jellyfin.mobile.R
import org.jellyfin.mobile.bergafin.update.BergafinUpdater

/** Seção "Bergafin" nas configurações do app (SettingsFragment). */
fun PreferenceScreen.Builder.bergafinSettings(fragment: Fragment) {
    categoryHeader("pref_category_bergafin") {
        titleRes = R.string.bergafin_app_name
    }
    pref("pref_bergafin_check_update") {
        titleRes = R.string.bergafin_update_check
        summary = fragment.getString(R.string.bergafin_update_installed, BuildConfig.VERSION_NAME)
        onClick {
            BergafinUpdater.checkNow(fragment.requireActivity())
            false
        }
    }
}
