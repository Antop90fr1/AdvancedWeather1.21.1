package net.antopfr.advancedweather.compat.atmospherics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.antopfr.advancedweather.client.render.SkyVeil;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.util.AWLogger;
import net.antopfr.advancedweather.util.ModLoaded;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindSample;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public final class AtmosphericsCompat {

    private AtmosphericsCompat() {}

    private static final List<ResourceKey<Biome>> DRY_BIOMES = List.of(
            Biomes.DESERT, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA,
            Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);

    private static final float CHORUS_HAZE_NEAR = 12f;
    private static final float CHORUS_HAZE_FAR = 160f;
    private static final float TWILIGHT_THROUGH_FOG = 0.15f;

    private static int dry;
    private static int visuals;
    private static int skyColor;
    private static boolean skyPass;

    private static boolean dryHere;
    private static int reported;

    public static boolean enabled() {
        return ModLoaded.ATMOSPHERICS && AWClientConfig.get().atmosphericsIntegration;
    }

    public static void enterDry() {
        if (!enabled()) return;
        dry++;
        report(1, "sandstorm");
    }

    public static void exitDry() {
        if (dry > 0) dry--;
    }

    public static boolean inDry() {
        return dry > 0;
    }

    public static void enterVisuals() {
        if (!enabled()) return;
        visuals++;
        report(2, "fog and sky");
    }

    public static void exitVisuals() {
        if (visuals > 0) visuals--;
    }

    public static void enterSkyPass() {
        skyPass = enabled();
    }

    public static void exitSkyPass() {
        skyPass = false;
    }

    public static void beginSkyColor() {
        skyColor++;
    }

    public static void endSkyColor() {
        if (skyColor > 0) skyColor--;
    }

    public static boolean asking() {
        return (dry > 0 || visuals > 0 || skyPass) && RenderSystem.isOnRenderThread();
    }

    private static void report(int hook, String name) {
        if ((reported & hook) != 0) return;
        reported |= hook;
        AWLogger.AW.info("[AdvancedWeather] ATMOSPHERICS hook active: {}", name);
    }

    private static float sandstorm(float delta) {
        return ClientWeatherState.presence(WeatherTypes.SANDSTORM, delta);
    }

    public static float rainLevel(float delta) {
        if (dry > 0) return sandstorm(delta);
        if (visuals > 0) return dryHere ? sandstorm(delta) : Float.NaN;
        if (skyPass && skyColor == 0) return 1f - SkyVeil.visibility(delta);
        return Float.NaN;
    }

    public static float thunderLevel(float delta) {
        return dry > 0 ? sandstorm(delta) : Float.NaN;
    }

    public static float dryRain() {
        return sandstorm(1f);
    }

    public static boolean dustWashedOut() {
        if (!enabled()) return false;
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.getRainLevel(1f) > 0.001f && sandstorm(1f) <= 0f;
    }

    public static boolean replacesWindStreaks() {
        if (!enabled()) return false;
        report(4, "wind streaks");
        return true;
    }

    public static Vec3 wind(ClientLevel level, double x, double y, double z, Vec3 theirs) {
        if (!enabled() || level == null || theirs == null) return null;
        report(8, "wind");

        WindSample air = WindField.sample(level, ClientWind.direction(), x, y, z, 0f);
        double strength = theirs.length() * Mth.clamp(0.5f + air.intensity(), 0.5f, 1.5f);
        return air.direction().scale(strength);
    }

    private static float progress() {
        return ClientWeatherState.getSmoothedTransitionProgress(
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }

    private static float authority(boolean from, boolean to, float progress) {
        if (from && to) return 1f;
        if (to) return progress;
        return from ? 1f - progress : 0f;
    }

    private static float ours(boolean from, boolean to, float progress, float before, float after) {
        if (from && to) return Mth.lerp(progress, before, after);
        return to ? after : before;
    }

    public static float fogStart(float theirs) {
        return fogDistance(theirs, true);
    }

    public static float fogEnd(float theirs) {
        return fogDistance(theirs, false);
    }

    private static float fogDistance(float theirs, boolean near) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled() || Float.isNaN(theirs) || mc.level == null) return theirs;

        float renderDistance = mc.gameRenderer.getRenderDistance();
        if (mc.level.dimension().equals(Level.END)) {
            float haze = ClientWeatherState.presence(WeatherTypes.CHORUS_GALE, 1f);
            return Mth.lerp(haze, theirs, near ? CHORUS_HAZE_NEAR : Math.min(renderDistance, CHORUS_HAZE_FAR));
        }

        WeatherTypes previous = ClientWeatherState.getPreviousWeather();
        WeatherTypes current = ClientWeatherState.getCurrentWeather();
        boolean from = previous.hasFog();
        boolean to = current.hasFog();
        if (!from && !to) return theirs;

        float progress = progress();
        float ours = ours(from, to, progress, distance(previous, near, renderDistance), distance(current, near, renderDistance));
        return Mth.lerp(authority(from, to, progress), theirs, ours);
    }

    private static float distance(WeatherTypes type, boolean near, float renderDistance) {
        float set = near ? WeatherDefinitions.get(type).fogNear : WeatherDefinitions.get(type).fogFar;
        if (set < 0f) return renderDistance;
        return near ? set : Math.min(renderDistance, set);
    }

    public static int fogColor(int theirs, int ours) {
        if (!enabled() || theirs == ours) return theirs;

        boolean from = ClientWeatherState.getPreviousWeather().hasFog();
        boolean to = ClientWeatherState.getCurrentWeather().hasFog();
        if (!from && !to) return theirs;

        float share = authority(from, to, progress());
        return blend(theirs, ours, 16, share) << 16 | blend(theirs, ours, 8, share) << 8 | blend(theirs, ours, 0, share);
    }

    public static float[] veiledTwilight(float[] colors) {
        if (colors == null || !enabled()) return colors;
        report(16, "twilight fog");

        float progress = progress();
        boolean from = ClientWeatherState.getPreviousWeather().hasFog();
        boolean to = ClientWeatherState.getCurrentWeather().hasFog();
        float fog = Mth.lerp(authority(from, to, progress), 1f, TWILIGHT_THROUGH_FOG);
        float veil = SkyVeil.visibility(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));

        float[] veiled = colors.clone();
        veiled[3] *= fog * veil;
        return veiled;
    }

    private static int blend(int theirs, int ours, int shift, float share) {
        return Mth.clamp(Math.round(Mth.lerp(share, theirs >> shift & 0xFF, ours >> shift & 0xFF)), 0, 255);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        dry = 0;
        visuals = 0;
        skyColor = 0;
        if (!enabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            dryHere = false;
            return;
        }
        var biome = mc.level.getBiome(mc.gameRenderer.getMainCamera().getBlockPosition());
        dryHere = DRY_BIOMES.stream().anyMatch(biome::is);
    }
}
