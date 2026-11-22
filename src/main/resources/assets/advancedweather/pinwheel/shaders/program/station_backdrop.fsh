#version 150

uniform float AWTime;
uniform float PanelAspect;
uniform vec3 WeatherTint;
uniform float WeatherEnergy;
uniform float CloudAmount;
uniform float RainLevel;
uniform float SnowAmount;
uniform float HailAmount;
uniform float WindLines;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float hash(vec2 p) {
  return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
  vec2 i = floor(p);
  vec2 f = fract(p);
  f = f * f * (3.0 - 2.0 * f);
  return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x),
  mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}

float fbm(vec2 p) {
  float v = 0.0;
  float a = 0.5;
  for (int i = 0; i < 3; i++) {
    v += a * noise(p);
    p = p * 2.1 + vec2(13.7, 7.3);
    a *= 0.5;
  }
  return v;
}

float flakePixel(vec2 p, float variant) {
  if (p.x < 0.0 || p.x >= 1.0 || p.y < 0.0 || p.y >= 1.0) return 0.0;
  int px = int(p.x * 3.0);
  int py = int(p.y * 3.0);

  int row;
  if (variant < 0.5) {
    // A :  . X .
    //      X . X
    //      . X .
    int rowsA[3] = int[3](2, 5, 2);   // 010 / 101 / 010
    row = rowsA[py];
  } else {
    // B :  X . X
    //      . X .
    //      X . X
    int rowsB[3] = int[3](5, 2, 5);   // 101 / 010 / 101
    row = rowsB[py];
  }
  return float((row >> (2 - px)) & 1);
}

float hailPixel(vec2 p) {
  if (p.x < 0.0 || p.x >= 1.0 || p.y < 0.0 || p.y >= 1.0) return 0.0;
  int px = int(p.x * 4.0);
  int py = int(p.y * 4.0);
  int rows[4] = int[4](6, 15, 15, 6); // 0110 / 1111 / 1111 / 0110
  return float((rows[py] >> (3 - px)) & 1);
}

