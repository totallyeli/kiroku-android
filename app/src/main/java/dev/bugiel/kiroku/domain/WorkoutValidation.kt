package dev.bugiel.kiroku.domain

import dev.bugiel.kiroku.domain.model.WorkoutSet

object WorkoutValidation {
    fun parseSet(weight: String, reps: String): WorkoutSet? {
        // Accept the decimal separator used by either supported locale, but no exponent/grouping syntax.
        val normalized = weight.trim().replace(',', '.')
        if (!normalized.matches(Regex("[0-9]+(?:\\.[0-9]{1,3})?"))) return null
        val kilograms = normalized.toDoubleOrNull() ?: return null
        val count = reps.trim().toIntOrNull() ?: return null
        return WorkoutSet(kilograms, count).takeIf(::isValid)
    }

    fun isValid(set: WorkoutSet): Boolean =
        set.weightKg.isFinite() && set.weightKg in 0.0..10_000.0 && set.reps in 1..10_000
}
