
out vec3 AwFrostPos;
out vec4 AwFrostOpen;
out float AwFrostWarm;
out vec3 AwFrostLight;

void tail() {
    float awFrostMark = _vert_color.a * 255.0;
    AwFrostPos = position;
    AwFrostOpen = vec4(smoothstep(216.0, 238.0, _vert_tex_light_coord.y * 256.0), step(236.5, awFrostMark) * step(awFrostMark, 253.5),
            1.0 - step(0.5, abs(awFrostMark - 248.0)), _vert_tex_light_coord.y * 256.0 / 240.0);
    AwFrostLight = texture(u_LightTex, _vert_tex_light_coord).rgb;
    AwFrostWarm = _vert_tex_light_coord.x * 256.0 / 240.0;
}
