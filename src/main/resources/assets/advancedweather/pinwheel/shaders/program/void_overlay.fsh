uniform sampler2D DiffuseSampler0;

uniform float Intensity;
uniform float Surge;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float intensity = clamp(Intensity, 0.0, 1.0);
    vec4 base = texture(DiffuseSampler0, texCoord);

    if (intensity < 0.01) {
        fragColor = base;
        return;
    }

    float amount = intensity * (0.25 + 0.75 * clamp(Surge, 0.0, 1.0));

    vec3 color = base.rgb;
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(color, vec3(luma), amount * 0.55);

    float edge = smoothstep(0.15, 0.95, length(texCoord - 0.5) * 1.4);
    color *= 1.0 - amount * (0.12 + 0.5 * edge);
    color += vec3(0.025, 0.0, 0.06) * amount * edge;

    fragColor = vec4(color, base.a);
}
