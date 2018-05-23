#version 330

in vec3 vNormal;
in vec3 vPosition;

in vec2 vTexCoord;

out vec4 fragColor;

uniform vec4 lightPosition;
uniform vec4 lightAmbientColor;
uniform vec4 lightDiffuseColor;
uniform vec4 lightSpecularColor;

uniform vec3 eyePosition;

uniform vec4 materialAmbientColor;
uniform vec4 materialDiffuseColor;
uniform vec4 materialSpecularColor;
uniform float materialShininess;

uniform bool useProceduralTexture;
uniform bool readTextureFromSampler;

uniform sampler2D woodTex;

const vec3 orange = vec3(0.471, 0.286, 0.216);
const vec3 grey = vec3(0.6602, 0.6562, 0.6445);

vec4 phong(vec4 matAmbientColor, vec4 matDiffuseColor, vec4 matSpecularColor, float matShininess);
vec4 brickWall();
float random(vec2 p);
vec3 getOrangeForPos(vec2 p);

void main() {
    vec4 color = mix(phong(materialAmbientColor, materialDiffuseColor, materialSpecularColor, materialShininess), brickWall(), int(useProceduralTexture));

    vec4 woodColor = vec4(texture(woodTex, vTexCoord).rgb, 1.0);

    vec4 totalResult = mix(color, woodColor, int(readTextureFromSampler));
    fragColor = totalResult;
}

/*
 * Computes lighting using Blinn-Phong model.
 */
vec4 phong(vec4 matAmbientColor, vec4 matDiffuseColor, vec4 matSpecularColor, float matShininess) {
    vec3 N = normalize(vNormal);

    vec3 lightDirection;
    if (lightPosition.w == 0.0) {
        lightDirection = normalize(lightPosition.xyz);
    } else {
        lightDirection = normalize(lightPosition.xyz - vPosition);
    }

    vec3 viewDirection = normalize(eyePosition - vPosition);
    vec3 halfVector = normalize(lightDirection + viewDirection);

    vec4 ambient = lightAmbientColor * matAmbientColor;

    float diffuseFactor = max(dot(N, lightDirection), 0.0);
    vec4 diffuse = lightDiffuseColor * matDiffuseColor * diffuseFactor;

    float specularFactor = pow(max(dot(N, halfVector), 0.0), matShininess) * diffuseFactor;
    vec4 specular = lightSpecularColor * matSpecularColor * specularFactor;

    return ambient + diffuse + specular;
}

vec4 brickWall() {
    float lgY = floor(40 * vTexCoord.y);
    float modulo = mod(lgY, 2);
    vec3 horizontal = mix(grey, getOrangeForPos(vTexCoord), smoothstep(0.035, 0.09, fract(17 * vTexCoord.x + modulo * 0.5)) - smoothstep(0.91, 0.965, fract(17 * vTexCoord.x + modulo * 0.5)));
    vec3 vertical = mix(grey, horizontal, smoothstep(0.06, 0.16, fract(40 * vTexCoord.y)) - smoothstep(0.84, 0.94, fract(40 * vTexCoord.y)));

    return vec4(vertical, 1.0);
}

float random(vec2 p) {
    return fract(sin(dot(p, vec2(15.79, 81.93)) * 45678.9123));
}

vec3 getOrangeForPos(vec2 p) {
    return orange * (1.0 + (random(p) / 3.0));
}
