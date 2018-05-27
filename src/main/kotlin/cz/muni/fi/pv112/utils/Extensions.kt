package cz.muni.fi.pv112.utils

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