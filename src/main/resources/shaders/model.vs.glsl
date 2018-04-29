#version 330

in vec3 position;
in vec3 normal;
in vec2 texcoord;

out vec3 vNormal;
out vec3 vPosition;

out vec2 vTexCoord;

uniform mat4 MVP; // model-view-projection matrix
uniform mat3 N; // transpose of inversed model matrix
uniform mat4 model; // model matrix

void main() {
    vNormal = normalize(N * normal);
    vPosition = vec3(model * vec4(position, 1.0));
    vTexCoord = texcoord;
    gl_Position = MVP * vec4(position, 1.0);
}
