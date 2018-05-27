package cz.muni.fi.pv112.cache

import org.lwjgl.opengl.GL20.glGetAttribLocation
import org.lwjgl.opengl.GL20.glGetUniformLocation

class UACache(private val program: Int) {
    enum class UAMode {
        UNIFORM, ATTRIB
    }

    private val uniforms = mutableMapOf<ModelProgram, Int>()
    private val attribs = mutableMapOf<ModelProgram, Int>()

    operator fun get(name: ModelProgram): Int {
        return when (name.mode) {
            UAMode.UNIFORM -> uniforms[name]
            UAMode.ATTRIB -> attribs[name]
        } ?: 0
    }

    fun cache(vararg names: ModelProgram) {
        names.forEach {
            when (it.mode) {
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