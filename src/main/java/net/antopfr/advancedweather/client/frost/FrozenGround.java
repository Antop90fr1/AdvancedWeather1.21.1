package net.antopfr.advancedweather.client.frost;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.render.TerrainUniforms;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.util.AWLogger;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class FrozenGround {

    private FrozenGround() {}

    private static final float FREEZE_TICKS = 20 * 45;
    private static final float THAW_TICKS = 20 * 60;

    private static final float SIDE_SHARE = 0.25f;
    private static final float PLANT_SHARE = 0.85f;

    private static final float SNOW_SIDE_SHARE = 0.06f;
    private static final float SNOW_PLANT_SHARE = 0.6f;

    private static final ResourceLocation SNOW_TEXTURE = ResourceLocation.withDefaultNamespace("block/snow");
    private static final ResourceLocation POWDER_SNOW_TEXTURE = ResourceLocation.withDefaultNamespace("block/powder_snow");
    private static final float[] SNOW_TINT = {0.93f, 0.96f, 1f};
    private static final float SNOW_GLINT = 0.3f;

    private static final ResourceLocation SAND_TEXTURE = ResourceLocation.withDefaultNamespace("block/sand");
    private static final ResourceLocation RED_SAND_TEXTURE = ResourceLocation.withDefaultNamespace("block/red_sand");
    private static final float[] SAND_TINT = {0.86f, 0.81f, 0.64f};
    private static final float[] RED_SAND_TINT = {0.75f, 0.40f, 0.13f};
    private static final float SAND_MOST = 0.55f;

    private static final double GLINT_PACE = 0.12;
    private static final double PATTERN_SPAN = 256.0;

    private static ClientLevel trackedLevel;
    private static float ice;
    private static float lastIce;
    private static boolean shown;
    private static boolean failed;

    public static float ice() {
        return ice;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            trackedLevel = null;
            ice = lastIce = 0f;
            return;
        }
        if (mc.isPaused()) return;

        float rain = level.dimension() == Level.OVERWORLD
                ? ClientWeatherState.presence(WeatherTypes.FREEZING_RAIN, 1f)
                : 0f;

        if (level != trackedLevel) {
            trackedLevel = level;
            ice = lastIce = rain;
            return;
        }

        lastIce = ice;
        ice = Mth.clamp(ice + rain / FREEZE_TICKS - (1f - rain) / THAW_TICKS, 0f, 1f);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || failed) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        float amount = AWClientConfig.get().frozenGround ? Mth.lerp(partialTick, lastIce, ice) : 0f;
        float snow = AWClientConfig.get().snowCover ? GroundCover.snow(partialTick) : 0f;
        float sand = AWClientConfig.get().sandCover ? GroundCover.sand(partialTick) : 0f;
        boolean sandy = sand > snow;
        float lying = Math.max(snow, sand);
        if (amount <= 0f && lying <= 0f && !shown) return;
        shown = amount > 0f || lying > 0f;

        try {
            Vec3 camera = event.getCamera().getPosition();
            double glint = Math.IEEEremainder((level.getGameTime() + (double) partialTick) * GLINT_PACE, Mth.TWO_PI);

            TerrainUniforms.set("AwFrost", amount, SIDE_SHARE, (float) glint, PLANT_SHARE);
            TerrainUniforms.set("AwSnow", lying, SNOW_SIDE_SHARE,
                    !sandy && AWClientConfig.get().snowCoverOnSnow ? 1f : 0f, SNOW_PLANT_SHARE);
            if (lying > 0f) {
                TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
                if (sandy) {
                    boolean red = GroundCover.redSand();
                    float[] tint = red ? RED_SAND_TINT : SAND_TINT;
                    place("AwSnowSprite", atlas.getSprite(red ? RED_SAND_TEXTURE : SAND_TEXTURE));
                    place("AwPowderSprite", atlas.getSprite(red ? SAND_TEXTURE : RED_SAND_TEXTURE));
                    TerrainUniforms.set("AwSnowTint", tint[0], tint[1], tint[2], 0f);
                    TerrainUniforms.set("AwSnowMost", SAND_MOST, 0f, 0f);
                } else {
                    place("AwSnowSprite", atlas.getSprite(SNOW_TEXTURE));
                    place("AwPowderSprite", atlas.getSprite(POWDER_SNOW_TEXTURE));
                    TerrainUniforms.set("AwSnowTint", SNOW_TINT[0], SNOW_TINT[1], SNOW_TINT[2], SNOW_GLINT);
                    TerrainUniforms.set("AwSnowMost", 1f, 1f, 0f);
                }
            }
            TerrainUniforms.set("AwFrostCam", wrapped(camera.x), wrapped(camera.y), wrapped(camera.z));
        } catch (Throwable t) {
            failed = true;
            AWLogger.AW.error("[AdvancedWeather] Frozen ground disabled", t);
        }
    }

    @SubscribeEvent
    public static void onLevelDrawn(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || failed || !shown) return;

        TerrainUniforms.set("AwFrost", 0f, 0f, 0f, 0f);
        TerrainUniforms.set("AwSnow", 0f, 0f, 0f, 0f);
    }

    private static void place(String uniform, TextureAtlasSprite sprite) {
        TerrainUniforms.set(uniform, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());
    }

    private static float wrapped(double coordinate) {
        return (float) (coordinate - Math.floor(coordinate / PATTERN_SPAN) * PATTERN_SPAN);
    }
}
