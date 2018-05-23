package cz.muni.fi.pv112.cache

enum class UAName(val varName: String, val type: UACache.UAMode = UACache.UAMode.UNIFORM) {
    MVP("MVP"),
    N("N"),
    MODEL("model"),

    LIGHT_POSITION("lightPosition"),
    LIGHT_AMBIENT_COLOR("lightAmbientColor"),
    LIGHT_DIFFUSE_COLOR("lightDiffuseColor"),
    LIGHT_SPECULAR_COLOR("lightSpecularColor"),

    RED_CONIC_LIGHT_POSITION("redConicLightPosition"),
    RED_CONIC_LIGHT_DIRECTION("redConicLightDirection"),
    GREEN_CONIC_LIGHT_POSITION("greenConicLightPosition"),
    GREEN_CONIC_LIGHT_DIRECTION("greenConicLightDirection"),
    CONIC_LIGHT_CUTOFF("conicLightCutoff"),

    EYE_POSITION("eyePosition"),

    MATERIAL_AMBIENT_COLOR("materialAmbientColor"),
    MATERIAL_DIFFUSE_COLOR("materialDiffuseColor"),
    MATERIAL_SPECULAR_COLOR("materialSpecularColor"),
    MATERIAL_SHININESS("materialShininess"),

    WOOD_TEX("woodTex"),

    USE_PROCEDURAL_TEXTURE("useProceduralTexture"),
    READ_TEXTURE_FROM_SAMPLER("readTextureFromSampler"),

    POSITION("position", UACache.UAMode.ATTRIB),
    NORMAL("normal", UACache.UAMode.ATTRIB),
    TEXCOORD("texcoord", UACache.UAMode.ATTRIB)
}