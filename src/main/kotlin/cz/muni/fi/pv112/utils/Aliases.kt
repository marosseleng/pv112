package cz.muni.fi.pv112.utils

import cz.muni.fi.pv112.logic.Position
import cz.muni.fi.pv112.logic.Stick

typealias Step = Pair<Position, Position>

fun Step.isLeftToRight(): Boolean {
    return first == Position.LEFT || (first == Position.CENTER && second == Position.RIGHT)
}

fun Step.isSmallStep(): Boolean {
    return Math.abs(first.ordinal - second.ordinal) == 1
}

fun Step.backwards(): Step {
    return second to first
}

typealias HanoiState = Triple<Stick, Stick, Stick>

typealias StepAndResult = Pair<Step, HanoiState>

typealias Sticks = Map<Position, Stick>

fun Sticks.toTriple(): Triple<Stick, Stick, Stick> {
    return Triple(get(Position.LEFT)?.deepCopy() ?: Stick(), get(Position.CENTER)?.deepCopy() ?: Stick(), get(Position.RIGHT)?.deepCopy() ?: Stick())
}

