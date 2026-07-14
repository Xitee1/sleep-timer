package dev.xitee.sleeptimer.core.data.model

enum class MediaEndAction {
    Pause,
    Stop,
    ;

    companion object {
        val Default: MediaEndAction = Pause

        fun fromStorage(value: String?): MediaEndAction =
            entries.firstOrNull { it.name == value } ?: Default
    }
}
