#version 330
#extension GL_ARB_separate_shader_objects : require

// Infinity Castle colour grade: split-tones the image toward deep crimson shadows and amber highlights, the way the
// castle's interiors glow in the show, then darkens the corners so the view seems to fall away into the distance.

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// Member order matters: std140 packs each float into the tail of the vec3 before it.
layout(std140) uniform GradeConfig {
    vec3 ShadowTint;
    float Strength;
    vec3 HighlightTint;
    float Vignette;
};

layout(location = 0) out vec4 fragColor;

const vec3 LUMA = vec3(0.299, 0.587, 0.114);

void main() {
    vec3 color = texture(InSampler, texCoord).rgb;
    float luma = dot(color, LUMA);

    vec3 tint = mix(ShadowTint, HighlightTint, smoothstep(0.02, 0.85, luma));
    vec3 toned = tint * (luma * 1.35 + 0.015);
    vec3 graded = mix(color, toned, Strength);

    // Gentle S-curve for contrast.
    graded = graded * graded * (3.0 - 2.0 * graded) * 0.35 + graded * 0.65;

    vec2 centered = (texCoord - 0.5) * vec2(InSize.x / InSize.y, 1.0);
    float falloff = smoothstep(0.95, 0.25, length(centered));
    graded *= mix(1.0 - Vignette, 1.0, falloff);

    fragColor = vec4(clamp(graded, 0.0, 1.0), 1.0);
}
