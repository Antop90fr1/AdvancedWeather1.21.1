package net.antopfr.advancedweather.client.storm;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.particle.AWParticles;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class HailOmen {

    private HailOmen() {}

    public static final float RED = 0.86f;
    public static final float GREEN = 0.80f;
    public static final float BLUE = 0.38f;
    private static final float DEEPEST = 0.65f;

    private static final float FADE_TICKS = 20 * 12;

    private static final float STONES_FROM = 0.75f;
    private static final int STONES_RANGE = 12;
    private static final float STONES_A_TICK = 0.6f;

    public static float yellowing(float partialTick) {
        float near = ComingWeather.nearness(WeatherTypes.HAIL, partialTick);
        if (near > 0f) return DEEPEST * near * near * (3f - 2f * near);

        long since = ComingWeather.sinceArrival(WeatherTypes.HAIL);
        if (since < 0 || since >= FADE_TICKS || !AWClientConfig.get().distantStorms) return 0f;
        return DEEPEST * (1f - (since + partialTick) / FADE_TICKS);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || level.dimension() != Level.OVERWORLD) return;

        float near = ComingWeather.nearness(WeatherTypes.HAIL, 0f);
        if (near < STONES_FROM) return;

        float chance = STONES_A_TICK * (near - STONES_FROM) / (1f - STONES_FROM);
        if (level.random.nextFloat() >= chance) return;

        BlockPos camera = mc.gameRenderer.getMainCamera().getBlockPosition();
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                camera.offset(level.random.nextInt(2 * STONES_RANGE + 1) - STONES_RANGE, 0,
                        level.random.nextInt(2 * STONES_RANGE + 1) - STONES_RANGE));
        level.addParticle(AWParticles.HAIL_DROP.get(),
                ground.getX() + level.random.nextDouble(), ground.getY() + 0.1, ground.getZ() + level.random.nextDouble(),
                0.0, 0.0, 0.0);
    }
}
