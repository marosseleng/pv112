package cz.muni.fi.pv112.logic

import cz.muni.fi.pv112.Step
import java.util.LinkedList
import java.util.Scanner
import kotlin.math.max

const val NUM_OF_RINGS = 8

data class Ring(val radius: Int) {
    override fun toString() = "(${(radius * 2).underscores()})"
}

typealias HanoiState = Triple<Stick, Stick, Stick>

typealias StepAndResult = Pair<Step, HanoiTowers>

class Stick {
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
        val emptyLines = NUM_OF_RINGS + 3 - ringsAttached
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
        val newStick = Stick()
        rings.reversed().forEach {
            newStick.push(it.copy())
        }
        return newStick
    }
}

class HanoiTowers {
    lateinit var sticks: Map<Position, Stick>
        private set

    var lastStep: Step? = null
        private set

    private var stepsCount = 1
    private lateinit var movesDone: MutableList<Step>
    private var existingStrategy = emptyList<Step>()
    private var indexOfLastGoodMove = -1

    init {
        initTowers()
        existingStrategy = solve()
        initTowers()
    }

    private fun initTowers() {
        val startingStick = Stick()
        repeat(NUM_OF_RINGS) {
            startingStick.push(Ring(NUM_OF_RINGS - it))
        }
        sticks = mapOf(Position.LEFT to startingStick, Position.CENTER to Stick(), Position.RIGHT to Stick())
        stepsCount = 1
        lastStep = null
        movesDone = mutableListOf()
    }

    /**
     * Solves this [HanoiTowers] in one call
     */
    fun solve(): List<Step> {
        val result = mutableListOf<Step>()
        while (stepsCount <= Math.pow(2.0, NUM_OF_RINGS.toDouble()) - 1) {
            step()
            result.add(lastStep ?: (Position.LEFT to Position.RIGHT))
        }
        return result
    }

    /**
     * Solves this [HanoiTowers] recursively in one call
     */
    fun solveRecursively(from: Position, inter: Position, to: Position) {
        solveRecursively(NUM_OF_RINGS, from, inter, to)
    }

    /**
     * This function takes existingStrategy[indexOfLastGoodMove] and performs it
     */
    fun correctStep(): Int {
        if (++indexOfLastGoodMove > existingStrategy.lastIndex) {
            indexOfLastGoodMove = -1
            return 0
        }
        val correctStep = existingStrategy[indexOfLastGoodMove]
        move(correctStep.first, correctStep.second)
        return indexOfLastGoodMove
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
        } else {
            return false
        }

        println()
        println("Moving ring from $from to $to:")
        println(this)
        val move = from to to
        lastStep = move
        movesDone.add(move)

        if (existingStrategy.indexOf(move) > indexOfLastGoodMove) {
            indexOfLastGoodMove = existingStrategy.indexOf(move)
        }

        return result
    }

    fun computeStepsForAutomaticSolve(): List<Step> {
        return computeReturnSteps() + existingStrategy.drop(indexOfLastGoodMove + 1)
    }

    fun reset(): HanoiTowers {
        return HanoiTowers()
    }

    /**
     * Computes the steps needed to get back on track with solving the problem (L-R strategy)
     */
    private fun computeReturnSteps(): List<Step> {
        return if (indexOfLastGoodMove < 0) {
            movesDone.reversed()
        } else {
            val lastKnownGoodStep = movesDone.lastIndexOf(existingStrategy[indexOfLastGoodMove])
            movesDone.drop(max(0, lastKnownGoodStep + 1)).reversed()
        }
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
    private fun step(): Int {
        when (stepsCount % 3) {
            0 -> exchange(Position.RIGHT, Position.CENTER)
            1 -> exchange(Position.LEFT, Position.CENTER)
            2 -> exchange(Position.LEFT, Position.RIGHT)
        }
        return stepsCount++
    }

    /**
     * Does a safe exchange be
     */
    private fun exchange(pos1: Position, pos2: Position) {
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
    val hanoi = HanoiTowers()
    println("*** Hanoi tower solution ***")
    println()
    println(hanoi)
    println()
    println()
    println("Press ENTER to see the next step!")

    val sc = Scanner(System.`in`)

    do {
        sc.nextLine()
        val stepsDone = hanoi.correctStep()
        println("Steps done: $stepsDone. Press ENTER to see the next step!")
    } while (stepsDone <= Math.pow(2.0, numOfDisks.toDouble()) - 1)
    println("*** DONE ***")
}