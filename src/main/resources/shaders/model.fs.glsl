#version 330

in vec3 vNormal;
in vec3 vPosition;

// Task 1:  add input variable vec2 vTexCoord in order to pass texture coordinate to fragment shader
in vec2 vTexCoord;

out vec4 fragColor;

uniform vec4 lightPosition;
uniform vec3 lightAmbientColor;
uniform vec3 lightDiffuseColor;
uniform vec3 lightSpecularColor;

uniform vec3 eyePosition;

uniform vec3 materialAmbientColor;
uniform vec3 materialDiffuseColor;
uniform vec3 materialSpecularColor;
uniform float materialShininess;

// Task 1:  add texture sampler woodTex by adding new uniform: uniform sampler2D woodTex
// Task 7:  add texture sampler diceTex
// Task 8:  add texture sampler rocksTex
uniform sampler2D woodTex;

vec3 phong(vec3 matAmbientColor, vec3 matDiffuseColor, vec3 matSpecularColor, float matShininess);

void main() {
    // Task 1:  sample woodTex sampler using texture(sampler2D texture, vec2 textureCoords) function (returns vec4)
    //            store rgb components to a vec3 variable woodColor
    // Task 7:  sample diceTex sampler using texture() function and store the colors to diceColor
    // Task 8:  sample rocksTex sampler using texture() function and store the colors to rocksColor

    // Task 8:  mix rocksColor and woodColor based on the dice texture
    //            you can use the mix(color1, color2, ratio) function. ratio can be float or vec3

    // Task 6:  using the phong() function below, add lightning to your textured model
    //            use wood texture color as material ambient and material diffuse colors
    // Task 7:  use diceColor to texture your cube
    vec3 color = phong(materialAmbientColor, materialDiffuseColor, materialSpecularColor, materialShininess);
    vec3 woodColor = texture(woodTex, vTexCoord).rgb;

    // Task 1:  use woodColor to color the fragment
    // Task 6:  use your texture-light combined color to color the fragment
    // Task 9:  lower fragment alpha (opacity) from 100% to 40% (range is 0.0-1.0)
    fragColor = vec4(woodColor, 1.0);
}

/*
 * Computes lighting using Blinn-Phong model.
 */
vec3 phong(vec3 matAmbientColor, vec3 matDiffuseColor, vec3 matSpecularColor, float matShininess) {
    vec3 N = normalize(vNormal);

    vec3 lightDirection;
    if (lightPosition.w == 0.0) {
        lightDirection = normalize(lightPosition.xyz);
    } else {
        lightDirection = normalize(lightPosition.xyz - vPosition);
    }

    vec3 viewDirection = normalize(eyePosition - vPosition);
    vec3 halfVector = normalize(lightDirection + viewDirection);

    vec3 ambient = lightAmbientColor * matAmbientColor;

    float diffuseFactor = max(dot(N, lightDirection), 0.0);
    vec3 diffuse = lightDiffuseColor * matDiffuseColor * diffuseFactor;

    float specularFactor = pow(max(dot(N, halfVector), 0.0), matShininess) * diffuseFactor;
    vec3 specular = lightSpecularColor * matSpecularColor * specularFactor;

    return ambient + diffuse + specular;
}
