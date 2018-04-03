#version 330

in vec3 position;
in vec3 color;

out vec3 vertColor;

// width/height ratio
uniform float aspect;
uniform mat4 MVP;
uniform float len;

void main() {
    vertColor = color;
    vec4 pos = vec4(len * position, 1.0);
    pos = MVP * pos;
    gl_Position = pos;
}