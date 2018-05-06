package cz.muni.fi.pv112

import org.joml.Vector3f
import org.joml.Vector4f

data class Material(
    val ambientColor: Vector4f,
    val diffuseColor: Vector4f,
    val specularColor: Vector4f,
    val shininess: Float
) {
    constructor(ambientColor: Vector3f, diffuseColor: Vector3f, specularColor: Vector3f, shininess: Float) : this(
        Vector4f(ambientColor, 1.0f),
        Vector4f(diffuseColor, 1.0f),
        Vector4f(specularColor, 1.0f),
        shininess
    )
}
