package math

import kotlin.math.sqrt

data class Quaternionf(val x: Float, val y: Float, val z: Float, val w: Float) {
    override fun toString(): String {
        return "Quaternion(x=$x, y=$y, z=$z, w=$w)"
    }

    fun vector(): Vector3f = Vector3f(x, y, z)

    fun scalar(): Float = w

    fun normalize(): Quaternionf {
        val length = length()
        return Quaternionf(x / length, y / length, z / length, w / length)
    }

    fun length(): Float =
        sqrt((x * x + y * y + z * z + w * w))

    fun conjugate(): Quaternionf =
        Quaternionf(-x, -y, -z, w)

    fun inverse(): Quaternionf {
        val length = length()
        return Quaternionf(-x / length, -y / length, -z / length, w / length)
    }

    infix fun dot(other: Quaternionf): Float =
        x * other.x + y * other.y + z * other.z + w * other.w

    operator fun times(scalar: Float): Quaternionf =
        Quaternionf(x * scalar, y * scalar, z * scalar, w * scalar)

    operator fun times(other: Quaternionf): Quaternionf {
        val newX = w * other.x + x * other.w + y * other.z - z * other.y
        val newY = w * other.y - x * other.z + y * other.w + z * other.x
        val newZ = w * other.z + x * other.y - y * other.x + z * other.w
        val newW = w * other.w - x * other.x - y * other.y - z * other.z
        return Quaternionf(newX, newY, newZ, newW)
    }

    operator fun times(other: Vector3f): Vector3f {
        val t = (this.vector() cross other) * 2.0f
        return other + t * this.scalar() + this.vector() cross t
    }

    companion object {
        fun identity(): Quaternionf =
            Quaternionf(0.0f, 0.0f, 0.0f, 1.0f)

        fun fromAxisAngle(axis: Vector3f, angleRad: Float): Quaternionf {
            val halfAngle = angleRad / 2.0f
            val sinHalfAngle = kotlin.math.sin(halfAngle)
            val cosHalfAngle = kotlin.math.cos(halfAngle)
            val normalizedAxis = axis.normalize()
            return Quaternionf(
                normalizedAxis.x * sinHalfAngle,
                normalizedAxis.y * sinHalfAngle,
                normalizedAxis.z * sinHalfAngle,
                cosHalfAngle
            )
        }
    }
}