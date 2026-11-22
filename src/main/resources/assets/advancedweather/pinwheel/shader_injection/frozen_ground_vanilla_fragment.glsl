
uniform vec4 AwFrost;
uniform vec4 AwSnow;
uniform vec4 AwSnowTint;
uniform vec3 AwSnowMost;
uniform vec4 AwSnowEdge;
uniform vec4 AwSnowSprite;
uniform vec4 AwPowderSprite;
uniform vec4 AwSnowSideSprite;
uniform vec4 AwWaterSprite;
uniform vec4 AwWaterFlowSprite;
uniform vec3 AwFrostCam;
uniform vec4 AwFrostShip;
uniform sampler2D AwRoofMap;
uniform vec4 AwRoofCam;
uniform vec3 AwFrostDetail;
uniform vec3 AwSnowWind;
uniform vec3 AwSnowColumns;
uniform float AwSnowLeaves;

in vec3 AwFrostPos;
in vec4 AwFrostOpen;
in float AwFrostWarm;
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
        float awDetail = clamp((AwFrostDetail.x - length(AwFrostPos)) / max(AwFrostDetail.x * 0.4, 1.0), 0.0, 1.0);
        float awTop = smoothstep(0.35, 0.75, awNormal.y);
        float awUnder = 1.0 - smoothstep(-0.75, -0.35, awNormal.y);

        vec3 awGrid = AwFrostShip.w > 0.5 ? AwFrostPos - AwFrostShip.xyz : AwFrostPos + AwFrostCam;
        vec3 awTexel = mod(floor((awGrid - awNormal * 0.02) * 16.0), 4096.0);
        vec2 awAtlasSize = vec2(textureSize(Sampler0, 0));
        float awSky = AwFrostOpen.x;
        float awTopOpen = AwFrostOpen.y;
        float awUnderLayer = 0.0;
        float awDrift = 0.0;
        float awLying = mix(AwSnow.x, AwSnowColumns.y, AwSnowColumns.x);
        if (AwRoofCam.w > 0.5 && AwFrostShip.w < 0.5) {
            float awMapped = step(max(abs(AwFrostPos.x), abs(AwFrostPos.z)), 120.0);
            awSky = smoothstep(mix(0.75, 0.95, awMapped), mix(0.9, 0.99, awMapped), AwFrostOpen.w) * mix(0.6, 1.0, AwFrostOpen.x);
            float awHeight = awGrid.y + AwRoofCam.y * 256.0;
            vec2 awColumn = floor(awGrid.xz + awNormal.xz * 0.5);
            vec4 awRoof = texelFetch(AwRoofMap, ivec2(mod(awColumn, 256.0)), 0);
            vec2 awWindow = floor(awColumn / 256.0) + AwRoofCam.xz;
            if (abs(awRoof.g - awWindow.x) + abs(awRoof.b - awWindow.y) < 0.5) {
                awSky = step(awRoof.r, awHeight + 0.6) * mix(0.6, 1.0, AwFrostOpen.x);
                float awWindLength = length(AwSnowWind.xy);
                if (awSky < 0.5 && awWindLength > 0.01) {
                    vec2 awUpwind = AwSnowWind.xy / awWindLength;
                    vec2 awStart = awGrid.xz + awNormal.xz * 0.5;
                    float awLevel = floor(awHeight + 0.02 * awNormal.y);
                    for (int awStep = 1; awStep <= 3; awStep++) {
                        vec2 awAhead = floor(awStart + awUpwind * float(awStep));
                        ivec2 awAt = ivec2(mod(awAhead, 256.0));
                        vec4 awAheadRoof = texelFetch(AwRoofMap, awAt, 0);
                        vec2 awAheadWindow = floor(awAhead / 256.0) + AwRoofCam.xz;
                        if (abs(awAheadRoof.g - awAheadWindow.x) + abs(awAheadRoof.b - awAheadWindow.y) > 0.5) break;
                        if (awAheadRoof.r <= awHeight + 0.6) {
                            awDrift = 1.0 - float(awStep - 1) / 3.0;
                            break;
                        }
                        vec4 awAheadGap = texelFetch(AwRoofMap, awAt + ivec2(256, 0), 0);
                        if (awAheadGap.y > awHeight + 0.6 || awAheadGap.x < awLevel + 1.0) break;
                    }
                }
            }
            vec2 awHome = floor(awGrid.xz - awNormal.xz * 0.5);
            vec4 awHomeRoof = texelFetch(AwRoofMap, ivec2(mod(awHome, 256.0)), 0);
            vec2 awHomeWindow = floor(awHome / 256.0) + AwRoofCam.xz;
            if (abs(awHomeRoof.g - awHomeWindow.x) + abs(awHomeRoof.b - awHomeWindow.y) < 0.5) {
                float awLayered = step(2048.0, awHomeRoof.a);
                float awHomeTop = awHomeRoof.a - 4096.0 * awLayered;
                float awHomeRest = mix(awHomeRoof.r, awHomeTop, AwFrostOpen.z);
                awTopOpen = step(awHomeRest, floor(awHeight) + 1.25);
                awUnderLayer = awLayered * step(abs(awHomeRest - awHomeTop), 0.5) * step(abs(awHomeTop - floor(awHeight) - 1.0), 0.5);
                if (AwSnowColumns.x > 0.5) {
                    vec4 awHomeGap = texelFetch(AwRoofMap, ivec2(mod(awHome, 256.0)) + ivec2(256, 0), 0);
                    if (awHomeGap.w > -1.0e8) awLying = clamp(awHomeGap.z + awHomeGap.w * AwSnowColumns.z, 0.0, 1.0);
                }
            }
        }
        float awWarmth = (1.0 - smoothstep(0.74, 0.8, AwFrostWarm))
                * (1.0 - max(awFrostInside(texCoord0, AwWaterSprite), awFrostInside(texCoord0, AwWaterFlowSprite)));
        awSky *= awWarmth;
        awDrift *= awWarmth;
        float awGrain = awFrostHash(awTexel);
        float awBlotch = awFrostPatch(awTexel / 16.0, 256.0);
        float awPatch = mix(0.5, awBlotch, awDetail);
        vec3 awSurface = color.rgb;

        float awOpen = AwFrost.x * awSky * max(mix(AwFrost.y, 1.0, awTop), AwFrostOpen.y * AwFrost.w);
        float awCover = clamp(awOpen * 1.8 - awPatch * 0.9, 0.0, 1.0) * 0.9;
        float awIce = mix(awCover, step(awGrain, awCover), awDetail);

        float awLight = max(vertexColor.r, max(vertexColor.g, vertexColor.b));
        float awLuma = dot(awSurface, vec3(0.299, 0.587, 0.114));

        awSurface = mix(awSurface, vec3(awLuma) * vec3(0.82, 0.96, 1.15), 0.4 * awOpen);
        float awThick = mix(0.34, 0.68, mix(0.55, awFrostHash(awTexel + 19.0), awDetail));
        awSurface = mix(awSurface, vec3(0.70, 0.86, 1.0) * awLight, awThick * awIce);

        float awGlint = step(0.975, awFrostHash(awTexel + 71.0)) * awIce * awDetail
                * pow(max(sin(AwFrost.z + awGrain * 6.2832 + dot(AwFrostPos, vec3(1.7, 2.3, 1.1))), 0.0), 10.0);
        float awGraze = pow(1.0 - abs(dot(awNormal, normalize(AwFrostPos))), 4.0) * awOpen;
        awSurface += awLight * (vec3(0.75, 0.85, 1.0) * 0.7 * awGlint + vec3(0.5, 0.65, 0.85) * 0.3 * awGraze);

        float awSnowy = max(max(awFrostInside(texCoord0, AwSnowSprite), awFrostInside(texCoord0, AwPowderSprite)),
                awFrostInside(texCoord0, AwSnowSideSprite));
        float awSnowFall = awLying * awSky * (1.0 - awSnowy * (1.0 - AwSnow.z));
        float awSnowBlotch = mix(0.5, awBlotch, clamp(2.0 - awSpread * 3.0, 0.0, 1.0));
        float awSnowSwell = mix(0.5, awFrostPatch(awTexel / 64.0, 64.0), clamp(2.0 - awSpread, 0.0, 1.0));
        float awSnowLie = 0.45 * awSnowBlotch + 0.55 * awSnowSwell;

        float awPlantShare = mix(AwSnow.w, AwSnowMost.z, AwFrostOpen.z);
        float awDustFall = awSnowFall * max(mix(AwSnow.y, 1.0, awTop), AwFrostOpen.y * awPlantShare * (1.0 - awUnder));
        float awDustCover = clamp(awDustFall * 2.2 - awSnowBlotch * 1.2, 0.0, 1.0)
                * (1.0 - 0.3 * mix(0.4, 1.0, awDetail) * smoothstep(0.66, 0.8, awSnowLie)) * AwSnowMost.x;
        float awDust = mix(awDustCover, step(awFrostHash(awTexel + 37.0), awDustCover), awDetail);
        float awPlaster = AwSnowWind.z * clamp(dot(awNormal.xz, AwSnowWind.xy), 0.0, 1.0) * max(awSky, awDrift)
                * (1.0 - awTop) * (1.0 - awUnder);
        float awDrifted = 0.8 * length(AwSnowWind.xy) * awDrift * awTop;
        float awWindShare = (awPlaster + awDrifted) * (1.0 - awSnowy) * (0.4 + 1.2 * awBlotch)
                * clamp(awLying / max(AwSnow.x, 0.001), 0.0, 1.0);
        awDust = max(awDust, mix(awWindShare, step(awFrostHash(awTexel + 97.0), awWindShare), awDetail));

        vec2 awUvAcross = dFdx(texCoord0);
        vec2 awUvDown = dFdy(texCoord0);
        float awAA = dot(awAcross, awAcross);
        float awAD = dot(awAcross, awDown);
        float awDD = dot(awDown, awDown);
        float awGram = awAA * awDD - awAD * awAD;
        vec2 awRise = vec2(0.0, -1.0);
        if (awGram > 1.0e-6 * awAA * awDD) {
            vec2 awSlope = (awDD * awAcross.y - awAD * awDown.y) * awUvAcross + (awAA * awDown.y - awAD * awAcross.y) * awUvDown;
            awRise = abs(awSlope.x) > abs(awSlope.y) ? vec2(sign(awSlope.x), 0.0) : vec2(0.0, sign(awSlope.y));
        }
        vec2 awTexelStep = awRise / awAtlasSize;
        float awRow = mod(awTexel.y, 16.0);
        float awCapFall = awSnowFall * (1.0 - awTop) * (1.0 - awUnder) * AwSnowMost.x;
        float awCapFirst = awCapFall * 0.85 * (1.0 - awUnderLayer);
        float awCapSecond = awCapFall * 0.35 * (1.0 - awUnderLayer);
        float awGapAbove = 0.0;
        float awGapHigher = 0.0;
        float awHoleBelow = 0.0;
        if (awCapFall > 0.0 && AwFrostOpen.y > 0.5) {
            awGapAbove = (1.0 - step(14.5, awRow)) * step(textureLod(Sampler0, texCoord0 + awTexelStep, 0.0).a, 0.5);
            awGapHigher = (1.0 - step(13.5, awRow)) * step(textureLod(Sampler0, texCoord0 + 2.0 * awTexelStep, 0.0).a, 0.5);
            awHoleBelow = step(textureLod(Sampler0, texCoord0 - awTexelStep, 0.0).a, 0.5);
        }
        float awCapHere = awGapAbove * step(awFrostHash(awTexel + 37.0), awCapFirst);
        float awCapLow = (1.0 - awGapAbove) * awGapHigher * step(awFrostHash(awTexel + vec3(0.0, 1.0, 0.0) + 37.0), awCapFirst)
                * step(awFrostHash(awTexel + 43.0), awCapSecond);
        float awDrop = 15.0 - awRow;
        vec3 awStrand = vec3(awTexel.x, floor(awTexel.y / 16.0), awTexel.z);
        float awFringeDepth = floor(awCapFall * (1.5 + 2.5 * awFrostHash(awStrand + 23.0)))
                + 2.0 * step(0.85, awFrostHash(awStrand + 29.0)) * step(0.5, awCapFall);
        float awFringe = awTopOpen * (1.0 - awUnderLayer) * step(awDrop + 0.5, awFringeDepth);
        float awFringeTone = awFringe * (step(awFringeDepth - 1.5, awDrop) * mix(0.5, 1.0, step(1.5, awFringeDepth))
                + 0.5 * step(awFringeDepth - 2.5, awDrop) * (1.0 - step(awFringeDepth - 1.5, awDrop)));
        float awCap = mix(awCapFall * (0.3 * AwFrostOpen.y + 0.15 * awTopOpen) * (1.0 - awUnderLayer), max(awCapHere + awCapLow, awFringe), awDetail);
        vec3 awBelow = awTexel - vec3(0.0, 1.0, 0.0);
        float awBelowSnowy = (1.0 - step(awRow, 0.5)) * (1.0 - awHoleBelow) * step(awFrostHash(awBelow + 43.0), awCapSecond);
        float awEdgeRamp = smoothstep(0.4, 0.85, awSnowFall) * AwSnowEdge.a * awDetail;
        float awEdge = mix(max(awCapLow, 0.5 * awCapHere * (1.0 - awBelowSnowy)), awFringeTone, awFringe) * awEdgeRamp;

        float awDeepLeft = 1.0 - awSnowFall * awTop;
        float awDeepCover = clamp((0.68 * (1.0 - awDeepLeft * awDeepLeft) - 0.13 - awSnowLie) * 4.0 + 0.5, 0.0, 1.0);
        awDeepCover = smoothstep(0.25, 0.75, awDeepCover);
        float awDeep = mix(awDeepCover, step(awFrostHash(awTexel + 61.0), awDeepCover), awDetail) * AwSnowMost.y;
        float awRim = 0.0;
        if (awTop > 0.5 && awEdgeRamp > 0.0) {
            vec3 awWest = awTexel - vec3(1.0, 0.0, 0.0);
            vec3 awNorth = awTexel - vec3(0.0, 0.0, 1.0);
            float awWestSnowy = max(step(awFrostHash(awWest + 37.0), awDustCover), step(awFrostHash(awWest + 61.0), awDeepCover * AwSnowMost.y));
            float awNorthSnowy = max(step(awFrostHash(awNorth + 37.0), awDustCover), step(awFrostHash(awNorth + 61.0), awDeepCover * AwSnowMost.y));
            awRim = 0.5 * (1.0 - min(awWestSnowy, awNorthSnowy)) * awEdgeRamp;
        }

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
        vec3 awSnowTexture = textureGrad(Sampler0, AwSnowSprite.xy + awSnowSize * (awSnowTurned + 0.5) / 16.0,
                awSnowAcross * awSnowSize, awSnowDown * awSnowSize).rgb;

        float awShade = 0.6 + 0.2 * abs(awNormal.z) + (0.4 - 0.2 * abs(awNormal.z)) * clamp(awNormal.y, 0.0, 1.0);
        awShade = mix(awShade, 1.0, AwFrostOpen.y * (1.0 - AwFrostOpen.z));
        float awSnowGlint = step(0.988, awFrostHash(awTexel + 89.0)) * awDetail
                * pow(max(sin(AwFrost.z + awGrain * 6.2832 + dot(AwFrostPos, vec3(1.3, 1.9, 0.9))), 0.0), 12.0);
        vec3 awDustLook = mix(AwSnowTint.rgb * (0.95 + 0.05 * awSnowBlotch + 0.04 * awDetail * (2.0 * awFrostHash(awTexel + 53.0) - 1.0)),
                awSnowTexture * (0.95 + 0.04 * awSnowBlotch), AwSnowEdge.a);
        vec3 awDeepLook = awSnowTexture * (0.95 + 0.04 * awSnowBlotch + 0.04 * awSnowSwell);
        vec3 awSnowLit = AwFrostLight * awShade;
        float awLeafFace = AwSnowLeaves * AwFrostOpen.z * mix(1.0, 0.3, awUnder);
        float awLeafShare = awLeafFace * smoothstep(0.15, 0.6, awSnowFall);
        float awLeafSnow = mix(awLeafShare, step(awFrostHash(awTexel + 113.0), awLeafShare), awDetail);
        float awLeafLuma = dot(texture(Sampler0, texCoord0).rgb, vec3(0.299, 0.587, 0.114));
        vec3 awLeafLook = mix(AwSnowEdge.rgb, vec3(0.96, 0.98, 1.0), smoothstep(0.28, 0.62, awLeafLuma));
        awSurface = mix(awSurface, (awLeafLook + AwSnowTint.a * awSnowGlint) * awSnowLit, awLeafSnow);
        awDust *= 1.0 - awLeafFace;
        awDeep *= 1.0 - awLeafFace;
        awCap *= 1.0 - awLeafFace;
        awSurface = mix(awSurface, (mix(awDustLook, AwSnowEdge.rgb, awRim) + AwSnowTint.a * awSnowGlint) * awSnowLit, awDust);
        awSurface = mix(awSurface, (mix(awDeepLook, AwSnowEdge.rgb, awEdge) + AwSnowTint.a * awSnowGlint) * awSnowLit, awCap);
        awSurface = mix(awSurface, (mix(awDeepLook, AwSnowEdge.rgb, awRim) + AwSnowTint.a * awSnowGlint) * awSnowLit, awDeep);

        color.rgb = awSurface;
        fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    }
}
