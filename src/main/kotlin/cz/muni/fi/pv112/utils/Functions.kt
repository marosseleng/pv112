package cz.muni.fi.pv112.utils

import cz.muni.fi.pv112.Main
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import java.io.IOException

fun loadProgram(vertexShaderFile: String, fragmentShaderFile: String): Int {
    // load vertex and fragment shaders (GLSL)
    val vs = loadShader(vertexShaderFile, GL20.GL_VERTEX_SHADER)
    val fs = loadShader(fragmentShaderFile, GL20.GL_FRAGMENT_SHADER)

    // create GLSL program, attach shaders and compile it
    val program = GL20.glCreateProgram()
    GL20.glAttachShader(program, vs)
    GL20.glAttachShader(program, fs)
    GL20.glLinkProgram(program)

    val status = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS)
    if (status == GL11.GL_FALSE) {
        val log = GL20.glGetProgramInfoLog(program)
        System.err.println(log)
    }

    return program
}

fun loadShader(filename: String, shaderType: Int): Int {
    val source = readAllFromResource(filename)
    val shader = GL20.glCreateShader(shaderType)

    // create and compile GLSL shader
    GL20.glShaderSource(shader, source)
    GL20.glCompileShader(shader)

    // check GLSL shader compile status
    val status = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS)
    if (status == GL11.GL_FALSE) {
        val log = GL20.glGetShaderInfoLog(shader)
        System.err.println(log)
    }

    return shader
}

fun readAllFromResource(resource: String): String {
    return (Main::class.java.getResourceAsStream(resource) ?: throw IOException("Resource not found: $resource"))
        .bufferedReader()
        .lineSequence()
        .joinToString(separator = "\n")
}