package net.antopfr.advancedweather.weather.effect.types.void_storm;

import net.antopfr.advancedweather.client.particle.AWParticles;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public final class VoidStormHandler {

    private VoidStormHandler() {}

    private static final int MOTE_RADIUS = 22;
    private static final int MOTE_ATTEMPTS = 28;
    private static final double MOTE_DEPTH_NEAR = 6.0;
    private static final double MOTE_DEPTH_FAR = 20.0;

    private static final double ELYTRA_LIFT = 0.055;
    private static final double FALL_CUSHION = 0.048;
    private static final double MAX_RISE = 0.6;

    private static float lift;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) return;

        float presence = level.dimension().equals(Level.END)
                ? ClientWeatherState.presence(WeatherTypes.VOID_STORM, 1f)
                : 0f;
        float surge = presence * VoidPulse.surge(level.getGameTime(), 1f);

        if (surge > 0f && ClientWeatherState.hasEffect(WeatherEffects.VOID_PARTICLES)) {
            spawnMotes(level, player.position(), surge);
        }
        updraft(level, player, surge);
    }

    private static void spawnMotes(ClientLevel level, Vec3 around, float surge) {
        RandomSource random = level.random;
        int attempts = Math.round(MOTE_ATTEMPTS * surge * surge);
        for (int i = 0; i < attempts; i++) {
            double x = around.x + (random.nextDouble() - 0.5) * 2 * MOTE_RADIUS;
            double z = around.z + (random.nextDouble() - 0.5) * 2 * MOTE_RADIUS;
            if (!isOverVoid(level, Mth.floor(x), Mth.floor(z))) continue;

            double y = around.y - MOTE_DEPTH_NEAR - random.nextDouble() * (MOTE_DEPTH_FAR - MOTE_DEPTH_NEAR);
            level.addParticle(AWParticles.VOID_MOTE.get(), x, y, z, 0.0, 0.35 + random.nextDouble() * 0.35, 0.0);
        }
    }

    private static void updraft(ClientLevel level, LocalPlayer player, float surge) {
        boolean carried = AWCommonConfig.get().voidStormUpdrafts
                && !player.onGround() && !player.getAbilities().flying && !player.isSpectator()
                && isOverVoid(level, player.getBlockX(), player.getBlockZ());

        float target = carried ? Mth.clamp((surge - 0.3f) / 0.6f, 0f, 1f) : 0f;
        lift += (target - lift) * 0.15f;
        if (lift < 0.01f) return;

        Vec3 motion = player.getDeltaMovement();
        if (player.isFallFlying()) {
            if (motion.y < MAX_RISE) player.setDeltaMovement(motion.x, motion.y + ELYTRA_LIFT * lift, motion.z);
        } else if (motion.y < 0.0) {
            player.setDeltaMovement(motion.x, motion.y + FALL_CUSHION * lift, motion.z);
        }
    }

    private static boolean isOverVoid(Level level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) <= level.getMinBuildHeight();
    }
}
