uniform sampler2D DiffuseSampler0;

uniform float AWTime;
uniform float Intensity;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    float intensity = clamp(Intensity, 0.0, 1.0);
    vec4 base = texture(DiffuseSampler0, texCoord);

    if (intensity < 0.01) {
        fragColor = base;
        return;
    }

    vec3 color = base.rgb;

    float gray = dot(color, vec3(0.333));
    color = mix(color, vec3(gray), intensity * 0.35);

    float grain = hash(floor(texCoord * vec2(400.0, 225.0)) + floor(AWTime * 8.0));
    color += vec3((grain - 0.5) * 0.05 * intensity);

    vec2 centered = texCoord - 0.5;
    float edgeDist = length(centered) * 1.3;
    float edgeMask = smoothstep(0.3, 1.0, edgeDist);
    vec3 ashGray = vec3(0.25, 0.24, 0.23);
    color = mix(color, ashGray, edgeMask * intensity * 0.5);

    color *= 1.0 - intensity * 0.1;

    fragColor = vec4(color, base.a);
}