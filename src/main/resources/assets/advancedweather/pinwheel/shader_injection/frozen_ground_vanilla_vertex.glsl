
out vec3 AwFrostPos;
out vec4 AwFrostOpen;
out float AwFrostWarm;
out vec3 AwFrostLight;

void tail() {
    float awFrostMark = Color.a * 255.0;
    AwFrostPos = Position + ChunkOffset;
    AwFrostOpen = vec4(smoothstep(216.0, 238.0, float(UV2.y)), step(236.5, awFrostMark) * step(awFrostMark, 253.5),
            1.0 - step(0.5, abs(awFrostMark - 248.0)), float(UV2.y) / 240.0);
    AwFrostLight = minecraft_sample_lightmap(Sampler2, UV2).rgb;
    AwFrostWarm = float(UV2.x) / 240.0;
}
