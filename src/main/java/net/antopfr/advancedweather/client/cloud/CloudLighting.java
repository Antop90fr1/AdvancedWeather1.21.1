package net.antopfr.advancedweather.client.cloud;

import net.antopfr.advancedweather.client.event.LightningFlashHandler;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

record CloudLighting(Vec3 lightDir, Vec3 lightColor, Vec3 ambientTop, Vec3 ambientBottom, float flash) {

    private static final Vec3 NOON = new Vec3(1.00, 0.97, 0.92);
    private static final Vec3 GOLDEN = new Vec3(1.00, 0.56, 0.28);
    private static final Vec3 MOON = new Vec3(0.30, 0.35, 0.50);
    private static final Vec3 BOUNCE = new Vec3(0.05, 0.045, 0.04);

    static CloudLighting compute(ClientLevel level, Vec3 camera, float partialTick) {
        double angle = level.getTimeOfDay(partialTick) * Math.PI * 2.0;
        Vec3 sun = new Vec3(-Math.sin(angle), Math.cos(angle), 0.0);
        float elevation = (float) sun.y;

        float day = smoothstep(-0.12f, 0.18f, elevation);
        float horizon = 1f - smoothstep(0.0f, 0.35f, Math.abs(elevation));
        Vec3 sunColor = lerp(NOON, GOLDEN, horizon).scale(day);

        float rain = level.getRainLevel(partialTick);
        float thunder = level.getThunderLevel(partialTick);
        boolean veiled = WeatherDefinitions.get(ClientWeatherState.getCurrentWeather()).hidesSun;
        float direct = (veiled ? 0.35f : 1f) * (1f - 0.4f * rain) * (1f - 0.3f * thunder);

        float night = smoothstep(-0.05f, -0.25f, elevation);
        Vec3 moonColor = MOON.scale(night * (0.35f + 0.65f * level.getMoonBrightness()));

        Vec3 lightDir = elevation > -0.05f ? sun : sun.scale(-1);
        Vec3 lightColor = sunColor.add(moonColor).scale(direct);

        Vec3 sky = level.getSkyColor(camera, partialTick);
        double luminance = sky.x * 0.3 + sky.y * 0.59 + sky.z * 0.11;
        Vec3 grey = new Vec3(luminance, luminance, luminance);
        Vec3 top = lerp(grey, sky, 0.55).scale(1.15).add(0.03, 0.03, 0.04);
        top = lerp(top, sunColor.scale(0.6), horizon * day * 0.35);
        Vec3 bottom = top.scale(0.5).add(BOUNCE.scale(day));

        return new CloudLighting(lightDir, lightColor, top, bottom, flash(level));
    }

    private static float flash(ClientLevel level) {
        float handler = LightningFlashHandler.flashStrength();
        float vanilla = level.getSkyFlashTime() > 0 ? 0.8f : 0f;
        return Math.max(handler, vanilla);
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return new Vec3(Mth.lerp(t, a.x, b.x), Mth.lerp(t, a.y, b.y), Mth.lerp(t, a.z, b.z));
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }
}
