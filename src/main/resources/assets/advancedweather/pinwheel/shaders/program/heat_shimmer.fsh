uniform sampler2D DiffuseSampler0;

uniform float AWTime;
uniform float Intensity;
uniform float Temperature;

in vec2 texCoord;
out vec4 fragColor;

// noise
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
    float intensity      = clamp(Intensity, 0.0, 1.0);
    float tempFactor     = clamp((Temperature - 40.0) / (148.0 - 40.0), 0.0, 1.0);
    float combinedFactor = intensity * tempFactor;

    float heightFactor = 1.0 - texCoord.y;

    float waveX1 = sin(texCoord.y * 12.5  + AWTime * 1.618) * 0.0055;
    float waveX2 = sin(texCoord.y * 27.0  + AWTime * 2.9)   * 0.0022;
    float waveY1 = cos(texCoord.x * 16.3  + AWTime * 2.314) * 0.0035;
    float waveY2 = cos(texCoord.x * 31.0  + AWTime * 1.7)   * 0.0015;

    float distortNoise = noise(texCoord * 6.0 + vec2(0.0, AWTime * 0.15)) * 2.0 - 1.0;

    vec2 distortion = vec2(
        (waveX1 + waveX2) * 1.3 + distortNoise * 0.0018,
        (waveY1 + waveY2) * 0.5 + distortNoise * 0.0010
    ) * combinedFactor * heightFactor;

    vec2 distortedCoord = clamp(texCoord + distortion, 0.001, 0.999);
    vec4 color = texture(DiffuseSampler0, distortedCoord);
    vec3 base  = color.rgb;

    float heatWave1 = sin(texCoord.y * 7.0   - AWTime * 1.1 + texCoord.x * 2.5) * 0.5 + 0.5;
    float heatWave2 = sin(texCoord.y * 12.0  - AWTime * 0.75 + texCoord.x * 4.5) * 0.5 + 0.5;
    float heatWave3 = sin(texCoord.y * 19.0  - AWTime * 1.4  - texCoord.x * 3.0) * 0.5 + 0.5;

    float patchNoise = noise(texCoord * 3.5 - vec2(0.0, AWTime * 0.25));

    float heatMask = pow(heatWave1 * heatWave2 * heatWave3, 0.45);
    heatMask = mix(heatMask, heatMask * patchNoise * 1.6, 0.5);
    heatMask = clamp(heatMask, 0.0, 1.0);

    float bottomBias = pow(1.0 - texCoord.y, 1.3);
    heatMask *= (0.35 + bottomBias * 0.65);

    vec3 heatColorLow  = vec3(1.0, 0.72, 0.28);
    vec3 heatColorHigh = vec3(1.0, 0.18, 0.04);
    vec3 heatColor     = mix(heatColorLow, heatColorHigh, tempFactor);

    float luma = dot(base, vec3(0.299, 0.587, 0.114));

    vec3 tinted = (1.0 - luma) * base * heatColor
                + luma * (1.0 - (1.0 - base) * (1.0 - heatColor));

    float tintStrength = combinedFactor * (0.22 + heatMask * 0.58);
    color.rgb = mix(base, tinted, tintStrength);

    color.rgb *= 1.0 + combinedFactor * heatMask * 0.16;

    color.r += combinedFactor * heatMask * 0.07;
    color.g += combinedFactor * heatMask * 0.02;

    float vignetteDist = length(texCoord - vec2(0.5, 0.45));
    float vignette = smoothstep(0.35, 0.75, vignetteDist);
    float extremeHeat = smoothstep(0.85, 1.0, tempFactor);
    color.rgb = mix(color.rgb, color.rgb * vec3(1.08, 0.85, 0.78), vignette * extremeHeat * intensity * 0.35);

    float desat = combinedFactor * heatMask * 0.08;
    float gray  = dot(color.rgb, vec3(0.333));
    color.rgb = mix(color.rgb, vec3(gray), desat * 0.3);

    fragColor = vec4(color.rgb, color.a);
}