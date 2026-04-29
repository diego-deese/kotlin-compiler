// ── Feature Flags ──────────────────────────────────────────────
object FeatureFlags {
    const val DEBUG = false
}

fun debugPrint(message: String) {
    if (FeatureFlags.DEBUG) {
        println(message)
    }
}

fun debugPrintln(message: String) {
    if (FeatureFlags.DEBUG) {
        println(message)
    }
}
