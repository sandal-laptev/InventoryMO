package ru.mokolomyagi.inventorymo.utilities

import kotlin.math.abs

object OrientationUtils {

    private var lastRotation = -1
    private const val HYSTERESIS_THRESHOLD = 30

    /**
     * Преобразует физический угол (0–359) в одну из фиксированных ориентаций (0, 90, 180, 270)
     * с применением гистерезиса, чтобы избежать "дёрганья".
     */
    fun getRotationWithHysteresis(degrees: Int): Int {
        val candidate = when (degrees) {
            in 45..134 -> 270
            in 135..224 -> 180
            in 225..314 -> 90
            else -> 0
        }

        if (lastRotation != -1 && candidate != lastRotation) {
            val diff = angleDifference(degrees, degreesForRotation(lastRotation))
            if (diff < HYSTERESIS_THRESHOLD) {
                return lastRotation // Не переключаем, слишком мало отклонение
            }
        }

        val normalizedDegrees = ((candidate % 360) + 360) % 360
        lastRotation = normalizedDegrees
        return normalizedDegrees
    }

    /**
     * Возвращает "идеальный" угол в градусах для заданной ориентации.
     */
    private fun degreesForRotation(rotation: Int): Int = when (rotation) {
        90 -> 270
        180 -> 180
        270 -> 90
        else -> 0
    }

    /**
     * Вычисляет минимальную разницу между двумя углами (в градусах).
     */
    private fun angleDifference(a: Int, b: Int): Int {
        val diff = abs(a - b) % 360
        return if (diff > 180) 360 - diff else diff
    }

}