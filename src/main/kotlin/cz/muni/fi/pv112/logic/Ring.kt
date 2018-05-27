package cz.muni.fi.pv112.logic

import cz.muni.fi.pv112.utils.Step
import cz.muni.fi.pv112.utils.StepAndResult
import cz.muni.fi.pv112.utils.Sticks
import cz.muni.fi.pv112.utils.backwards
import cz.muni.fi.pv112.utils.spaces
import cz.muni.fi.pv112.utils.toTriple
import cz.muni.fi.pv112.utils.underscores
import java.util.LinkedList
import java.util.Objects
import java.util.Scanner
import kotlin.math.max

const val NUM_OF_RINGS = 8

data class Ring(val radius: Int) {
    override fun equals(other: Any?) = other != null && other is Ring && other.radius == this.radius
    override fun hashCode() = this.radius
    override fun toString() = "(${(radius * 2).underscores()})"
}

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
        val width = 2 * maxRadius + 2
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

    private fun justStick(width: Int): String {
        val numOfSpaces = (width - 2) / 2
        return numOfSpaces.spaces() + "||" + numOfSpaces.spaces()
    }

    override fun equals(other: Any?) = other != null && other is Stick && other.rings.all { it in this.rings } && this.rings.all { it in other.rings }

    override fun hashCode() = Objects.hash(this.rings)
}

class HanoiTowers {
    lateinit var sticks: Sticks
        private set

    var lastStep: Step? = null
        private set

    private var stepsCount = 1
    private lateinit var movesDone: MutableList<Step>
    private var existingStrategy = emptyList<StepAndResult>()
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
     * This function takes existingStrategy[indexOfLastGoodMove] and performs it
     */
    fun correctStep(): Int {
        if (++indexOfLastGoodMove > existingStrategy.lastIndex) {
            indexOfLastGoodMove = -1
            return 0
        }
        val correctStep = existingStrategy[indexOfLastGoodMove].first
        move(correctStep.first, correctStep.second, true)
        return indexOfLastGoodMove
    }

    /**
     * Move a single [Ring] from the [Stick] at [Position] [from] to the [Stick] at [Position] [to]
     *
     * @return true, iff the move succeeded
     */
    fun move(from: Position, to: Position, verbose: Boolean = false): Boolean {
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

        if (verbose) {
            println()
            println("Moving ring from $from to $to:")
            println(this)
        }

        val move = from to to
        lastStep = move
        movesDone.add(move)

        val existingStrategyIndexOf = existingStrategy.indexOfFirst {
            it.second == sticks.toTriple()
        }

        if (existingStrategyIndexOf > indexOfLastGoodMove) {
            indexOfLastGoodMove = existingStrategyIndexOf
        }

        return result
    }

    /**
     * Computes the steps that the automatic mode will perform in order to successfully finish the game
     */
    fun computeStepsForAutomaticSolve(): List<Step> {
        return computeReturnSteps() + existingStrategy.drop(indexOfLastGoodMove + 1).map { it.first }
    }

    /**
     * Solves this [HanoiTowers] in one call
     */
    private fun solve(): List<StepAndResult> {
        val result = mutableListOf<StepAndResult>()
        while (stepsCount <= Math.pow(2.0, NUM_OF_RINGS.toDouble()) - 1) {
            step()
            result.add((lastStep?.copy() ?: (Position.LEFT to Position.RIGHT)) to sticks.toTriple())
        }
        return result
    }

    /**
     * Computes the steps needed to get back on track with solving the problem (L-R strategy)
     */
    private fun computeReturnSteps(): List<Step> {
        return if (indexOfLastGoodMove < 0) {
            movesDone
        } else {
            val lastKnownGoodStep = movesDone.lastIndexOf(existingStrategy[indexOfLastGoodMove].first)
            movesDone.drop(max(0, lastKnownGoodStep + 1))
        }.map { it.backwards() }
            .reversed()
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