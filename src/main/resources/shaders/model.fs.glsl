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

//uniform sampler2D woodTex;

vec4 phong(vec4 matAmbientColor, vec4 matDiffuseColor, vec4 matSpecularColor, float matShininess);

void main() {
    vec4 color = phong(materialAmbientColor, materialDiffuseColor, materialSpecularColor, materialShininess);
//    vec3 woodColor = texture(woodTex, vTexCoord).rgb;
//    fragColor = vec4(woodColor, 1.0);
    fragColor = color;
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
