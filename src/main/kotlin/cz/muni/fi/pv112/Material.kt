package cz.muni.fi.pv112

import org.joml.Vector3f
import org.joml.Vector4f

data class Material(
    val ambientColor: Vector4f,
    val diffuseColor: Vector4f,
    val specularColor: Vector4f,
    val shininess: Float
) {

    companion object {
        val discMaterials = arrayOf(
            Material(
                ambientColor = Vector4f(0.1f, 0.18725f, 0.1745f, 0.8f),
                diffuseColor = Vector4f(0.396f, 0.74151f, 0.69102f, 0.8f),
                specularColor = Vector4f(0.297254f, 0.30829f, 0.306678f, 0.8f),
                shininess = 12.8f
            ), Material(
                ambientColor = Vector4f(0.25f, 0.20725f, 0.20725f, 0.922f),
                diffuseColor = Vector4f(1.0f, 0.829f, 0.829f, 0.922f),
                specularColor = Vector4f(0.296648f, 0.296648f, 0.296648f, 0.922f),
                shininess = 11.264f
            ), Material(
                ambientColor = Vector3f(0f, 0f, 0f),
                diffuseColor = Vector3f(0.5f, 0f, 0f),
                specularColor = Vector3f(0.7f, 0.6f, 0.6f),
                shininess = 32f
            ), Material(
                ambientColor = Vector3f(0.05f, 0.05f, 0f),
                diffuseColor = Vector3f(0.5f, 0.5f, 0.4f),
                specularColor = Vector3f(0.7f, 0.7f, 0.04f),
                shininess = 10f
            ), Material(
                ambientColor = Vector3f(0f, 0f, 0f),
                diffuseColor = Vector3f(0.1f, 0.35f, 0.1f),
                specularColor = Vector3f(0.45f, 0.55f, 0.45f),
                shininess = 32f
            ), Material(
                ambientColor = Vector3f(0f, 0f, 0f),
                diffuseColor = Vector3f(1f, 0.01f, 0.01f),
                specularColor = Vector3f(0.5f, 0.5f, 0.5f),
                shininess = 32f
            ), Material(
                ambientColor = Vector3f(0.2125f, 0.1275f, 0.054f),
                diffuseColor = Vector3f(0.714f, 0.4284f, 0.18144f),
                specularColor = Vector3f(0.393548f, 0.271906f, 0.166721f),
                shininess = 25.6f
            ), Material(
                ambientColor = Vector3f(0.25f, 0.25f, 0.25f),
                diffuseColor = Vector3f(0.4f, 0.4f, 0.4f),
                specularColor = Vector3f(0.774597f, 0.774597f, 0.774597f),
                shininess = 76.8f
            )
        )
    }

    constructor(ambientColor: Vector3f, diffuseColor: Vector3f, specularColor: Vector3f, shininess: Float) : this(
        Vector4f(ambientColor, 1.0f),
        Vector4f(diffuseColor, 1.0f),
        Vector4f(specularColor, 1.0f),
        shininess
    )
}
