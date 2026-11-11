package net.antopfr.advancedweather.weather.effect.types.chorus_petals;

import net.antopfr.advancedweather.client.particle.AWParticles;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.util.PlayerChecks;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class ChorusPetalHandler {

    private static final int PEAK_COUNT = 10;
    private static final double UPWIND_NEAR = 6.0;
    private static final double UPWIND_FAR = 22.0;
    private static final double SIDE_SPREAD = 34.0;
    private static final double HEIGHT_BELOW = 3.0;
    private static final double HEIGHT_ABOVE = 11.0;
    private static final double SPEED = 0.25;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        if (!ClientWeatherState.hasEffect(WeatherEffects.CHORUS_PETALS)) return;
        if (PlayerChecks.isShielded(mc.level, mc.player.blockPosition())) return;

        RandomSource random = mc.level.random;
        float gust = ClientWind.gust();
        int count = Math.round(PEAK_COUNT * (0.3f + 0.7f * gust * gust));

        Vec3 wind = ClientWind.direction();
        double speed = SPEED * ClientWind.surge();
        Vec3 eye = mc.player.getEyePosition();

        for (int i = 0; i < count; i++) {
            double upwind = UPWIND_NEAR + random.nextDouble() * (UPWIND_FAR - UPWIND_NEAR);
            double side = (random.nextDouble() - 0.5) * SIDE_SPREAD;
            double x = eye.x - wind.x * upwind - wind.z * side;
            double z = eye.z - wind.z * upwind + wind.x * side;
            double y = eye.y - HEIGHT_BELOW + random.nextDouble() * (HEIGHT_BELOW + HEIGHT_ABOVE);

            double pace = speed * (0.7 + random.nextDouble() * 0.6);
            mc.level.addParticle(AWParticles.CHORUS_PETAL.get(), x, y, z,
                    wind.x * pace + (random.nextDouble() - 0.5) * 0.04,
                    (random.nextDouble() - 0.4) * 0.05,
                    wind.z * pace + (random.nextDouble() - 0.5) * 0.04);
        }
    }
}
