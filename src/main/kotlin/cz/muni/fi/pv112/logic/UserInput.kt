package cz.muni.fi.pv112.logic

class UserInputSequence(first: Position = Position.LEFT, second: Position = Position.RIGHT, val onValuePushedCallback: () -> Unit) {
    private val array = Array(2) {
        if (it == 0) {
            first
        } else {
            second
        }
    }
    var currentUnsetPosition = 0
        private set

    fun push(value: Int) {
        array[currentUnsetPosition % 2] = when (value) {
            1 -> {
                Position.LEFT
            }
            2 -> {
                Position.CENTER
            }
            3 -> {
                Position.RIGHT
            }
            else -> {
                array[currentUnsetPosition % 2]
            }
        }
        currentUnsetPosition++
        onValuePushedCallback()
    }

    fun isValid() = array[0] != array[1]

    fun get() = array[0] to array[1]

    fun reset() {
        currentUnsetPosition = 0
        array[0] = Position.LEFT
        array[1] = Position.CENTER
        onValuePushedCallback()
    }

    operator fun component1(): Position {
        return array[0]
    }

    operator fun component2(): Position {
        return array[1]
    }

    override fun toString(): String {
        return "[${array[0]}-${array[1]}]: ${if (isValid()) "valid" else "invalid"}"
    }
}