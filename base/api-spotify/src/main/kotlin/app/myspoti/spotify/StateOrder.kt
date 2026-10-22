package app.myspoti.spotify

/** Source timestamp ordering, not arrival time; scoped to one page session. */
internal class StateOrder {
    private var latest = Long.MIN_VALUE

    fun accept(timestampMs: Long?): Boolean {
        // Null means session teardown, not a timestamped track sample. In-flight polls
        // are additionally guarded by SpotifyPlayer's event generation.
        if (timestampMs == null) return true
        if (timestampMs < latest) return false
        latest = timestampMs
        return true
    }

    fun reset() { latest = Long.MIN_VALUE }
}
