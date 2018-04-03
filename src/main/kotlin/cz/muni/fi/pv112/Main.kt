package cz.muni.fi.pv112

import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.*
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO

class Cv4 {

    private var camera: Camera? = null

    // the window handle
    private var window: Long = 0

    // window size
    private var width: Int = 0
    private var height: Int = 0
    private var resized = false

    // animation
    private var animate = false
    private var t = 0f

    // rendering mode
    private var mode = GL_FILL

    // model
    private var cube: ObjLoader? = null

    // our OpenGL resources
    private var axesBuffer: Int = 0
    private var cubeBuffer: Int = 0
    private var teapotBuffer: Int = 0
    private var axesArray: Int = 0
    private var cubeArray: Int = 0
    private val teapotArray: Int = 0

    private var woodTexture: Int = 0
    private val rocksTexture: Int = 0
    private val diceTextures = IntArray(6)

    // our GLSL resources
    private var axesProgram: Int = 0
    private var axesAspectUniformLoc: Int = 0
    private var axesLengthUniformLoc: Int = 0
    private var axesMvpUniformLoc: Int = 0

    private var modelProgram: Int = 0
    private var modelMvpLoc: Int = 0
    private var modelNLoc: Int = 0
    private var modelModelLoc: Int = 0

    private var lightPositionLoc: Int = 0
    private var lightAmbientColorLoc: Int = 0
    private var lightDiffuseColorLoc: Int = 0
    private var lightSpecularColorLoc: Int = 0

    private var materialAmbientColorLoc: Int = 0
    private var materialDiffuseColorLoc: Int = 0
    private var materialSpecularColorLoc: Int = 0
    private var materialShininessLoc: Int = 0

    private var eyePositionLoc: Int = 0

    private var woodTexLoc: Int = 0
    private val rocksTexLoc: Int = 0
    private val diceTexLoc: Int = 0

    fun run() {
        println("Hello LWJGL " + Version.getVersion() + "!")

        camera = Camera()

        initGLFW()
        loop()

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        // Terminate GLFW and free the error callback
        glfwTerminate()
        glfwSetErrorCallback(null)!!.free()
    }

