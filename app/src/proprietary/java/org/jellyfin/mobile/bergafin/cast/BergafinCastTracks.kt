package org.jellyfin.mobile.bergafin.cast

import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaTrack
import org.json.JSONArray

/**
 * Faixas de legenda (WebVTT) junto com a mídia mandada ao Chromecast.
 *
 * A ponte chrome.cast do app (ChromecastUtilities) ignorava o campo "tracks" do MediaInfo.
 * O cast do Bergafin-web usa o Default Media Receiver, que só mostra legenda de texto
 * se ela vier como faixa da mídia.
 */
object BergafinCastTracks {
    @JvmStatic
    fun withTracks(info: MediaInfo, tracksJson: JSONArray?): MediaInfo {
        val tracks = parse(tracksJson)
        if (tracks.isEmpty()) return info
        return MediaInfo.Builder(info.contentId.orEmpty())
            .setContentType(info.contentType.orEmpty())
            .setStreamType(info.streamType)
            .setStreamDuration(info.streamDuration)
            .setCustomData(info.customData)
            .setMetadata(info.metadata)
            .setTextTrackStyle(info.textTrackStyle)
            .setMediaTracks(tracks)
            .build()
    }

    private fun parse(json: JSONArray?): List<MediaTrack> {
        if (json == null) return emptyList()
        return (0 until json.length()).mapNotNull { i ->
            val track = json.optJSONObject(i) ?: return@mapNotNull null
            val type = when (track.optString("type")) {
                "TEXT" -> MediaTrack.TYPE_TEXT
                "AUDIO" -> MediaTrack.TYPE_AUDIO
                "VIDEO" -> MediaTrack.TYPE_VIDEO
                else -> return@mapNotNull null
            }
            MediaTrack.Builder(track.optLong("trackId"), type).apply {
                if (type == MediaTrack.TYPE_TEXT) {
                    setSubtype(
                        when (track.optString("subtype")) {
                            "CAPTIONS" -> MediaTrack.SUBTYPE_CAPTIONS
                            else -> MediaTrack.SUBTYPE_SUBTITLES
                        },
                    )
                }
                track.optString("trackContentId").takeIf { it.isNotEmpty() }?.let(::setContentId)
                track.optString("trackContentType").takeIf { it.isNotEmpty() }?.let(::setContentType)
                track.optString("name").takeIf { it.isNotEmpty() && it != "null" }?.let(::setName)
                track.optString("language").takeIf { it.isNotEmpty() && it != "null" }?.let(::setLanguage)
            }.build()
        }
    }
}
