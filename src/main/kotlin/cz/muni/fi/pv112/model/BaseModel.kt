package cz.muni.fi.pv112.model

import cz.muni.fi.pv112.Material
import cz.muni.fi.pv112.Obj
import org.joml.Matrix4f

abstract class BaseModel(
    val model: Matrix4f,
    val vao: Int,
    val offset: Int,
    val count: Int,
    val material: Material?,
    val texture: Int? = 0
)

class ObjModel(
    val obj: Obj,
    model: Matrix4f,
    vao: Int,
    material: Material?,
    texture: Int? = 0
) : BaseModel(
    model = model,
    vao = vao,
    offset = 0,
    count = obj.triangleCount * 3,
    material = material,
    texture = texture)