    private fun initGLFW() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3) // request the OpenGL 3.3 core profile context
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE)

        // set initial width and height
        width = 640
        height = 480

        // Create the window
        window = glfwCreateWindow(width, height, "Hello World!", NULL, NULL)
        if (window == NULL) {
            throw RuntimeException("Failed to create the GLFW window")
        }

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window) { window, key, scancode, action, mods ->
            if (action == GLFW_RELEASE) {
                when (key) {
                    GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(
                        window,
                        true
                    ) // We will detect this in the rendering loop
                    GLFW_KEY_A -> animate = !animate
                    GLFW_KEY_T -> {
                    }
                    GLFW_KEY_L -> mode = GL_LINE
                    GLFW_KEY_F -> mode = GL_FILL
                }// TODO toggle fullscreen
            }
        }

        glfwSetMouseButtonCallback(window) { window, button, action, mods ->
            if (action == GLFW_PRESS) {
                if (button == GLFW_MOUSE_BUTTON_1) {
                    camera!!.updateMouseButton(Camera.Button.LEFT, true)
                } else if (button == GLFW_MOUSE_BUTTON_2) {
                    camera!!.updateMouseButton(Camera.Button.RIGHT, true)
                }
            } else if (action == GLFW_RELEASE) {
                if (button == GLFW_MOUSE_BUTTON_1) {
                    camera!!.updateMouseButton(Camera.Button.LEFT, false)
                } else if (button == GLFW_MOUSE_BUTTON_2) {
                    camera!!.updateMouseButton(Camera.Button.RIGHT, false)
                }
            }
        }

        glfwSetCursorPosCallback(window) { window, xpos, ypos -> camera!!.updateMousePosition(xpos, ypos) }

        // add window size callback
        glfwSetWindowSizeCallback(window) { window, width, height ->
            this.width = width
            this.height = height
            resized = true
        }

        // Get the thread stack and push a new frame
        stackPush().use({ stack ->
                            val pWidth = stack.mallocInt(1) // int*
                            val pHeight = stack.mallocInt(1) // int*

                            // Get the window size passed to glfwCreateWindow
                            glfwGetWindowSize(window, pWidth, pHeight)

                            // Get the resolution of the primary monitor
                            val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())

                            // Center the window
                            glfwSetWindowPos(
                                window,
                                (vidmode!!.width() - pWidth.get(0)) / 2,
                                (vidmode!!.height() - pHeight.get(0)) / 2
                            )
                        }) // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window)
        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window)

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()
    }

    private fun loop() {
        // Prepare data for rendering
        init()

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            render()

            glfwSwapBuffers(window) // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()
        }
    }

    private fun init() {
        // empty scene color
        // Task 10: set clear color to a brighter color (e.g., 0.15, 0.15, 0.15) so that we can better see the cube
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        glLineWidth(3.0f) // makes lines thicker

        // Task 9:  disable depth test in order to draw also occluded geometry
        glEnable(GL_DEPTH_TEST)


        // Task 9:  enable blending using glEnable(GL_BLEND)
        //          set blending function by glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        //              it will work like this: resultFrag = a * newFrag + (1-a) * oldFrag


        // Task 10: enable face culling in order to draw back faces first and front faces afterwards
        //             use glEnable(GL_CULL_FACE)


        // load GLSL program (vertex, fragment shaders) and textures
        try {
            axesProgram = loadProgram(
                "/shaders/axes.vs.glsl",
                "/shaders/axes.fs.glsl"
            )
            modelProgram = loadProgram(
                "/shaders/model.vs.glsl",
                "/shaders/model.fs.glsl"
            )

            // Task 1:  load wood.jpg texture using loadTexture("/resources/textures/wood.jpg"), store it to woodTexture variable
            // Task 7:  load all dice1-6.png textures from "/resources/textures/dice1-6.png", store them to diceTextures array
            // Task 8:  load rocks.jpg texture from "/resources/textures/rocks.jpg", store it to rocksTexture variable
            woodTexture = loadTexture("/textures/wood.jpg")


        } catch (ex: IOException) {
            Logger.getLogger(Cv4::class.java.name).log(Level.SEVERE, null, ex)
            System.exit(1)
        }

        // get uniform locations
        // axes program uniforms
        axesAspectUniformLoc = glGetUniformLocation(axesProgram, "aspect")
        axesLengthUniformLoc = glGetUniformLocation(axesProgram, "len")
        axesMvpUniformLoc = glGetUniformLocation(axesProgram, "MVP")

        // model program uniforms
        modelMvpLoc = glGetUniformLocation(modelProgram, "MVP")
        modelNLoc = glGetUniformLocation(modelProgram, "N")
        modelModelLoc = glGetUniformLocation(modelProgram, "model")

        lightPositionLoc = glGetUniformLocation(modelProgram, "lightPosition")
        lightAmbientColorLoc = glGetUniformLocation(modelProgram, "lightAmbientColor")
        lightDiffuseColorLoc = glGetUniformLocation(modelProgram, "lightDiffuseColor")
        lightSpecularColorLoc = glGetUniformLocation(modelProgram, "lightSpecularColor")

        eyePositionLoc = glGetUniformLocation(modelProgram, "eyePosition")

        materialAmbientColorLoc = glGetUniformLocation(modelProgram, "materialAmbientColor")
        materialDiffuseColorLoc = glGetUniformLocation(modelProgram, "materialDiffuseColor")
        materialSpecularColorLoc = glGetUniformLocation(modelProgram, "materialSpecularColor")
        materialShininessLoc = glGetUniformLocation(modelProgram, "materialShininess")

        // Task 1:  get location of woodTex uniform using glGetUniformLocation(...), store it to woodTexLoc variable
        // Task 7:  get location of diceTex uniform, store it to diceTexLoc
        // Task 8:  get location of rocksTex uniform, store it to rocksTexLoc
        woodTexLoc = glGetUniformLocation(modelProgram, "woodTex")


        // create buffers with geometry
        val buffers = IntArray(3)
        glGenBuffers(buffers)
        axesBuffer = buffers[0]
        cubeBuffer = buffers[1]
        teapotBuffer = buffers[2]

        // fill a buffers with geometry
        glBindBuffer(GL_ARRAY_BUFFER, axesBuffer)
        glBufferData(GL_ARRAY_BUFFER, AXES, GL_STATIC_DRAW)

        // load teapot and fill buffer with teapot data
        cube = ObjLoader("/models/cube.obj")
        try {
            cube!!.load()
        } catch (ex: IOException) {
            Logger.getLogger(Cv4::class.java.name).log(Level.SEVERE, null, ex)
            System.exit(1)
        }

        val length = 3 * 8 * cube!!.triangleCount
        val cubeData = BufferUtils.createFloatBuffer(length)
        for (f in 0 until cube!!.triangleCount) {
            val pi = cube!!.vertexIndices?.get(f)
            val ni = cube!!.normalIndices?.get(f)
            val ti = cube!!.texcoordIndices?.get(f)
            for (i in 0..2) {
                val position = cube!!.vertices?.get(pi?.get(i) ?: 0)
                val normal = cube!!.normals?.get(ni?.get(i) ?: 0)
                val texcoord = cube!!.texcoords?.get(ti?.get(i) ?: 0)
                cubeData.put(position)
                cubeData.put(normal)
                cubeData.put(texcoord)
            }
        }
        cubeData.rewind()
        glBindBuffer(GL_ARRAY_BUFFER, cubeBuffer)
        glBufferData(GL_ARRAY_BUFFER, cubeData, GL_STATIC_DRAW)

        // clear buffer binding, so that other code doesn't presume it (easier error detection)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        // create a vertex array object for the geometry
        val arrays = IntArray(2)
        glGenVertexArrays(arrays)
        axesArray = arrays[0]
        cubeArray = arrays[1]

        // get axes program attributes
        var positionAttribLoc = glGetAttribLocation(axesProgram, "position")
        val colorAttribLoc = glGetAttribLocation(axesProgram, "color")
        // bind axes buffer
        glBindVertexArray(axesArray)
        glBindBuffer(GL_ARRAY_BUFFER, axesBuffer)
        // stride and offset are employed as both position and color data reside in the same vertex buffer
        glEnableVertexAttribArray(positionAttribLoc)
        glVertexAttribPointer(positionAttribLoc, 3, GL_FLOAT, false, SIZEOF_AXES_VERTEX, 0)
        glEnableVertexAttribArray(colorAttribLoc)
        glVertexAttribPointer(colorAttribLoc, 3, GL_FLOAT, false, SIZEOF_AXES_VERTEX, COLOR_OFFSET.toLong())

        // get cube program attributes
        positionAttribLoc = glGetAttribLocation(modelProgram, "position")
        val normalAttribLoc = glGetAttribLocation(modelProgram, "normal")
        val texcoordAttribLoc = glGetAttribLocation(modelProgram, "texcoord")

        // bind teapot buffer
        glBindVertexArray(cubeArray)
        glBindBuffer(GL_ARRAY_BUFFER, cubeBuffer)
        glEnableVertexAttribArray(positionAttribLoc)
        glVertexAttribPointer(positionAttribLoc, 3, GL_FLOAT, false, SIZEOF_MODEL_VERTEX, 0)
        glEnableVertexAttribArray(normalAttribLoc)
        glVertexAttribPointer(normalAttribLoc, 3, GL_FLOAT, false, SIZEOF_MODEL_VERTEX, NORMAL_OFFSET.toLong())
        glEnableVertexAttribArray(texcoordAttribLoc)
        glVertexAttribPointer(texcoordAttribLoc, 2, GL_FLOAT, false, SIZEOF_MODEL_VERTEX, TEXCOORD_OFFSET.toLong())

        // clear bindings, so that other code doesn't presume it (easier error detection)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
    }

    private fun render() {
        // Resize OpenGL viewport, i.e., the (bitmap) extents to that is the
        // OpenGL screen space [-1, 1] mapped.
        if (resized) {
            glViewport(0, 0, width, height)
            resized = false
        }

        // animate variables
        if (animate) {
            t += 0.02f
        }

        glPolygonMode(GL_FRONT_AND_BACK, mode)

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        val projection = Matrix4f()
            .perspective(Math.toRadians(60.0).toFloat(), width / height.toFloat(), 1f, 500f)

        val view = Matrix4f()
            .lookAt(camera!!.eyePosition!!, Vector3f(0f, 0f, 0f), Vector3f(0f, 1f, 0f))

        // Task 7:  remove drawing of the whole cube at once
        val mat = Material(Vector3f(1f, 1f, 1f), Vector3f(1f, 1f, 1f), Vector3f(1f, 1f, 1f), 45f)
        drawModel(Matrix4f(), view, projection, cubeArray, cube!!.triangleCount * 3, mat)

        // Task 7:  draw each side of the cube with a different dice (1-6 dots) texture
        //          use second version of drawModel(...) which allows to specify an offset into VBO and the dice texture
        // Task 10: draw only back faces first and only front faces afterwards, use glCullFace(GL_FRONT [or GL_BACK])
        //              tip: draw the cube twice


    }

    private fun drawModel(
        model: Matrix4f,
        view: Matrix4f,
        projection: Matrix4f,
        vao: Int,
        count: Int,
        material: Material
    ) {
        drawModel(model, view, projection, vao, 0, count, material, 0)
    }

    private fun drawModel(
        model: Matrix4f,
        view: Matrix4f,
        projection: Matrix4f,
        vao: Int,
        offset: Int,
        count: Int,
        material: Material?,
        diceTexture: Int
    ) {
        // compute model-view-projection matrix
        val mvp = Matrix4f(projection)
            .mul(view)
            .mul(model)

        // compute normal matrix
        val n = model.get3x3(Matrix3f())
            .invert()
            .transpose()

        glUseProgram(modelProgram)
        glBindVertexArray(vao) // bind vertex array to draw

        glUniform4f(lightPositionLoc, 2f, 5f, 3f, 1f)
        glUniform3f(lightAmbientColorLoc, 0.3f, 0.3f, 0.3f)
        glUniform3f(lightDiffuseColorLoc, 1f, 1f, 1f)
        glUniform3f(lightSpecularColorLoc, 1f, 1f, 1f)

        glUniform3f(eyePositionLoc, camera!!.eyePosition!!.x, camera!!.eyePosition!!.y, camera!!.eyePosition!!.z)

        if (material != null) {
            glUniform3f(
                materialAmbientColorLoc,
                material.ambientColor.x,
                material.ambientColor.y,
                material.ambientColor.z
            )
            glUniform3f(
                materialDiffuseColorLoc,
                material.diffuseColor.x,
                material.diffuseColor.y,
                material.diffuseColor.z
            )
            glUniform3f(
                materialSpecularColorLoc,
                material.specularColor.x,
                material.specularColor.y,
                material.specularColor.z
            )
            glUniform1f(materialShininessLoc, material.shininess)
        }

        // Task 1:  set active texture to texture unit (TU) 0 using glActiveTexture(GL_TEXTURE0)
        //          bind woodTexture to TU 0 using glBindTexture(GL_TEXTURE_2D, woodTexture)
        //          assign TU 0 to woodTex sampler in GLSL program using glUniform1i(woodTexLoc, 0)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, woodTexture)
        glUniform1i(woodTexLoc, 0)


        // Task 7:  set active texture to texture unit (TU) 1 using glActiveTexture(GL_TEXTURE1)
        //          bind diceTexture to TU 1 using glBindTexture(GL_TEXTURE_2D, diceTexture)
        //          assign TU 1 to diceTex sampler in GLSL program using glUniform1i(diceTexLoc, 1)
        // Task 8:  set active texture to texture unit (TU) 2 using glActiveTexture(GL_TEXTURE2)
        //          bind rocksTexture to TU 2 using glBindTexture(GL_TEXTURE_2D, rocksTexture)
        //          assign TU 2 to rocksTex sampler in GLSL program using glUniform1i(rocksTexLoc, 2)


        val mvpData = BufferUtils.createFloatBuffer(16)
        val nData = BufferUtils.createFloatBuffer(9)
        val modelData = BufferUtils.createFloatBuffer(16)
        mvp.get(mvpData)
        n.get(nData)
        model.get(modelData)
        glUniformMatrix4fv(modelMvpLoc, false, mvpData) // pass MVP matrix to shader
        glUniformMatrix3fv(modelNLoc, false, nData) // pass Normal matrix to shader
        glUniformMatrix4fv(modelModelLoc, false, modelData) // pass model matrix to shader

        glDrawArrays(GL_TRIANGLES, offset, count)

        glBindTexture(GL_TEXTURE_2D, 0)
        glBindVertexArray(0)
        glUseProgram(0)
    }

    private fun drawAxes(modelViewProjection: Matrix4f, length: Float) {
        glUseProgram(axesProgram)
        glBindVertexArray(axesArray)

        val mvpData = BufferUtils.createFloatBuffer(16)
        modelViewProjection.get(mvpData)
        glUniform1f(axesLengthUniformLoc, length)
        glUniform1f(axesAspectUniformLoc, width / height.toFloat())
        glUniformMatrix4fv(axesMvpUniformLoc, false, mvpData)

        glDrawArrays(GL_LINES, 0, 6)

        glBindVertexArray(0)
        glUseProgram(0)
    }

    @Throws(IOException::class)
    private fun loadTexture(filename: String): Int {
        val image = ImageIO.read(Cv4::class.java.getResourceAsStream(filename))
        var pixels = (image.raster.dataBuffer as DataBufferByte).data

        val internalFormat: Int
        val format: Int
        when (image.type) {
            BufferedImage.TYPE_3BYTE_BGR -> {
                internalFormat = GL_RGB
                format = GL_BGR
            }
            BufferedImage.TYPE_4BYTE_ABGR -> {
                internalFormat = GL_RGBA
                format = GL_BGRA
                pixels = toBGRA(pixels)
            }
            else -> throw IOException("Unknown image type: " + image.type)
        }

        val textureData = BufferUtils.createByteBuffer(pixels.size)
        textureData.put(pixels)
        textureData.rewind()

        var texture = 0
        // Task 1:  create GL texture object using glGenTextures() and store it in texture local variable
        //          bind the texture using glBindTexture(GL_TEXTURE_2D, texture)
        //          upload texture data using glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, <width>, <height>, 0, format, GL_UNSIGNED_BYTE, <data>)
        //              get width and height from image object
        //              data is loaded to ByteBuffer textureData
        // Task 3:  generate mipmap levels using glGenerateMipmap(GL_TEXTURE_2D)
        texture = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, 256, 256, 0, format, GL_UNSIGNED_BYTE, textureData)

        // Task 1:  set texture filtering using glTexParameteri(...) to GL_NEAREST
        //              minification filter: glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        //              magnification filter: glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        // Task 2:  set texture filtering using glTexParameteri(...) to GL_LINEAR
        //              minification filter: glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        //              magnification filter: glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        // Task 3:  change minification filter (GL_TEXTURE_MIN_FILTER) from GL_LINEAR to GL_LINEAR_MIPMAP_LINEAR
        // Task 5:  set texture wrap mode to GL_MIRRORED_REPEAT in both S and T directions
        //              S direction: glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT)
        //              T direction: glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_MIRRORED_REPEAT)
        //              also try other modes, listed in the attached PDF :)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)

        // unbind texture
        glBindTexture(GL_TEXTURE_2D, 0)

        return texture
    }

    private fun toBGRA(abgr: ByteArray): ByteArray {
        val bgra = ByteArray(abgr.size)
        var i = 0
        while (i < abgr.size) {
            bgra[i] = abgr[i + 1]
            bgra[i + 1] = abgr[i + 2]
            bgra[i + 2] = abgr[i + 3]
            bgra[i + 3] = abgr[i]
            i += 4
        }
        return bgra
    }

    @Throws(IOException::class)
    private fun loadShader(filename: String, shaderType: Int): Int {
        val source = readAllFromResource(filename)
        val shader = glCreateShader(shaderType)

        // create and compile GLSL shader
        glShaderSource(shader, source)
        glCompileShader(shader)

        // check GLSL shader compile status
        val status = glGetShaderi(shader, GL_COMPILE_STATUS)
        if (status == GL_FALSE) {
            val log = glGetShaderInfoLog(shader)
            System.err.println(log)
        }

        return shader
    }

    @Throws(IOException::class)
    private fun loadProgram(vertexShaderFile: String, fragmentShaderFile: String): Int {
        // load vertex and fragment shaders (GLSL)
        val vs = loadShader(vertexShaderFile, GL_VERTEX_SHADER)
        val fs = loadShader(fragmentShaderFile, GL_FRAGMENT_SHADER)

        // create GLSL program, attach shaders and compile it
        val program = glCreateProgram()
        glAttachShader(program, vs)
        glAttachShader(program, fs)
        glLinkProgram(program)

        val status = glGetProgrami(program, GL_LINK_STATUS)
        if (status == GL_FALSE) {
            val log = glGetProgramInfoLog(program)
            System.err.println(log)
        }

        return program
    }

    @Throws(IOException::class)
    private fun readAllFromResource(resource: String): String {
        val `is` = Cv4::class.java.getResourceAsStream(resource) ?: throw IOException("Resource not found: $resource")

        val reader = BufferedReader(InputStreamReader(`is`))
        val sb = StringBuilder()

        var c = reader.read()
        while (c != -1) {
            sb.append(c.toChar())
            c = reader.read()
        }

        return sb.toString()
    }

    companion object {

        private val SIZEOF_AXES_VERTEX = 6 * java.lang.Float.BYTES
        private val COLOR_OFFSET = 3 * java.lang.Float.BYTES

        private val AXES = floatArrayOf(
            // .. position .......... color ....
            // x axis
            1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            // y axis
            0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f,
            // z axis
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f
        )

        private val SIZEOF_MODEL_VERTEX = 8 * java.lang.Float.BYTES
        private val NORMAL_OFFSET = 3 * java.lang.Float.BYTES
        private val TEXCOORD_OFFSET = 6 * java.lang.Float.BYTES
    }
}

fun main(args: Array<String>) {
    Cv4().run()
}

