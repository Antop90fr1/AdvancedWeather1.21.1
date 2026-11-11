
out vec3 AwFrostPos;
out vec2 AwFrostOpen;
out vec3 AwFrostLight;

void tail() {
    float awFrostMark = Color.a * 255.0;
    AwFrostPos = Position + ChunkOffset;
    AwFrostOpen = vec2(smoothstep(216.0, 238.0, float(UV2.y)), step(236.5, awFrostMark) * step(awFrostMark, 253.5));
    AwFrostLight = minecraft_sample_lightmap(Sampler2, UV2).rgb;
}