void main() {
  vec2 uv = texCoord0;
  vec2 auv = vec2(uv.x * PanelAspect, uv.y);

  float speed = 0.15 + WeatherEnergy * 0.35;
  float n1 = fbm(uv * 3.0 + vec2(AWTime * speed, AWTime * speed * 0.3));
  float n2 = fbm(uv * 6.0 - vec2(AWTime * speed * 1.7, 0.0) + 42.0);

  float clouds = n1 * 0.65 + n2 * 0.35;

  float contrast = mix(0.35, 0.9, WeatherEnergy);
  clouds = smoothstep(0.5 - contrast * 0.4, 0.5 + contrast * 0.4, clouds);

  clouds *= CloudAmount;

  float flash = 0.0;
  if (WeatherEnergy > 0.7) {
    float t = fract(AWTime * 0.11);
    flash = smoothstep(0.965, 0.975, t) * (1.0 - smoothstep(0.975, 1.0, t));
    flash *= hash(vec2(floor(AWTime * 0.11), 3.7)) > 0.5 ? 1.0 : 0.0;
  }

  float clearBoost = (1.0 - CloudAmount) * 0.18;
  float skyGradient = mix(0.05, 0.0, uv.y) * (1.0 - CloudAmount);
  vec3 base = WeatherTint * (0.22 + clearBoost + skyGradient);
  vec3 cloudCol = WeatherTint * mix(0.30, 0.55, clouds);
  vec3 color = mix(base, cloudCol, clouds) + vec3(flash * 0.35);

  if (RainLevel > 0.01) {
    float slant = uv.y * 0.15;

    float colPos1 = (uv.x + slant) * 90.0;
    float col1 = floor(colPos1);
    float active1 = step(0.75, hash(vec2(col1, 0.0)));

    float thin1 = smoothstep(0.30, 0.42, fract(colPos1)) * smoothstep(0.70, 0.58, fract(colPos1));
    float fall1 = fract(uv.y * 2.0 - AWTime * 6.0 + hash(vec2(col1, 7.0)) * 19.0);
    float drop1 = active1 * thin1 * smoothstep(0.0, 0.06, fall1) * smoothstep(0.40, 0.34, fall1);

    float colPos2 = (uv.x + slant * 1.5) * 140.0;
    float col2 = floor(colPos2);
    float active2 = step(0.8, hash(vec2(col2, 3.0)));
    float thin2 = smoothstep(0.35, 0.45, fract(colPos2)) * smoothstep(0.65, 0.55, fract(colPos2));
    float fall2 = fract(uv.y * 2.5 - AWTime * 3.8 + hash(vec2(col2, 11.0)) * 23.0);
    float drop2 = active2 * thin2 * smoothstep(0.0, 0.05, fall2) * smoothstep(0.32, 0.27, fall2);

    float rain = (drop1 * 0.55 + drop2 * 0.3) * RainLevel;
    color = mix(color, WeatherTint * 0.85 + vec3(0.15), rain * 0.5);
  }

  // snow
  if (SnowAmount > 0.01) {
    float snow = 0.0;

    float fallSpeed = 0.10 + WeatherEnergy * 0.75;
    float driftMag = 0.10 + WeatherEnergy * 0.14;

    float scale = 16.0;

    float sway = sin(AWTime * 0.35) * 0.06;
    vec2 grid = vec2(auv.x + sway * auv.y, auv.y - AWTime * fallSpeed) * scale;
    vec2 cell = floor(grid);
    vec2 f = fract(grid);

    float has = step(0.62, hash(cell));
    if (has > 0.5) {
      float seed = hash(cell + 3.1);
      vec2 flakePos = vec2(seed, hash(cell + 9.7)) * 0.4 + 0.15;
      flakePos.x += (sin(AWTime * (0.8 + seed * 1.4) + seed * 6.28)
      + 0.5 * sin(AWTime * (1.9 + seed * 0.7) + seed * 12.6)) * driftMag;

      float variant = step(0.5, hash(cell + 27.3));
      vec2 local = (f - flakePos) / 0.45;
      snow = flakePixel(local, variant);
    }
    color = mix(color, vec3(0.95, 0.96, 1.0), min(snow, 1.0) * SnowAmount * 0.8);
  }

  // hail
  if (HailAmount > 0.01) {
    float scale = 20.0;
    vec2 grid = vec2(auv.x, auv.y - AWTime * 1.5) * scale;
    vec2 cell = floor(grid);
    vec2 f = fract(grid);

    float hailPx = 0.0;
    float has = step(0.68, hash(cell + 23.0));
    if (has > 0.5) {
      vec2 stonePos = vec2(hash(cell + 5.3), hash(cell + 13.1)) * 0.5 + 0.2;
      vec2 local = (f - stonePos) / 0.35;
      hailPx = hailPixel(local);
    }
    color = mix(color, vec3(0.85, 0.85, 0.72), hailPx * HailAmount * 0.75);
  }

  if (WindLines > 0.01) {
    float wind = 0.0;

    float rowScale = 24.0;
    float row = floor(uv.y * rowScale);
    float rowSeed = hash(vec2(row, 41.0));

    float activeCycle = hash(vec2(row, floor(AWTime * 0.25)));
    if (activeCycle > 0.7) {
      float speed = 1.2 + rowSeed * 1.4;

      float xPos = fract(uv.x * 0.5 - AWTime * speed * 0.3 + rowSeed * 7.0);

      float line = smoothstep(0.0, 0.25, xPos) * smoothstep(0.32, 0.30, xPos);

      float yInRow = fract(uv.y * rowScale + sin(uv.x * 9.0 + AWTime * 2.0 + rowSeed * 6.28) * 0.08);
      float thin = smoothstep(0.35, 0.48, yInRow) * smoothstep(0.65, 0.52, yInRow);

      wind = line * thin;
    }
    color = mix(color, WeatherTint * 0.7 + vec3(0.25), wind * WindLines * 0.45);
  }

  vec2 c = uv - 0.5;
  float vignette = 1.0 - dot(c, c) * 0.8;
  color *= max(vignette, 0.55);

  fragColor = vec4(color, vertexColor.a);
}