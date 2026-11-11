
uniform vec4 AwSwayA;
uniform vec4 AwSwayB;
uniform vec4 AwSwayWind;

void tail() {
    float awMark = Color.a * 255.0;
    if (awMark > 236.5 && awMark < 253.5) {
        vec3 awPos = Position + ChunkOffset;
        float awWave = sin(dot(awPos.xz, AwSwayA.xy) + AwSwayA.z) * AwSwayA.w
                + sin(dot(awPos.xz, AwSwayB.xy) + AwSwayB.z) * AwSwayB.w;
        float awFlutter = sin(AwSwayWind.w + (Position.x + Position.z) * 0.3927) * AwSwayA.w * 0.2;
        vec2 awOffset = AwSwayWind.xy * (AwSwayWind.z + awWave)
                + vec2(-AwSwayWind.y, AwSwayWind.x) * awFlutter;
        awPos.xz += awOffset * ((253.0 - awMark) / 16.0);
        gl_Position = ProjMat * ModelViewMat * vec4(awPos, 1.0);
        vertexColor.a = 1.0;
    }
}
