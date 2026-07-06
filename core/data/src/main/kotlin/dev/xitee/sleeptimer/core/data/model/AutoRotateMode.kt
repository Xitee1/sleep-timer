package dev.xitee.sleeptimer.core.data.model

enum class AutoRotateMode {
    System,
    Always,
    Portrait,
    ;

    companion object {
        val Default: AutoRotateMode = System

        fun fromStorage(value: String?): AutoRotateMode =
            entries.firstOrNull { it.name == value } ?: Default
    }
}
