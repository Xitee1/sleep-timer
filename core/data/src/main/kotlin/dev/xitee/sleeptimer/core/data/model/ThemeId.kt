package dev.xitee.sleeptimer.core.data.model

enum class ThemeId {
    Midnight,
    Ocean,
    Ember,
    Light,
    Basic,
    Amoled,
    ;

    companion object {
        val Default: ThemeId = Midnight

        fun fromStorage(value: String?): ThemeId =
            values().firstOrNull { it.name == value } ?: Default
    }
}
