package cz.muni.fi.pv112

import org.joml.Vector3f

/**
 * SIMPLE PV112 CAMERA CLASS.
 *
 * This is a VERY SIMPLE class that allows to very simply move with the camera.
 * It is not a perfect, brilliant, smart, or whatever implementation of a
 * camera, but it is sufficient for PV112 lectures.
 *
 * Use left mouse button to change the point of view. Use right mouse button to
 * zoom in and zoom out.
 */
class Camera {

    /// direction is an angle in which determines into which direction in xz plane I look.
    ///		- 0 degrees .. I look in -z direction
    ///		- 90 degrees .. I look in -x direction
    ///		- 180 degrees .. I look in +z direction
    ///		- 270 degrees .. I look in +x direction
    private var directon: Float = 0.toFloat()

    /// elevation is an angle in which determines from which "height" I look.
    ///		- positive elevation .. I look from above the xz plane
    ///		- negative elevation .. I look from below the xz plane
    private var elevation: Float = 0.toFloat()

    /// Distance from (0,0,0), the point at which I look
    private var distance: Float = 0.toFloat()

    /// Final position of the eye in world space coordinates, for LookAt or shaders
    /// Returns the position of the eye in world space coordinates
    var eyePosition: Vector3f? = null
        private set

    /// Last X and Y coordinates of the mouse cursor
    private var lastX: Double = 0.toDouble()
    private var lastY: Double = 0.toDouble()

    /// True or false if moused buttons are pressed and the user rotates/zooms the camera
    private var rotating: Boolean = false
    private var zooming: Boolean = false

    enum class Button {
        LEFT, RIGHT
    }

    init {
        directon = 0.0f
        elevation = 0.0f
        distance = 10.0f
        lastX = 0.0
        lastY = 0.0
        rotating = false
        zooming = false
        updateEyePosition()
    }

    /// Recomputes 'eye_position' from 'angle_direction', 'angle_elevation', and 'distance'
    private fun updateEyePosition() {
        val x = (distance.toDouble() * Math.cos(elevation.toDouble()) * -Math.sin(directon.toDouble())).toFloat()
        val y = (distance * Math.sin(elevation.toDouble())).toFloat()
        val z = (distance.toDouble() * Math.cos(elevation.toDouble()) * Math.cos(directon.toDouble())).toFloat()
        eyePosition = Vector3f(x, y, z)
    }

    /// Called when the user presses or releases a mouse button (see MainWindow)
    fun updateMouseButton(button: Button, pressed: Boolean) {
        // Left mouse button affects the angles
        if (button == Button.LEFT) {
            rotating = pressed
        }
        // Right mouse button affects the zoom
        if (button == Button.RIGHT) {
            zooming = pressed
        }
    }

    /// Called when the user moves with the mouse cursor (see MainWindow)
    fun updateMousePosition(x: Double, y: Double) {
        val dx = (x - lastX).toFloat()
        val dy = (y - lastY).toFloat()
        lastX = x
        lastY = y

        if (rotating) {
            directon += dx * ANGLE_SENSITIVITY
            elevation += dy * ANGLE_SENSITIVITY

            // Clamp the results
            if (elevation > MAX_ELEVATION) {
                elevation = MAX_ELEVATION
            }
            if (elevation < MIN_ELEVALITON) {
                elevation = MIN_ELEVALITON
            }
        }
        if (zooming) {
            distance *= 1.0f + dy * ZOOM_SENSITIVITY

            // Clamp the results
            if (distance < MIN_DISTANCE) {
                distance = MIN_DISTANCE
            }
        }

        updateEyePosition()
    }

    companion object {

        /// Constants that defines the behaviour of the camera
        ///		- Minimum elevation in radians
        private val MIN_ELEVALITON = -1.5f
        ///		- Maximum elevation in radians
        private val MAX_ELEVATION = 1.5f
        ///		- Minimum distance from the point of interest
        private val MIN_DISTANCE = 1f
        ///		- Sensitivity of the mouse when changing elevation or direction angles
        private val ANGLE_SENSITIVITY = 0.008f
        ///		- Sensitivity of the mouse when changing zoom
        private val ZOOM_SENSITIVITY = 0.003f
    }
}
