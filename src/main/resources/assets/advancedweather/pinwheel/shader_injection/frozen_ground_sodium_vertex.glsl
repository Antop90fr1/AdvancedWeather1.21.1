
out vec3 AwFrostPos;
out vec2 AwFrostOpen;
out vec3 AwFrostLight;

void tail() {
    float awFrostMark = _vert_color.a * 255.0;
    AwFrostPos = position;
    AwFrostOpen = vec2(smoothstep(216.0, 238.0, _vert_tex_light_coord.y * 256.0), step(236.5, awFrostMark) * step(awFrostMark, 253.5));
    AwFrostLight = texture(u_LightTex, _vert_tex_light_coord).rgb;
}
