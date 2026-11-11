
uniform vec4 AwFrost;
uniform vec4 AwSnow;
uniform vec4 AwSnowTint;
uniform vec3 AwSnowMost;
uniform vec4 AwSnowSprite;
uniform vec4 AwPowderSprite;
uniform vec3 AwFrostCam;

in vec3 AwFrostPos;
in vec2 AwFrostOpen;
in vec3 AwFrostLight;

float awFrostHash(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

float awFrostPatch(vec3 cell, float period) {
    vec3 corner = floor(cell);
    vec3 f = fract(cell);
    f = f * f * (3.0 - 2.0 * f);
    float x00 = mix(awFrostHash(mod(corner, period)), awFrostHash(mod(corner + vec3(1.0, 0.0, 0.0), period)), f.x);
    float x10 = mix(awFrostHash(mod(corner + vec3(0.0, 1.0, 0.0), period)), awFrostHash(mod(corner + vec3(1.0, 1.0, 0.0), period)), f.x);
    float x01 = mix(awFrostHash(mod(corner + vec3(0.0, 0.0, 1.0), period)), awFrostHash(mod(corner + vec3(1.0, 0.0, 1.0), period)), f.x);
    float x11 = mix(awFrostHash(mod(corner + vec3(0.0, 1.0, 1.0), period)), awFrostHash(mod(corner + vec3(1.0, 1.0, 1.0), period)), f.x);
    return mix(mix(x00, x10, f.y), mix(x01, x11, f.y), f.z);
}

float awFrostInside(vec2 uv, vec4 sprite) {
    return step(sprite.x, uv.x) * step(uv.x, sprite.z) * step(sprite.y, uv.y) * step(uv.y, sprite.w);
}

void tail() {
    if (AwFrost.x > 0.0 || AwSnow.x > 0.0) {
        vec3 awAcross = dFdx(AwFrostPos);
        vec3 awDown = dFdy(AwFrostPos);
        vec3 awCross = cross(awAcross, awDown);
        float awCrossLength = length(awCross);
        vec3 awNormal = vec3(0.0, 1.0, 0.0);
        if (awCrossLength > 0.0) {
            awNormal = awCross / awCrossLength;
        }
        float awSpread = length(fwidth(AwFrostPos));
        float awDetail = clamp(1.6 - awSpread * 16.0, 0.0, 1.0);
        float awTop = smoothstep(0.35, 0.75, awNormal.y);
        float awSolid = step(0.99, color.a);

        vec3 awTexel = mod(floor((AwFrostPos + AwFrostCam - awNormal * 0.02) * 16.0), 4096.0);
        float awGrain = awFrostHash(awTexel);
        float awBlotch = awFrostPatch(awTexel / 16.0, 256.0);
        float awPatch = mix(0.5, awBlotch, awDetail);
        vec3 awSurface = color.rgb;

        float awOpen = AwFrost.x * AwFrostOpen.x * max(mix(AwFrost.y, 1.0, awTop), AwFrostOpen.y * AwFrost.w) * awSolid;
        float awCover = clamp(awOpen * 1.8 - awPatch * 0.9, 0.0, 1.0) * 0.9;
        float awIce = mix(awCover, step(awGrain, awCover), awDetail);

        float awLight = max(v_Color.r, max(v_Color.g, v_Color.b));
        float awLuma = dot(awSurface, vec3(0.299, 0.587, 0.114));

        awSurface = mix(awSurface, vec3(awLuma) * vec3(0.82, 0.96, 1.15), 0.4 * awOpen);
        float awThick = mix(0.34, 0.68, mix(0.55, awFrostHash(awTexel + 19.0), awDetail));
        awSurface = mix(awSurface, vec3(0.70, 0.86, 1.0) * awLight, awThick * awIce);

        float awGlint = step(0.975, awFrostHash(awTexel + 71.0)) * awIce * awDetail
                * pow(max(sin(AwFrost.z + awGrain * 6.2832 + dot(AwFrostPos, vec3(1.7, 2.3, 1.1))), 0.0), 10.0);
        float awGraze = pow(1.0 - abs(dot(awNormal, normalize(AwFrostPos))), 4.0) * awOpen;
        awSurface += awLight * (vec3(0.75, 0.85, 1.0) * 0.7 * awGlint + vec3(0.5, 0.65, 0.85) * 0.3 * awGraze);

        float awSnowy = max(awFrostInside(v_TexCoord, AwSnowSprite), awFrostInside(v_TexCoord, AwPowderSprite));
        float awSnowFall = AwSnow.x * AwFrostOpen.x * (1.0 - awSnowy * (1.0 - AwSnow.z)) * awSolid;
        float awSnowBlotch = mix(0.5, awBlotch, clamp(2.0 - awSpread * 3.0, 0.0, 1.0));
        float awSnowSwell = mix(0.5, awFrostPatch(awTexel / 64.0, 64.0), clamp(2.0 - awSpread, 0.0, 1.0));
        float awSnowLie = 0.45 * awSnowBlotch + 0.55 * awSnowSwell;

        float awDustFall = awSnowFall * max(mix(AwSnow.y, 1.0, awTop), AwFrostOpen.y * AwSnow.w);
        float awDustCover = clamp(awDustFall * 2.2 - awSnowBlotch * 1.2, 0.0, 1.0)
                * (1.0 - 0.3 * mix(0.4, 1.0, awDetail) * smoothstep(0.66, 0.8, awSnowLie)) * AwSnowMost.x;
        float awDust = mix(awDustCover, step(awFrostHash(awTexel + 37.0), awDustCover), awDetail);

        float awDeepLeft = 1.0 - awSnowFall * awTop;
        float awDeepCover = clamp((0.68 * (1.0 - awDeepLeft * awDeepLeft) - 0.13 - awSnowLie) * 4.0 + 0.5, 0.0, 1.0);
        awDeepCover = smoothstep(0.25, 0.75, awDeepCover);
        float awDeep = mix(awDeepCover, step(awFrostHash(awTexel + 61.0), awDeepCover), awDetail) * AwSnowMost.y;

        float awFlat = step(max(abs(awNormal.x), abs(awNormal.z)), abs(awNormal.y));
        float awAlongZ = (1.0 - awFlat) * step(abs(awNormal.z), abs(awNormal.x));
        vec3 awWithin = mod(awTexel, 16.0);
        vec2 awSnowTexel = mix(mix(vec2(awWithin.x, 15.0 - awWithin.y), vec2(awWithin.z, 15.0 - awWithin.y), awAlongZ), awWithin.xz, awFlat);
        vec2 awSnowAcross = mix(mix(vec2(awAcross.x, -awAcross.y), vec2(awAcross.z, -awAcross.y), awAlongZ), awAcross.xz, awFlat);
        vec2 awSnowDown = mix(mix(vec2(awDown.x, -awDown.y), vec2(awDown.z, -awDown.y), awAlongZ), awDown.xz, awFlat);
        float awTurn = floor(awFrostHash(floor(awTexel / 16.0) + 11.0) * 4.0);
        vec2 awSnowTurned = mix(awSnowTexel, vec2(15.0 - awSnowTexel.y, awSnowTexel.x), step(0.5, mod(awTurn, 2.0)));
        awSnowTurned = mix(awSnowTurned, vec2(15.0) - awSnowTurned, step(1.5, awTurn));
        vec2 awSnowSize = AwSnowSprite.zw - AwSnowSprite.xy;
        vec3 awSnowTexture = textureGrad(u_BlockTex, AwSnowSprite.xy + awSnowSize * (awSnowTurned + 0.5) / 16.0,
                awSnowAcross * awSnowSize, awSnowDown * awSnowSize).rgb;

        float awShade = 0.6 + 0.2 * abs(awNormal.z) + (0.4 - 0.2 * abs(awNormal.z)) * clamp(awNormal.y, 0.0, 1.0);
        awShade = mix(awShade, 1.0, AwFrostOpen.y);
        float awSnowGlint = step(0.988, awFrostHash(awTexel + 89.0)) * awDetail
                * pow(max(sin(AwFrost.z + awGrain * 6.2832 + dot(AwFrostPos, vec3(1.3, 1.9, 0.9))), 0.0), 12.0);
        vec3 awDustLook = AwSnowTint.rgb * (0.95 + 0.05 * awSnowBlotch + 0.04 * awDetail * (2.0 * awFrostHash(awTexel + 53.0) - 1.0));
        vec3 awDeepLook = awSnowTexture * (0.95 + 0.04 * awSnowBlotch + 0.04 * awSnowSwell);
        vec3 awSnowLit = AwFrostLight * awShade;
        awSurface = mix(awSurface, (awDustLook + AwSnowTint.a * awSnowGlint) * awSnowLit, awDust);
        awSurface = mix(awSurface, (awDeepLook + AwSnowTint.a * awSnowGlint) * awSnowLit, awDeep);

        color.rgb = awSurface;
        fragColor = _linearFog(color, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
    }
}
