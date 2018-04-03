package cz.muni.fi.pv112

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList

class ObjLoader(private val path: String) {

    var vertices: MutableList<FloatArray>? = null
    var normals: MutableList<FloatArray>? = null
    var texcoords: MutableList<FloatArray>? = null
    var vertexIndices: MutableList<IntArray>? = null
    var normalIndices: MutableList<IntArray>? = null
    var texcoordIndices: MutableList<IntArray>? = null

    val triangleCount: Int
        get() = vertexIndices!!.size

    @Throws(IOException::class)
    fun load() {
        /* Mesh containing the loaded object */
        vertices = ArrayList()
        normals = ArrayList()
        texcoords = ArrayList()
        vertexIndices = ArrayList()
        normalIndices = ArrayList()
        texcoordIndices = ArrayList()

        val `is` = ObjLoader::class.java.getResourceAsStream(path) ?: throw IOException("File not found $path")

        BufferedReader(InputStreamReader(`is`)).use { `in` ->

            var line = `in`.readLine()
            while (line != null) {
                if (line.startsWith("v ")) {

                    val vertStr = line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val vertex = FloatArray(3)

                    vertex[0] = java.lang.Float.parseFloat(vertStr[1])
                    vertex[1] = java.lang.Float.parseFloat(vertStr[2])
                    vertex[2] = java.lang.Float.parseFloat(vertStr[3])
                    vertices!!.add(vertex)

                } else if (line.startsWith("vn ")) {

                    val normStr = line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val normal = FloatArray(3)

                    normal[0] = java.lang.Float.parseFloat(normStr[1])
                    normal[1] = java.lang.Float.parseFloat(normStr[2])
                    normal[2] = java.lang.Float.parseFloat(normStr[3])
                    normals!!.add(normal)

                } else if (line.startsWith("vt ")) {

                    val texcoordStr = line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val texcoord = FloatArray(2)

                    texcoord[0] = java.lang.Float.parseFloat(texcoordStr[1])
                    texcoord[1] = java.lang.Float.parseFloat(texcoordStr[2])
                    texcoords!!.add(texcoord)

                } else if (line.startsWith("f ")) {

                    val faceStr = line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val faceVert = IntArray(3)

                    faceVert[0] = Integer.parseInt(faceStr[1].split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]) -
                            1
                    faceVert[1] = Integer.parseInt(faceStr[2].split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]) -
                            1
                    faceVert[2] = Integer.parseInt(faceStr[3].split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]) -
                            1
                    vertexIndices!!.add(faceVert)

                    val faceTexcoord = IntArray(3)
                    faceTexcoord[0] = Integer.parseInt(faceStr[1].split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]) -
                            1
                    faceTexcoord[1] = Integer.parseInt(faceStr[2].split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]) -
                            1
                    faceTexcoord[2] = Integer.parseInt(faceStr[3].split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]) -
                            1
                    texcoordIndices!!.add(faceTexcoord)

                    if (faceStr[1].split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size >= 3) {
                        val faceNorm = IntArray(3)

                        faceNorm[0] = Integer.parseInt(faceStr[1].split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2]) -
                                1
                        faceNorm[1] = Integer.parseInt(faceStr[2].split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2]) -
                                1
                        faceNorm[2] = Integer.parseInt(faceStr[3].split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2]) -
                                1
                        normalIndices!!.add(faceNorm)
                    }
                }
                line = `in`.readLine()
            }
        }
    }
}
