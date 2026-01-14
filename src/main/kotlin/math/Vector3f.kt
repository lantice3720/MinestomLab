package math

data class Vector3f(val x: Float, val y: Float, val z: Float) {
    override fun toString(): String {
        return "Vector3f(x=$x, y=$y, z=$z)"
    }

    fun normalize(): Vector3f {
        val length = length()
        return Vector3f(x / length, y / length, z / length)
    }

    fun length(): Float {
        return kotlin.math.sqrt(x * x + y * y + z * z)
    }

    infix fun dot(other: Vector3f): Float {
        return x * other.x + y * other.y + z * other.z
    }

    infix fun cross(other: Vector3f): Vector3f {
        return Vector3f(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
    }

    operator fun plus(other: Vector3f): Vector3f {
        return Vector3f(x + other.x, y + other.y, z + other.z)
    }

    operator fun minus(other: Vector3f): Vector3f {
        return Vector3f(x - other.x, y - other.y, z - other.z)
    }

    operator fun times(scalar: Float): Vector3f {
        return Vector3f(x * scalar, y * scalar, z * scalar)
    }

    operator fun times(other: Vector3f): Float {
        return this.dot(other)
    }
}