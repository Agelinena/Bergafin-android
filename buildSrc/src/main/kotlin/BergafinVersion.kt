/**
 * Versão do Bergafin: "<versão oficial>-berga.<N>", ex.: 2.7.3-berga.4.
 * versionCode = código oficial da versão base × 100 + N, então sempre cresce:
 * a cada build nosso (N) e a cada versão oficial nova.
 *
 *   2.7.3-berga.4 -> 2070399 * 100 + 4 = 207039904
 *
 * Sem o sufixo (build local), cai no cálculo oficial × 100.
 */
fun bergafinVersionCode(versionName: String): Int {
    val base = versionName.substringBefore("-berga.")
    val build = versionName.substringAfter("-berga.", "0").toIntOrNull() ?: 0
    require(build in 0..99) { "Build do Bergafin fora de 0..99: $versionName" }
    return getVersionCode(base) * 100 + build
}
