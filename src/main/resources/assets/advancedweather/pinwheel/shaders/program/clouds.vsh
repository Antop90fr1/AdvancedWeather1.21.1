#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;
layout(location = 3) in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform vec3 TileOffset;
uniform float Morph;
uniform float Threshold;
uniform float MinThickness;
uniform float MaxThickness;
uniform float Puff;
uniform float HeightCurve;
uniform float CellSize;

const float SWELL = 3.0;

out vec3 relPos;
out vec3 normal;
out float heightFrac;
out float coreDepth;
out float cover;

void main() {
    float density = mix(Color.r, Color.g, Morph);
    float body = mix(Color.b, Color.a, Morph);

    cover = smoothstep(Threshold, Threshold + 0.05, density);
    float range = max(0.22, (1.0 - Threshold) * 0.8);
    coreDepth = clamp((body - Threshold) / range, 0.0, 1.0);

    float swell = (abs(UV0.x) - 1.5) * 2.0 * Puff * SWELL * smoothstep(0.0, 0.35, coreDepth);
    float height = (mix(MinThickness, MaxThickness, pow(coreDepth, HeightCurve)) + swell) * cover;
    vec3 local = vec3(Position.x, Position.y * height, Position.z);
    local.xz += sign(UV0) * (1.0 - cover) * 0.5 * CellSize;

    relPos = TileOffset + local;
    normal = Normal;
    heightFrac = Position.y;
    gl_Position = ProjMat * ModelViewMat * vec4(relPos, 1.0);
}
