package cz.muni.fi.pv112.cache

import org.lwjgl.opengl.GL20.*

class UACache(private val program: Int) {
    enum class UAMode {
        UNIFORM, ATTRIB
    }

    private val uniforms = mutableMapOf<UAName, Int>()
    private val attribs = mutableMapOf<UAName, Int>()

    operator fun get(name: UAName): Int {
        return when (name.type) {
            UAMode.UNIFORM -> uniforms[name]
            UAMode.ATTRIB -> attribs[name]
        } ?: 0
    }

    fun cache(vararg names: UAName) {
        names.forEach {
            when (it.type) {
                UAMode.ATTRIB -> {
                    attribs[it] = glGetAttribLocation(program, it.varName)
                }
                UAMode.UNIFORM -> {
                    uniforms[it] = glGetUniformLocation(program, it.varName)
                }
            }
        }
    }
}