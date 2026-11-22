package net.antopfr.advancedweather.client.frost;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.render.TerrainUniforms;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.client.wind.ClientWind;
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
    private static final float SNOW_LEAF_SHARE = 0.35f;

    private static final ResourceLocation SNOW_TEXTURE = ResourceLocation.withDefaultNamespace("block/snow");
    private static final ResourceLocation POWDER_SNOW_TEXTURE = ResourceLocation.withDefaultNamespace("block/powder_snow");
    private static final ResourceLocation SNOWY_SIDE_TEXTURE = ResourceLocation.withDefaultNamespace("block/grass_block_snow");
    private static final ResourceLocation WATER_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final float[] SNOW_TINT = {0.93f, 0.96f, 1f};
    private static final float SNOW_GLINT = 0.3f;
    private static final float[] SNOW_EDGE = {0xB2 / 255f, 0xD5 / 255f, 0xD5 / 255f};
    private static final float PLASTER_TICKS = 20 * 90;
    private static final float PLASTER_MOST = 0.25f;
    private static final float PLASTER_CALM = 0.1f;
    private static final float PLASTER_GALE = 0.7f;

    private static final ResourceLocation SAND_TEXTURE = ResourceLocation.withDefaultNamespace("block/sand");
    private static final ResourceLocation RED_SAND_TEXTURE = ResourceLocation.withDefaultNamespace("block/red_sand");
    private static final float[] SAND_TINT = {0.86f, 0.81f, 0.64f};
    private static final float[] RED_SAND_TINT = {0.75f, 0.40f, 0.13f};
    private static final float SAND_MOST = 0.55f;

    private static final double GLINT_PACE = 0.12;
    private static final double PATTERN_SPAN = RoofMap.SIZE;

    private static ClientLevel trackedLevel;
    private static float ice;
    private static float lastIce;
    private static float plasterX;
    private static float plasterZ;
    private static boolean shown;

    private static final int VALUES = 1;
    private static final int TEXTURES = 2;
    private static final int ROOF = 4;
    private static int broken;

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
            plasterX = plasterZ = 0f;
            return;
        }
        if (mc.isPaused()) return;

        float rain = level.dimension() == Level.OVERWORLD
                ? ClientWeatherState.presence(WeatherTypes.FREEZING_RAIN, 1f)
                : 0f;

        if (level != trackedLevel) {
            trackedLevel = level;
            ice = lastIce = rain;
            plasterX = plasterZ = 0f;
            broken = 0;
            TerrainUniforms.retry();
            return;
        }

        lastIce = ice;
        ice = Mth.clamp(ice + rain / FREEZE_TICKS - (1f - rain) / THAW_TICKS, 0f, 1f);
        plaster(level);
    }

    private static void plaster(ClientLevel level) {
        float falling = level.dimension() == Level.OVERWORLD
                ? ClientWeatherState.presence(WeatherTypes.SNOW, 1f) + ClientWeatherState.presence(WeatherTypes.BLIZZARD, 1f)
                : 0f;
        float windy = Mth.clamp((ClientWind.intensity() - PLASTER_CALM) / (PLASTER_GALE - PLASTER_CALM), 0f, 1f);
        Vec3 wind = ClientWind.direction();
        float flat = (float) Math.sqrt(wind.x * wind.x + wind.z * wind.z);
        if (falling > 0f && windy > 0f && flat > 1.0e-4f) {
            float pace = Math.min(1f, falling) * windy / PLASTER_TICKS;
            plasterX += ((float) -wind.x / flat - plasterX) * pace;
            plasterZ += ((float) -wind.z / flat - plasterZ) * pace;
        }
        float lying = GroundCover.snowNearby(1f);
        float length = Mth.sqrt(plasterX * plasterX + plasterZ * plasterZ);
        if (length > lying) {
            float kept = lying / length;
            plasterX *= kept;
            plasterZ *= kept;
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || off(VALUES)) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        try {
            draw(event, level);
        } catch (Throwable t) {
            fail(VALUES, "Frozen ground and ground cover switched off", t);
            TerrainUniforms.set("AwFrost", 0f, 0f, 0f, 0f);
            TerrainUniforms.set("AwSnow", 0f, 0f, 0f, 0f);
            TerrainUniforms.set("AwSnowColumns", 0f, 0f, 0f);
            TerrainUniforms.set("AwSnowWind", 0f, 0f, 0f);
        }
    }

    private static void draw(RenderLevelStageEvent event, ClientLevel level) {
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        float amount = AWClientConfig.get().frozenGround ? Mth.lerp(partialTick, lastIce, ice) : 0f;
        boolean textured = !off(TEXTURES);
        boolean covering = textured && AWClientConfig.get().snowCover;
        float snowHere = covering ? GroundCover.snow(partialTick) : 0f;
        float snow = covering ? GroundCover.snowNearby(partialTick) : 0f;
        float sand = textured && AWClientConfig.get().sandCover ? GroundCover.sand(partialTick) : 0f;
        boolean sandy = sand > snowHere;
        float lying = sandy ? sand : snow;
        if (amount <= 0f && lying <= 0f && !shown) return;
        shown = amount > 0f || lying > 0f;

        Vec3 camera = event.getCamera().getPosition();
        double glint = Math.IEEEremainder((level.getGameTime() + (double) partialTick) * GLINT_PACE, Mth.TWO_PI);

        TerrainUniforms.set("AwFrost", amount, SIDE_SHARE, (float) glint, PLANT_SHARE);
        TerrainUniforms.set("AwSnow", lying, SNOW_SIDE_SHARE,
                !sandy && AWClientConfig.get().snowCoverOnSnow ? 1f : 0f, SNOW_PLANT_SHARE);
        boolean columns = covering && !sandy && GroundCover.columnsKnown();
        TerrainUniforms.set("AwSnowColumns", columns ? 1f : 0f, snowHere, GroundCover.snowClock(partialTick));
        TerrainUniforms.set("AwSnowLeaves", !sandy && AWClientConfig.get().snowyLeaves ? 1f : 0f);
        boolean plastered = !sandy && snow > 0f;
        TerrainUniforms.set("AwSnowWind", plastered ? plasterX : 0f, plastered ? plasterZ : 0f, plastered ? PLASTER_MOST : 0f);
        TerrainUniforms.set("AwFrostCam", wrapped(camera.x), wrapped(camera.y), wrapped(camera.z));
        TerrainUniforms.set("AwFrostDetail", AWClientConfig.get().coverDetailDistance, 0f, 0f);
        placeSprites(lying, sandy);
        placeRoof(level, camera);
    }

    private static void placeSprites(float lying, boolean sandy) {
        if (off(TEXTURES)) return;
        try {
            if (lying > 0f) {
                TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
                if (sandy) {
                    boolean red = GroundCover.redSand();
                    float[] tint = red ? RED_SAND_TINT : SAND_TINT;
                    place("AwSnowSprite", atlas.getSprite(red ? RED_SAND_TEXTURE : SAND_TEXTURE));
                    place("AwPowderSprite", atlas.getSprite(red ? SAND_TEXTURE : RED_SAND_TEXTURE));
                    place("AwSnowSideSprite", atlas.getSprite(red ? RED_SAND_TEXTURE : SAND_TEXTURE));
                    TerrainUniforms.set("AwSnowTint", tint[0], tint[1], tint[2], 0f);
                    TerrainUniforms.set("AwSnowMost", SAND_MOST, 0f, SNOW_LEAF_SHARE);
                    TerrainUniforms.set("AwSnowEdge", 0f, 0f, 0f, 0f);
                } else {
                    place("AwSnowSprite", atlas.getSprite(SNOW_TEXTURE));
                    place("AwPowderSprite", atlas.getSprite(POWDER_SNOW_TEXTURE));
                    place("AwSnowSideSprite", atlas.getSprite(SNOWY_SIDE_TEXTURE));
                    TerrainUniforms.set("AwSnowTint", SNOW_TINT[0], SNOW_TINT[1], SNOW_TINT[2], SNOW_GLINT);
                    TerrainUniforms.set("AwSnowMost", 1f, 1f, SNOW_LEAF_SHARE);
                    TerrainUniforms.set("AwSnowEdge", SNOW_EDGE[0], SNOW_EDGE[1], SNOW_EDGE[2], 1f);
                }
            }
            TextureAtlas blocks = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
            place("AwWaterSprite", blocks.getSprite(WATER_TEXTURE));
            place("AwWaterFlowSprite", blocks.getSprite(WATER_FLOW_TEXTURE));
        } catch (Throwable t) {
            fail(TEXTURES, "Snow and sand cover switched off, frozen ground keeps going", t);
            TerrainUniforms.set("AwSnow", 0f, 0f, 0f, 0f);
            TerrainUniforms.set("AwSnowColumns", 0f, 0f, 0f);
            TerrainUniforms.set("AwSnowWind", 0f, 0f, 0f);
        }
    }

    private static boolean off(int part) {
        return (broken & part) != 0;
    }

    private static void fail(int part, String what, Throwable t) {
        broken |= part;
        AWLogger.AW.error("[AdvancedWeather] {}", what, t);
    }

    @SubscribeEvent
    public static void onLevelDrawn(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || off(VALUES) || !shown) return;

        TerrainUniforms.set("AwFrost", 0f, 0f, 0f, 0f);
        TerrainUniforms.set("AwSnow", 0f, 0f, 0f, 0f);
        TerrainUniforms.set("AwSnowWind", 0f, 0f, 0f);
    }

    private static void placeRoof(ClientLevel level, Vec3 camera) {
        if (off(ROOF)) return;
        try {
            RoofMap.update(level, camera.x, camera.z);
            RoofMap.bind();
            TerrainUniforms.setInt("AwRoofMap", RoofMap.UNIT);
            TerrainUniforms.set("AwRoofCam", (float) Math.floor(camera.x / PATTERN_SPAN),
                    (float) Math.floor(camera.y / PATTERN_SPAN), (float) Math.floor(camera.z / PATTERN_SPAN), 1f);
        } catch (Throwable t) {
            fail(ROOF, "Roof map switched off, snow and frost follow sky light only", t);
            TerrainUniforms.set("AwRoofCam", 0f, 0f, 0f, 0f);
        }
    }

    private static void place(String uniform, TextureAtlasSprite sprite) {
        TerrainUniforms.set(uniform, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());
    }

    private static float wrapped(double coordinate) {
        return (float) (coordinate - Math.floor(coordinate / PATTERN_SPAN) * PATTERN_SPAN);
    }
}
