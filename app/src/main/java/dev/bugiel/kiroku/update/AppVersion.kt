package dev.bugiel.kiroku.update

data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int =
        compareValuesBy(this, other, AppVersion::major, AppVersion::minor, AppVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        fun parse(value: String): AppVersion? {
            val clean = value.trim().removePrefix("v").substringBefore('-').substringBefore('+')
            val parts = clean.split('.')
            if (parts.size != 3) return null
            return AppVersion(
                major = parts[0].toIntOrNull() ?: return null,
                minor = parts[1].toIntOrNull() ?: return null,
                patch = parts[2].toIntOrNull() ?: return null,
            )
        }
    }
}
