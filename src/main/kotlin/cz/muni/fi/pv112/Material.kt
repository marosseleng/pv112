package cz.muni.fi.pv112

import org.joml.Vector3f

data class Material(
    val ambientColor: Vector3f,
    val diffuseColor: Vector3f,
    val specularColor: Vector3f,
    val shininess: Float
)
