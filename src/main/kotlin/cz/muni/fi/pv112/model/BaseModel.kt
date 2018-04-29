package cz.muni.fi.pv112.model

import cz.muni.fi.pv112.Material
import org.joml.Matrix4f

data class BaseModel(val model: Matrix4f, val vao: Int, val offset: Int, val count: Int, val material: Material?, val texture: Int? = 0)