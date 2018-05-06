package cz.muni.fi.pv112.logic

import java.util.LinkedList
import java.util.Scanner

data class Ring(val radius: Int) {
    override fun toString() = "(${(radius * 2).underscores()})"
}

class Stick(private val numOfRings: Int = 8) {
    val rings = LinkedList<Ring>()

    /**
     * Returns the [Ring] that is on the top of the stick, but does not remove it
     */
    fun peek(): Ring? = rings.peek()

    /**
     * Inserts a [Ring] into this [Stick]
     *
     * @return true iff ring was inserted, else otherwise
     */
    fun push(ring: Ring): Boolean {
        val top = peek()
        return if (top == null || ring.radius < top.radius) {
            rings.push(ring)
            true
        } else {
            false
        }
    }

    /**
     * Removes the top [Ring] from this [Stick] and returns it.
     *
     * @return top [Ring] or null if this [Stick] is Empty
     */
    fun pop(): Ring? = rings.pop()

    /**
     * Returns true iff this [Stick] is does not contain any [Ring]s
     */
    fun isEmpty() = rings.isEmpty()

    /**
     * The number of [Ring]s on this [Stick]
     */
    val size = rings.size

    fun toStringLines(): List<String> {
        val maxRadius = rings.map { it.radius }.max() ?: 0
        val width = 2 * maxRadius + 2 // diameter + ()
        val ringsAttached = rings.size
        val emptyLines = numOfRings + 3 - ringsAttached
        val result = mutableListOf<String>()
        repeat(emptyLines) {
            result.add(justStick(width))
        }
        repeat(rings.size) {
            val ring = rings[it]
            val stringRing = ring.toString()
            val remainingSpaces = width - stringRing.length
            result.add((remainingSpaces / 2).spaces() + stringRing + (remainingSpaces / 2).spaces())
        }
        return result
    }

    fun deepCopy(): Stick {
        val newStick = Stick(numOfRings)
        rings.reversed().forEach {
            newStick.push(it.copy())
        }
        return newStick
    }
}

data class HanoiTowers(val numOfDisks: Int = 8) {
    var strategy: Strategy = Strategy.LEFT_RIGHT
        private set
    val sticks: Map<Position, Stick>
    private var stepsDone = 1
    var lastStep: Pair<Position, Position>? = null
        private set

    init {
        val startingStick = Stick(numOfDisks)
        repeat(numOfDisks) {
            startingStick.push(Ring(numOfDisks - it))
        }
        val leftStick = when (strategy) {
            Strategy.LEFT_RIGHT -> startingStick
            Strategy.RIGHT_LEFT -> Stick(numOfDisks)
        }
        val rightStick = when (strategy) {
            Strategy.LEFT_RIGHT -> Stick(numOfDisks)
            Strategy.RIGHT_LEFT -> startingStick
        }

        sticks = mapOf(Position.LEFT to leftStick, Position.CENTER to Stick(), Position.RIGHT to rightStick)
    }

    /**
     * Solves this [HanoiTowers] in one call
     */
    fun solve() {
        while (stepsDone <= Math.pow(2.0, numOfDisks.toDouble()) - 1) {
            step()
        }
        reverseStrategy()
    }

    /**
     * Solves this [HanoiTowers] recursively in one call
     */
    fun solveRecursively(from: Position, inter: Position, to: Position) {
        solveRecursively(numOfDisks, from, inter, to)
        reverseStrategy()
    }

    private fun solveRecursively(topN: Int, from: Position, inter: Position, to: Position) {
        if (topN == 1) {
            move(from, to)
        } else {
            solveRecursively(topN - 1, from, to, inter)
            move(from, to)
            solveRecursively(topN - 1, inter, from, to)
        }
    }

    /**
     * Performs the 1 step towards the solution
     */
    fun step(): Int {
        when (stepsDone % 3) {
            0 -> exchange(Position.RIGHT, Position.CENTER)
            1 -> exchange(Position.LEFT, Position.CENTER)
            2 -> exchange(Position.LEFT, Position.RIGHT)
        }
        return stepsDone++
    }

    /**
     * Checks whether this game is solved
     *
     * @return true iff there is one [Stick] containing all [Ring]s in the correct order
     */
    fun isSolved() = (sticks[strategy.to]?.size ?: numOfDisks) == numOfDisks

    /**
     * Does a safe exchange be
     */
    fun exchange(pos1: Position, pos2: Position) {
        val ring1 = sticks[pos1]?.peek()
        val ring2 = sticks[pos2]?.peek()
        val rad1 = ring1?.radius ?: Int.MAX_VALUE
        val rad2 = ring2?.radius ?: Int.MAX_VALUE

        if (rad1 < rad2) {
            move(pos1, pos2)
        } else {
            move(pos2, pos1)
        }
    }

    /**
     * Move a single [Ring] from the [Stick] at [Position] [from] to the [Stick] at [Position] [to]
     *
     * @return true, iff the move succeeded
     */
    fun move(from: Position, to: Position): Boolean {
        if (sticks[from]?.isEmpty() == true) {
            return false
        }
        val fromRing = sticks[from]?.peek() ?: return false
        val result = sticks[to]?.push(fromRing) ?: return false
        if (result) {
            sticks[from]?.pop()
        }

        println()
        println("Moving ring from $from to $to:")
        println(this)
        lastStep = Pair(from, to)

        return result
    }

    /**
     * Reverses the current [Strategy]
     */
    fun reverseStrategy() {
        strategy = !strategy
    }

    override fun toString(): String {
        val lines = mutableListOf<String>()
        val leftStick = sticks[Position.LEFT]?.toStringLines() ?: emptyList()
        val centerStick = sticks[Position.CENTER]?.toStringLines() ?: emptyList()
        val rightStick = sticks[Position.RIGHT]?.toStringLines() ?: emptyList()

        repeat(minOf(leftStick.size, centerStick.size, rightStick.size)) {
            lines.add(leftStick[it] + 5.spaces() + centerStick[it] + 5.spaces() + rightStick[it])
        }
        return lines.joinToString(separator = "\n")
    }
}

enum class Position {
    LEFT, CENTER, RIGHT
}

enum class Strategy(val from: Position, val to: Position) {
    LEFT_RIGHT(Position.LEFT, Position.RIGHT),
    RIGHT_LEFT(Position.RIGHT, Position.LEFT);

    operator fun not(): Strategy {
        return when (this) {
            LEFT_RIGHT -> RIGHT_LEFT
            RIGHT_LEFT -> LEFT_RIGHT
        }
    }
}

fun Int.underscores(): String {
    val sb = StringBuilder()
    repeat(this) {
        sb.append("_")
    }
    return sb.toString()
}

fun Int.spaces(): String {
    val sb = StringBuilder()
    repeat(this) {
        sb.append(" ")
    }
    return sb.toString()
}

fun justStick(width: Int): String {
    val numOfSpaces = (width - 2) / 2
    return numOfSpaces.spaces() + "||" + numOfSpaces.spaces()
}

fun main(args: Array<String>) {
    val numOfDisks = 8
    val hanoi = HanoiTowers(numOfDisks)
    println("*** Hanoi tower solution ***")
    println()
    println(hanoi)
    println()
    println()
    println("Press ENTER to see the next step!")

    val sc = Scanner(System.`in`)

    do {
        sc.nextLine()
        val stepsDone = hanoi.step()
        println("Steps done: $stepsDone. Press ENTER to see the next step!")
    } while (stepsDone <= Math.pow(2.0, numOfDisks.toDouble()) - 1)
    println("*** DONE ***")
}