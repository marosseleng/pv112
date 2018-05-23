package cz.muni.fi.pv112

import cz.muni.fi.pv112.cache.UACache
import cz.muni.fi.pv112.cache.UAName
import cz.muni.fi.pv112.cache.UAName.*
import cz.muni.fi.pv112.logic.HanoiTowers
import cz.muni.fi.pv112.logic.Position
import cz.muni.fi.pv112.logic.Stick
import cz.muni.fi.pv112.logic.UserInputSequence
import cz.muni.fi.pv112.model.BaseModel
import cz.muni.fi.pv112.model.ObjModel
import cz.muni.fi.pv112.utils.Step
import cz.muni.fi.pv112.utils.isLeftToRight
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
import org.lwjgl.opengl.GL12.GL_BGR
import org.lwjgl.opengl.GL12.GL_BGRA
import org.lwjgl.opengl.GL13.GL_MULTISAMPLE
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_STATIC_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glGenBuffers
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.opengl.GL30.glGenerateMipmap
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename
import java.nio.ShortBuffer
import org.lwjgl.system.libc.LibCStdlib.free
import kotlin.math.cos

fun Class<*>.getResourcePath(name: String): String {
    return getResource(name).path
}

/**
 * TODO: for the game to be playable: save user's step from the last known state, in other words with every step, save it and compare with some known state,
 * if the state is recognized (we have it in the "official" solution), set the new "last-known-good-step" pointer to point to it
 */
class Main {
    // the window handle
    private var window: Long = 0

    // window size
    private var width: Int = 0
    private var height: Int = 0
    private var resized = false

    // animation
    private var frameCounter = 0
    private var rotationStartedFrame: Int? = null
    private var rotationEndedFrame: Int? = null
    private var stepAnimating: Step? = null
    private val animationsLocked: Boolean
        get() = stepAnimating != null

    // model
    private lateinit var torusObj: Obj
    private lateinit var cubeObj: Obj
    private lateinit var cylinderObj: Obj

    // our OpenGL resources
    private var torusBuffer: Int = 0
    private var torusArray: Int = 0
    private var cubeBuffer: Int = 0
    private var cubeArray: Int = 0
    private var cylinderBuffer: Int = 0
    private var cylinderArray: Int = 0

    private var woodTexture: Int = 0

    private lateinit var cache: UACache

    private var modelProgram: Int = 0

    private var lookAtEyePosition = Vector3f(0f, 5.5f, 50f)
    private var lookAtCenter = Vector3f(0f, 5.5f, 0f)
    private var lookAtUp = Vector3f(0f, 1f, 0f)

    private lateinit var projection: Matrix4f
    private lateinit var view: Matrix4f

    private var redConicLightPosition: Vector3f = Vector3f()
    private var redConicLightDirection: Vector3f = Vector3f()
    private var greenConicLightPosition: Vector3f = Vector3f()
    private var greenConicLightDirection: Vector3f = Vector3f()

    private lateinit var modelsToDraw: List<BaseModel>

    // game related stuff
    private val numOfDiscs = 8
    private lateinit var game: HanoiTowers
    private lateinit var userInputSequence: UserInputSequence
    private var automaticMode = false
        set(value) {
            onUserInput()
            if (field == value) {
                return
            }
            if (value) {
                // automatic mode enabled
                stepsForAutomaticMode = game.computeStepsForAutomaticSolve()
            } else {
                // manual mode enabled
                stepsForAutomaticMode = mutableListOf()
            }
            field = value
        }

    private var stepsForAutomaticMode = listOf<Step>() // TODO

    private val stickDistance = 15f
    private val frameSpeed: Float
        get() = (Math.PI * stickDistance / 180).toFloat()
    private var yTranslation = 0f
    private var rotationAngleDegrees = 0.0
    private val maxY = 15f

    private var sourcePointer: Int = -1
    private var bufferPointer: Int = -1
    private var context: Long = -1L
    private var device: Long = -1L


