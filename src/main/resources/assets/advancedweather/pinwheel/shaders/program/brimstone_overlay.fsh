uniform sampler2D DiffuseSampler0;

uniform float AWTime;
uniform float Intensity;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(cell), hash(cell + vec2(1.0, 0.0)), f.x),
               mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0, 1.0)), f.x), f.y);
}

void main() {
    float intensity = clamp(Intensity, 0.0, 1.0);
    vec4 base = texture(DiffuseSampler0, texCoord);

    if (intensity < 0.01) {
        fragColor = base;
        return;
    }

    vec2 uv = texCoord * vec2(1.78, 1.0);

    vec2 wobble = vec2(noise(uv * 6.0 + vec2(0.0, AWTime * 0.6)),
                       noise(uv * 6.0 + vec2(13.0, -AWTime * 0.5))) - 0.5;
    vec3 color = texture(DiffuseSampler0, texCoord + wobble * 0.004 * intensity).rgb;

    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(color, luma * vec3(1.15, 0.92, 0.35), intensity * 0.45);
    color += vec3(0.10, 0.075, 0.01) * intensity * 0.3;

    float edge = smoothstep(0.25, 0.95, length(texCoord - 0.5) * 1.35);
    float fumes = noise(uv * 3.0 + vec2(AWTime * 0.15, -AWTime * 0.08)) * 0.6
                + noise(uv * 7.0 - vec2(AWTime * 0.2, 0.0)) * 0.4;
    color = mix(color, vec3(0.30, 0.24, 0.08), edge * (0.35 + 0.35 * fumes) * intensity);

    float grain = hash(floor(texCoord * vec2(400.0, 225.0)) + floor(AWTime * 8.0));
    color += vec3((grain - 0.5) * 0.04 * intensity);

    fragColor = vec4(color, base.a);
}
