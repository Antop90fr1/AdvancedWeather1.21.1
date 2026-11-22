uniform sampler2D DiffuseSampler0;

uniform float AWTime;
uniform float Intensity;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
    mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
    mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
    u.y
    );
}

void main() {
    float intensity = clamp(Intensity, 0.0, 1.0);

    if (intensity < 0.01) {
        fragColor = texture(DiffuseSampler0, texCoord);
        return;
    }

    vec2 centered = texCoord - 0.5;
    float dist = length(centered);

    float fractureNoise = noise(centered * 6.0 + vec2(AWTime * 0.15, -AWTime * 0.1));
    float fractureMask = smoothstep(0.4, 0.85, fractureNoise);

    float aberration = (0.006 + dist * 0.01) * intensity * (0.3 + fractureMask * 0.7);

    vec2 dir = normalize(centered + 0.0001);

    vec2 redOffset   = dir * aberration * 1.0;
    vec2 greenOffset = dir * aberration * -0.3;
    vec2 blueOffset  = dir * aberration * -1.0;

    float r = texture(DiffuseSampler0, texCoord + redOffset).r;
    float g = texture(DiffuseSampler0, texCoord + greenOffset).g;
    float b = texture(DiffuseSampler0, texCoord + blueOffset).b;
    float a = texture(DiffuseSampler0, texCoord).a;

    vec3 color = vec3(r, g, b);

    vec3 voidTint = vec3(0.55, 0.25, 0.85);
    float tintStrength = fractureMask * intensity * 0.12;
    color = mix(color, color * voidTint + voidTint * 0.05, tintStrength);

    float flicker = 1.0 + sin(AWTime * 8.0 + fractureNoise * 20.0) * 0.03 * fractureMask * intensity;
    color *= flicker;

    fragColor = vec4(color, a);
}