    fun run() {
        println("Hello LWJGL " + Version.getVersion() + "!")

        initGLFW()
        loop()

        //Terminate OpenAL
        alDeleteSources(sourcePointer)
        alDeleteBuffers(bufferPointer)
        alcDestroyContext(context)
        alcCloseDevice(device)

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        // Terminate GLFW and free the error callback
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
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
        glfwWindowHint(GLFW_SAMPLES, 8)
        if (System.getProperty("os.name").startsWith("Mac")) {
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE)
        }

        // push initial width and height
        width = 1280
        height = 720

        // Create the window
        window = glfwCreateWindow(width, height, "PV112 project - Maroš Šeleng, 422624", NULL, NULL)
        if (window == NULL) {
            throw RuntimeException("Failed to create the GLFW window")
        }

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, ::keyCallback)
        glfwSetMouseButtonCallback(window, ::mouseButtonCallback)
        glfwSetCursorPosCallback(window, ::cursorPosCallback)
        glfwSetWindowSizeCallback(window, ::windowSizeCallback)

        /* OPENAL */
        val defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER)
        device = alcOpenDevice(defaultDeviceName)

        val attributes = intArrayOf(0)
        context = alcCreateContext(device, attributes)
        alcMakeContextCurrent(context)

        val alcCapabilities = ALC.createCapabilities(device)
        val alCapabilities = AL.createCapabilities(alcCapabilities)

        var rawAudioBuffer: ShortBuffer

        var channels: Int = -1
        var sampleRate: Int = -1
        /* OPENAL END */

        // Get the thread stack and push a new frame
        stackPush()
            .use { stack ->
                val pWidth = stack.mallocInt(1) // int*
                val pHeight = stack.mallocInt(1) // int*

                // Get the window size passed to glfwCreateWindow
                glfwGetWindowSize(window, pWidth, pHeight)

                // Get the resolution of the primary monitor
                val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor()) ?: return@use

                // Center the window
                glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
                )

                /* OPENAL */
                //Allocate space to store return information from the function
                val channelsBuffer = stack.mallocInt(1)
                val sampleRateBuffer = stack.mallocInt(1)

                rawAudioBuffer = stb_vorbis_decode_filename(Main::class.java.getResourcePath("/sounds/buzz.ogg"), channelsBuffer, sampleRateBuffer)

                //Retreive the extra information that was stored in the buffers by the function
                channels = channelsBuffer[0]
                sampleRate = sampleRateBuffer[0]
                //Find the correct OpenAL format

                var format = -1
                if (channels == 1) {
                    format = AL_FORMAT_MONO16
                } else if (channels == 2) {
                    format = AL_FORMAT_STEREO16
                }

                //Request space for the buffer
                bufferPointer = alGenBuffers()

                //Send the data to OpenAL
                alBufferData(bufferPointer, format, rawAudioBuffer, sampleRate)

                //Free the memory allocated by STB
                free(rawAudioBuffer)
            } // the stack frame is popped automatically

        //Request a source
        sourcePointer = alGenSources()

        //Assign the sound we just loaded to the source
        alSourcei(sourcePointer, AL_BUFFER, bufferPointer)

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
        var lastTime = glfwGetTime()
        var nbFrames = 0
        while (!glfwWindowShouldClose(window)) {

            // Measure speed
            val currentTime = glfwGetTime()
            nbFrames++
            if (currentTime - lastTime >= 1.0) { // If last prinf() was more than 1 sec ago
//                println("$nbFrames FPS (${1000.0 / nbFrames.toDouble()} ms/frame)")
                nbFrames = 0
                lastTime += 1.0
            }

            frameCounter++

            render()

            glfwSwapBuffers(window) // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()
        }
    }

    private fun init() {
        projection = Matrix4f()
            .perspective(java.lang.Math.toRadians(60.0).toFloat(), width / height.toFloat(), 1f, 1000f)
        viewChanged()

        // empty scene color
        glClearColor(0.15f, 0.15f, 0.15f, 1.0f)
        glLineWidth(3.0f) // makes lines thicker

        glEnable(GL_DEPTH_TEST)
//        glEnable(GL_BLEND)
        glEnable(GL_MULTISAMPLE)


        try {
            modelProgram = loadProgram(
                "/shaders/model.vs.glsl",
                "/shaders/model.fs.glsl"
            )

            woodTexture = loadTexture("/textures/wood3.jpg")
        } catch (ex: IOException) {
            Logger.getLogger(Main::class.java.name).log(Level.SEVERE, null, ex)
            System.exit(1)
        }

        initCache()

        // create buffers with geometry
        // TODO extract the following code!!!
        val buffers = IntArray(3)
        glGenBuffers(buffers)
        torusBuffer = buffers[0]
        cubeBuffer = buffers[1]
        cylinderBuffer = buffers[2]

        torusObj = Obj("/models/torus.obj")
        cubeObj = Obj("/models/cube.obj")
        cylinderObj = Obj("/models/cylinder.obj")

        try {
            torusObj.load()
            cubeObj.load()
            cylinderObj.load()
        } catch (ex: IOException) {
            Logger.getLogger(Main::class.java.name).log(Level.SEVERE, null, ex)
            System.exit(1)
        }

        var length = 3 * 8 * torusObj.triangleCount
        val torusData = BufferUtils.createFloatBuffer(length)
        for (f in 0 until torusObj.triangleCount) {
            val pi = torusObj.vertexIndices[f]
            val ni = torusObj.normalIndices[f]
            val ti = torusObj.texcoordIndices[f]
            for (i in 0..2) {
                val position = torusObj.vertices[pi[i]]
                val normal = torusObj.normals[ni[i]]
                val texcoord = torusObj.texcoords[ti[i]]
                torusData.put(position)
                torusData.put(normal)
                torusData.put(texcoord)
            }
        }
        torusData.rewind()
        glBindBuffer(GL_ARRAY_BUFFER, torusBuffer)
        glBufferData(GL_ARRAY_BUFFER, torusData, GL_STATIC_DRAW)

        length = 3 * 8 * cubeObj.triangleCount
        val cubeData = BufferUtils.createFloatBuffer(length)
        for (f in 0 until cubeObj.triangleCount) {
            val pi = cubeObj.vertexIndices[f]
            val ni = cubeObj.normalIndices[f]
            val ti = cubeObj.texcoordIndices[f]
            for (i in 0..2) {
                val position = cubeObj.vertices[pi[i]]
                val normal = cubeObj.normals[ni[i]]
                val texcoord = cubeObj.texcoords[ti[i]]
                cubeData.put(position)
                cubeData.put(normal)
                cubeData.put(texcoord)
            }
        }
        cubeData.rewind()
        glBindBuffer(GL_ARRAY_BUFFER, cubeBuffer)
        glBufferData(GL_ARRAY_BUFFER, cubeData, GL_STATIC_DRAW)

        length = 3 * 8 * cylinderObj.triangleCount
        val cylinderData = BufferUtils.createFloatBuffer(length)
        for (f in 0 until cylinderObj.triangleCount) {
            val pi = cylinderObj.vertexIndices[f]
            val ni = cylinderObj.normalIndices[f]
            val ti = cylinderObj.texcoordIndices[f]
            for (i in 0..2) {
                val position = cylinderObj.vertices[pi[i]]
                val normal = cylinderObj.normals[ni[i]]
                val texcoord = cylinderObj.texcoords[ti[i]]
                cylinderData.put(position)
                cylinderData.put(normal)
                cylinderData.put(texcoord)
            }
        }
        cylinderData.rewind()
        glBindBuffer(GL_ARRAY_BUFFER, cylinderBuffer)
        glBufferData(GL_ARRAY_BUFFER, cylinderData, GL_STATIC_DRAW)

        // clear buffer binding, so that other code doesn't presume it (easier error detection)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        // create a vertex array object for the geometry
        val arrays = IntArray(3)
        glGenVertexArrays(arrays)
        torusArray = arrays[0]
        cubeArray = arrays[1]
        cylinderArray = arrays[2]

        // get cube program attributes
        val positionAttribLoc = cache[POSITION]
        val normalAttribLoc = cache[NORMAL]
        val texcoordAttribLoc = cache[TEXCOORD]

        glBindVertexArray(torusArray)
        glBindBuffer(GL_ARRAY_BUFFER, torusBuffer)
        glEnableVertexAttribArray(positionAttribLoc)
        glVertexAttribPointer(positionAttribLoc, 3, GL_FLOAT, false, SIZEOF_MODEL_VERTEX, 0)
        glEnableVertexAttribArray(normalAttribLoc)
        glVertexAttribPointer(normalAttribLoc, 3, GL_FLOAT, false, SIZEOF_MODEL_VERTEX, NORMAL_OFFSET.toLong())
        glEnableVertexAttribArray(texcoordAttribLoc)
        glVertexAttribPointer(texcoordAttribLoc, 2, GL_FLOAT, false, SIZEOF_MODEL_VERTEX, TEXCOORD_OFFSET.toLong())

        glBindVertexArray(cubeArray)
        glBindBuffer(GL_ARRAY_BUFFER, cubeBuffer)
        glEnableVertexAttribArray(positionAttribLoc)
        glVertexAttribPointer(positionAttribLoc, 3, GL_FLOAT, false, SIZEOF_MODEL_VERTEX, 0)
        glEnableVertexAttribArray(normalAttribLoc)
        glVertexAttribPointer(normalAttribLoc, 3, GL_FLOAT, false, SIZEOF_MODEL_VERTEX, NORMAL_OFFSET.toLong())
        glEnableVertexAttribArray(texcoordAttribLoc)
        glVertexAttribPointer(texcoordAttribLoc, 2, GL_FLOAT, false, SIZEOF_MODEL_VERTEX, TEXCOORD_OFFSET.toLong())

        glBindVertexArray(cylinderArray)
        glBindBuffer(GL_ARRAY_BUFFER, cylinderBuffer)
        glEnableVertexAttribArray(positionAttribLoc)
        glVertexAttribPointer(positionAttribLoc, 3, GL_FLOAT, false, SIZEOF_MODEL_VERTEX, 0)
        glEnableVertexAttribArray(normalAttribLoc)
        glVertexAttribPointer(normalAttribLoc, 3, GL_FLOAT, false, SIZEOF_MODEL_VERTEX, NORMAL_OFFSET.toLong())
        glEnableVertexAttribArray(texcoordAttribLoc)
        glVertexAttribPointer(texcoordAttribLoc, 2, GL_FLOAT, false, SIZEOF_MODEL_VERTEX, TEXCOORD_OFFSET.toLong())

        // clear bindings, so that other code doesn't presume it (easier error detection)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)

        initGame()
    }

    private fun initCache() {
        cache = UACache(modelProgram).apply {
            cache(*UAName.values())
        }
    }

    private fun initGame() {
        game = HanoiTowers()
        userInputSequence = UserInputSequence(onValuePushedCallback = ::onUserInput)
    }

    private fun onUserInput() {
        if (userInputSequence.currentUnsetPosition == 0) {
            // sequence is reset
            greenConicLightPosition = Vector3f()
            greenConicLightDirection = Vector3f()
            redConicLightPosition = Vector3f()
            redConicLightDirection = Vector3f()
            return
        }
        val (from, to) = userInputSequence
        val (greenPosition, greenDirection) = when (from) {
            Position.LEFT -> Pair(Vector3f(-stickDistance, 45f, 0f), Vector3f(-stickDistance, 0f, 0f))
            Position.CENTER -> Pair(Vector3f(0f, 45f, 0f), Vector3f(0f, 0f, 0f))
            Position.RIGHT -> Pair(Vector3f(stickDistance, 45f, 0f), Vector3f(stickDistance, 0f, 0f))
        }
        greenConicLightPosition = greenPosition
        greenConicLightDirection = greenDirection
        val (redPosition, redDirection) = when (to) {
            Position.LEFT -> Pair(Vector3f(-stickDistance, 45f, 0f), Vector3f(-stickDistance, 0f, 0f))
            Position.CENTER -> Pair(Vector3f(0f, 45f, 0f), Vector3f(0f, 0f, 0f))
            Position.RIGHT -> Pair(Vector3f(stickDistance, 45f, 0f), Vector3f(stickDistance, 0f, 0f))
        }
        redConicLightPosition = redPosition
        redConicLightDirection = redDirection
    }

    private fun render() {
        // Resize OpenGL viewport, i.e., the (bitmap) extents to that is the
        // OpenGL screen space [-1, 1] mapped.
        if (resized) {
            glViewport(0, 0, width, height)
            resized = false
        }

        if (!animationsLocked && automaticMode) {
            val nextStep = stepsForAutomaticMode.firstOrNull()
            if (nextStep != null) {
                stepsForAutomaticMode = stepsForAutomaticMode.drop(1)
                game.move(nextStep.first, nextStep.second)
                stepAnimating = game.lastStep
            }
        }

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        prepareModels()
        drawModels()
    }

    private fun prepareModels() {
        val result = mutableListOf<BaseModel>()
        val factor = 0.88

        /* RINGS */
        for ((position, stick) in game.sticks) {
            val staticRings = if (animationsLocked && (position == stepAnimating?.second)) {
                stick.rings.drop(1)
            } else {
                stick.rings
            }

            val dynamicRing = if (animationsLocked && position == stepAnimating?.second) {
                stick.rings.firstOrNull()
            } else {
                null
            }

            // Add static rings
            staticRings.reversed().forEachIndexed { index, ring ->
                val scale = Math.pow(factor, numOfDiscs - ring.radius.toDouble())
                val yTranslate = getYForRingOnStick(index, stick, factor)
                val material = Material.discMaterials[(ring.radius - 1) % Material.discMaterials.size]

                result.add(
                    ObjModel(
                        obj = torusObj,
                        model = Matrix4f()
                            .translate(getTranslationX(position), yTranslate, 0f)
                            .scale(scale.toFloat()),
                        vao = torusArray,
                        material = material,
                        texture = null
                    )
                )
            }

            if (dynamicRing != null) {
                var animationDone = false
                val scale = Math.pow(factor, numOfDiscs - dynamicRing.radius.toDouble())
                if (yTranslation < maxY && rotationStartedFrame == null) {
                    // MOVE UP
                    if (yTranslation <= 0) {
                        val startingStick = game.sticks[stepAnimating?.first]?.deepCopy()
                        startingStick?.push(dynamicRing.copy())
                        yTranslation =
                                getYForRingOnStick(startingStick?.rings?.lastIndex ?: 7, startingStick ?: stick, factor)
                    }
                    yTranslation = increaseUntil(yTranslation, frameSpeed, maxY)
                } else if (rotationEndedFrame == null) {
                    // ROTATE
                    if (rotationStartedFrame == null) {
                        rotationStartedFrame = frameCounter
                    }
                    val diff = frameCounter - (rotationStartedFrame ?: 0)
                    rotationAngleDegrees = if (diff <= 180) {
                        diff.toDouble()
                    } else {
                        rotationEndedFrame = frameCounter
                        180.0
                    }
                } else if (yTranslation > getYForRingOnStick(stick.rings.lastIndex, stick, factor)) {
                    // MOVING DOWN
                    yTranslation = decreaseUntil(
                        yTranslation,
                        frameSpeed,
                        getYForRingOnStick(stick.rings.lastIndex, stick, factor)
                    )
                } else {
                    // ANIMATION DONE, RESETTING
                    animationDone = true
                }

                val material = Material.discMaterials[(dynamicRing.radius - 1) % Material.discMaterials.size]

                result.add(
                    ObjModel(
                        obj = torusObj,
                        model = Matrix4f()
                            .translate(getCenterOfRotationX(), yTranslation, 0f)
                            .rotate(
                                Math.toRadians(rotationAngleDegrees * getRotationAngleFactor()).toFloat(),
                                0f,
                                0f,
                                1f
                            )
                            .translate(getRadiusOfRotationX(), 0f, 0f)
                            .scale(scale.toFloat()),
                        vao = torusArray,
                        material = material,
                        texture = null
                    )
                )

                if (animationDone) {
                    resetAnimation()
                }
            }
        }

        /* WALL */
        result.add(
            ObjModel(
                obj = cubeObj,
                model = Matrix4f()
                    .translate(0f, 0f, -30f)
                    .scale(100f, 70f, 0.1f),
                vao = cubeArray,
                material = null
            )
        )

        /* STICKS */
        for (position in Position.values()) {
            result.add(
                ObjModel(
                    obj = cylinderObj,
                    model = Matrix4f()
                        .translate(getTranslationX(position), 0f, 0f)
                        .scale(0.65f, 4.6f, 0.65f),
                    vao = cylinderArray,
                    material = null,
                    texture = woodTexture
                )
            )
        }

        /* TABLE */
        result.add(
            ObjModel(
                obj = cylinderObj,
                model = Matrix4f()
                    .scale(17f, 0.2f, 6f)
                    .rotate(Math.PI.toFloat(), 1f, 0f, 0f),
                vao = cylinderArray,
                material = null,
                texture = woodTexture
            )
        )

        modelsToDraw = result
    }

    private fun getCenterOfRotationX(): Float {
        return when (stepAnimating) {
            Position.LEFT to Position.CENTER, Position.CENTER to Position.LEFT -> {
                -stickDistance / 2
            }
            Position.LEFT to Position.RIGHT, Position.RIGHT to Position.LEFT -> {
                0f
            }
            Position.CENTER to Position.RIGHT, Position.RIGHT to Position.CENTER -> {
                stickDistance / 2
            }
            else -> {
                0f
            }
        }
    }

    /**
     * When rotating from left to right, use positive angle, when rotating from right to left, use negative angle
     */
    private fun getRotationAngleFactor(): Float {
        return if (stepAnimating?.isLeftToRight() == true) {
            -1f
        } else {
            1f
        }
    }

    private fun getRadiusOfRotationX(): Float {
        return when (stepAnimating) {
            Pair(Position.LEFT, Position.CENTER) -> {
                -stickDistance / 2
            }
            Pair(Position.CENTER, Position.LEFT) -> {
                stickDistance / 2
            }
            Pair(Position.LEFT, Position.RIGHT) -> {
                -stickDistance
            }
            Pair(Position.RIGHT, Position.LEFT) -> {
                stickDistance
            }
            Pair(Position.CENTER, Position.RIGHT) -> {
                -stickDistance / 2
            }
            Pair(Position.RIGHT, Position.CENTER) -> {
                stickDistance / 2
            }
            else -> {
                0f
            }
        }
    }

    private fun getTranslationX(position: Position): Float {
        return when (position) {
            Position.LEFT -> -stickDistance
            Position.CENTER -> 0f
            Position.RIGHT -> stickDistance
        }
    }

    private fun increaseUntil(start: Float, step: Float, max: Float): Float {
        return if (start + step < max) {
            start + step
        } else {
            max
        }
    }

    private fun decreaseUntil(start: Float, step: Float, min: Float): Float {
        return if (start - step > min) {
            start - step
        } else {
            min
        }
    }

    /**
     * indexOnStick == 0 means bottom most one
     */
    private fun getYForRingOnStick(indexOnStick: Int, stick: Stick, factor: Double): Float {
        val reversedRings = stick.rings.reversed()
        val myHeight = Math.pow(factor, 8.0 - reversedRings[indexOnStick].radius) * 2.5
        var cumSum = 0.0
        (0 until indexOnStick).forEach { i ->
            cumSum += (Math.pow(factor, 8.0 - reversedRings[i].radius) * 2.5)
        }
        return (cumSum + myHeight / 2.0).toFloat()
    }

    private fun drawModels() {
        modelsToDraw.forEach(::drawModel)
    }

    private fun drawModel(model: BaseModel) {
        drawModel(model.model, model.vao, model.offset, model.count, model.material, model.texture ?: 0)
    }

    private fun drawModel(
        model: Matrix4f,
        vao: Int,
        offset: Int,
        count: Int,
        material: Material?,
        texture: Int
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

        glUniform4f(cache[LIGHT_POSITION], 0f, 75f, 0f, 1f)
        glUniform4f(cache[LIGHT_AMBIENT_COLOR], 0.3f, 0.3f, 0.3f, 1f)
        glUniform4f(cache[LIGHT_DIFFUSE_COLOR], 1f, 1f, 1f, 1f)
        glUniform4f(cache[LIGHT_SPECULAR_COLOR], 1f, 1f, 1f, 1f)

        glUniform4f(cache[RED_CONIC_LIGHT_POSITION], redConicLightPosition.x, redConicLightPosition.y, redConicLightPosition.z, 1f)
        glUniform4f(cache[RED_CONIC_LIGHT_DIRECTION], redConicLightDirection.x, redConicLightDirection.y, redConicLightDirection.z, 1f)
        glUniform4f(cache[GREEN_CONIC_LIGHT_POSITION], greenConicLightPosition.x, greenConicLightPosition.y, greenConicLightPosition.z, 1f)
        glUniform4f(cache[GREEN_CONIC_LIGHT_DIRECTION], greenConicLightDirection.x, greenConicLightDirection.y, greenConicLightDirection.z, 1f)
        glUniform1f(cache[CONIC_LIGHT_CUTOFF], cos(Math.toRadians(8.5)).toFloat())

        glUniform3f(cache[EYE_POSITION], lookAtEyePosition.x, lookAtEyePosition.y, lookAtEyePosition.z)

        if (material != null) {
            glUniform4f(
                cache[MATERIAL_AMBIENT_COLOR],
                material.ambientColor.x,
                material.ambientColor.y,
                material.ambientColor.z,
                material.ambientColor.w
            )
            glUniform4f(
                cache[MATERIAL_DIFFUSE_COLOR],
                material.diffuseColor.x,
                material.diffuseColor.y,
                material.diffuseColor.z,
                material.diffuseColor.w
            )
            glUniform4f(
                cache[MATERIAL_SPECULAR_COLOR],
                material.specularColor.x,
                material.specularColor.y,
                material.specularColor.z,
                material.specularColor.w
            )
            glUniform1f(cache[MATERIAL_SHININESS], material.shininess)
            glUniform1i(cache[USE_PROCEDURAL_TEXTURE], 0)
        } else {
            glUniform1i(cache[USE_PROCEDURAL_TEXTURE], 1)
        }

        if (texture != 0) {
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, texture)
            glUniform1i(cache[WOOD_TEX], 0)
            glUniform1i(cache[READ_TEXTURE_FROM_SAMPLER], 1)
        } else {
            glUniform1i(cache[READ_TEXTURE_FROM_SAMPLER], 0)
        }


        val mvpData = BufferUtils.createFloatBuffer(16)
        val nData = BufferUtils.createFloatBuffer(9)
        val modelData = BufferUtils.createFloatBuffer(16)
        mvp.get(mvpData)
        n.get(nData)
        model.get(modelData)
        glUniformMatrix4fv(cache[UAName.MVP], false, mvpData) // pass MVP matrix to shader
        glUniformMatrix3fv(cache[UAName.N], false, nData) // pass Normal matrix to shader
        glUniformMatrix4fv(cache[UAName.MODEL], false, modelData) // pass model matrix to shader

        glDrawArrays(GL_TRIANGLES, offset, count)

        glBindTexture(GL_TEXTURE_2D, 0)
        glBindVertexArray(0)
        glUseProgram(0)
    }

    private fun loadTexture(filename: String): Int {
        val image = ImageIO.read(Main::class.java.getResourceAsStream(filename))
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

        val texture = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            internalFormat,
            image.width,
            image.height,
            0,
            format,
            GL_UNSIGNED_BYTE,
            textureData
        )
        glGenerateMipmap(GL_TEXTURE_2D)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)

        // unbind texture
        glBindTexture(GL_TEXTURE_2D, 0)

        return texture
    }

    private fun keyCallback(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if (action == GLFW_RELEASE) {
            when (key) {
                GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true)
                GLFW_KEY_ENTER -> {
                    if (automaticMode) {
                        // automatic mode is on
                        alSourcePlay(sourcePointer)
                        return
                    }
                    if (animationsLocked) {
                        // other animation in progress
                        alSourcePlay(sourcePointer)
                        return
                    }
                    if (userInputSequence.isValid()) {
                        val (from, to) = userInputSequence
                        if (game.move(from, to)) {
                            // valid move
                            stepAnimating = game.lastStep
                            userInputSequence.reset()
                        } else {
                            alSourcePlay(sourcePointer)
                        }
                    } else {
                        alSourcePlay(sourcePointer)
                    }
                }
                GLFW_KEY_1 -> {
                    if (automaticMode) {
                        return
                    }
                    userInputSequence.push(1)
                }
                GLFW_KEY_2 -> {
                    if (automaticMode) {
                        return
                    }
                    userInputSequence.push(2)
                }
                GLFW_KEY_3 -> {
                    if (automaticMode) {
                        return
                    }
                    userInputSequence.push(3)
                }
                GLFW_KEY_C -> {
                    if (automaticMode) {
                        return
                    }
                    userInputSequence.reset()
                }
                GLFW_KEY_A -> {
                    automaticMode = true
                }
                GLFW_KEY_M -> {
                    automaticMode = false
                }
                GLFW_KEY_R -> {
                    resetGame()
                    resetAnimation()
                }
                GLFW_KEY_UP -> {
                    lookAtEyePosition = Vector3f(0f, 50f, 0f)
                    lookAtCenter = Vector3f(0f, 0f, 0f)
                    lookAtUp = Vector3f(0f, 0f, -1f)
                    viewChanged()
                }
                GLFW_KEY_DOWN -> {
                    lookAtEyePosition = Vector3f(0f, 5.5f, 50f)
                    lookAtCenter = Vector3f(0f, 5.5f, 0f)
                    lookAtUp = Vector3f(0f, 1f, 0f)
                    viewChanged()
                }
            }
        }
    }

    private fun resetGame() {
        game = HanoiTowers()
        userInputSequence.reset()
        automaticMode = false
    }

    private fun resetAnimation() {
        stepAnimating = null
        yTranslation = 0f
        rotationStartedFrame = null
        rotationEndedFrame = null
        rotationAngleDegrees = 0.0
    }

    private fun viewChanged() {
        view = Matrix4f()
            .lookAt(lookAtEyePosition, lookAtCenter, lookAtUp)
    }

    private fun mouseButtonCallback(window: Long, button: Int, action: Int, mods: Int) {
//        if (action == GLFW_PRESS) {
//            if (button == GLFW_MOUSE_BUTTON_1) {
//                camera.updateMouseButton(Camera.Button.LEFT, true)
//            } else if (button == GLFW_MOUSE_BUTTON_2) {
//                camera.updateMouseButton(Camera.Button.RIGHT, true)
//            }
//        } else if (action == GLFW_RELEASE) {
//            if (button == GLFW_MOUSE_BUTTON_1) {
//                camera.updateMouseButton(Camera.Button.LEFT, false)
//            } else if (button == GLFW_MOUSE_BUTTON_2) {
//                camera.updateMouseButton(Camera.Button.RIGHT, false)
//            }
//        }
    }

    private fun cursorPosCallback(window: Long, xpos: Double, ypos: Double) {
//        camera.updateMousePosition(xpos, ypos)
    }

    private fun windowSizeCallback(window: Long, width: Int, height: Int) {
        this.width = width
        this.height = height
        resized = true
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

    private fun readAllFromResource(resource: String): String {
        return (javaClass.getResourceAsStream(resource) ?: throw IOException("Resource not found: $resource"))
            .bufferedReader()
            .lineSequence()
            .joinToString(separator = "\n")
    }

    companion object {
        private const val SIZEOF_MODEL_VERTEX = 8 * java.lang.Float.BYTES
        private const val NORMAL_OFFSET = 3 * java.lang.Float.BYTES
        private const val TEXCOORD_OFFSET = 6 * java.lang.Float.BYTES
    }
}

fun main(args: Array<String>) {
    Main().run()
}

