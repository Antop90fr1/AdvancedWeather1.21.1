#version 330 core

in vec3 relPos;
in vec3 normal;
in float heightFrac;
in float coreDepth;
in float cover;

uniform vec3 LightDir;
uniform vec3 LightColor;
uniform vec3 AmbientTop;
uniform vec3 AmbientBottom;
uniform vec3 FogColor;
uniform vec3 CloudTint;
uniform float Opacity;
uniform float FadeStart;
uniform float FadeEnd;
uniform float Flash;
uniform vec3 FarFlash;
uniform float TileFade;

out vec4 fragColor;

void main() {
    float dist = length(relPos.xz);
    float fade = 1.0 - smoothstep(FadeStart, FadeEnd, dist);
    float alpha = Opacity * cover * TileFade * fade;
    if (alpha < 0.02) discard;

    vec3 n = normalize(normal);
    vec3 viewDir = normalize(relPos);

    vec3 ambient = mix(AmbientBottom, AmbientTop, n.y * 0.5 + 0.5);

    float wrap = clamp((dot(n, LightDir) + 0.4) / 1.4, 0.0, 1.0);

    float selfShadow = mix(0.45, 1.0, heightFrac);
    if (n.y < -0.5) selfShadow = mix(0.85, 0.28, smoothstep(0.0, 1.0, coreDepth));

    vec3 color = CloudTint * (ambient + LightColor * wrap * selfShadow);

    float towardsLight = pow(clamp(dot(viewDir, LightDir), 0.0, 1.0), 10.0);
    color += LightColor * towardsLight * (1.0 - coreDepth) * 0.9;

    color += vec3(0.78, 0.82, 1.0) * Flash * (1.15 - heightFrac * 0.6);

    float towardsStorm = max(dot(relPos.xz / max(dist, 0.001), FarFlash.xy), 0.0);
    color += vec3(0.78, 0.82, 1.0) * FarFlash.z * pow(towardsStorm, 6.0)
            * smoothstep(0.15, 0.8, dist / FadeEnd) * (1.15 - heightFrac * 0.6);

    color = mix(FogColor, color, mix(0.25, 1.0, fade));

    fragColor = vec4(color, alpha);
}
