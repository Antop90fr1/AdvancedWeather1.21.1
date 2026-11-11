
uniform vec4 AwSwayA;
uniform vec4 AwSwayB;
uniform vec4 AwSwayWind;

void tail() {
    float awMark = _vert_color.a * 255.0;
    if (awMark > 236.5 && awMark < 253.5) {
        float awWave = sin(dot(position.xz, AwSwayA.xy) + AwSwayA.z) * AwSwayA.w
                + sin(dot(position.xz, AwSwayB.xy) + AwSwayB.z) * AwSwayB.w;
        float awFlutter = sin(AwSwayWind.w + (_vert_position.x + _vert_position.z) * 0.3927) * AwSwayA.w * 0.2;
        vec2 awOffset = AwSwayWind.xy * (AwSwayWind.z + awWave)
                + vec2(-AwSwayWind.y, AwSwayWind.x) * awFlutter;
        position.xz += awOffset * ((253.0 - awMark) / 16.0);
        gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);
        v_Color.a = 1.0;
    }
}
