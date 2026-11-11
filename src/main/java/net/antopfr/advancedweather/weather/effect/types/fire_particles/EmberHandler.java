package net.antopfr.advancedweather.weather.effect.types.fire_particles;

import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.effect.types.lava_rain.LavaRainSky;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public final class EmberHandler {

    private EmberHandler() {}

    private static final int RADIUS = 16;
    private static final int EMBERS = 4;
    private static final double HEIGHT = 8.0;
    private static final double DRIFT = 0.35;
    private static final float MINIMAL_SHARE = 0.35f;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;
        if (!ClientWeatherState.hasEffect(WeatherEffects.FIRE_PARTICLES)) return;

        float strength = ClientWeatherState.presence(WeatherTypes.NETHERSTORM, 1f);
        if (strength <= 0f) return;

        boolean minimal = mc.options.particles().get() == ParticleStatus.MINIMAL;
        int embers = Math.round(EMBERS * strength * (minimal ? MINIMAL_SHARE : 1f));

        RandomSource random = level.random;
        Vec3 player = mc.player.position();
        Vec3 prevailing = ClientWind.direction();

        for (int i = 0; i < embers; i++) {
            double x = player.x + (random.nextDouble() - 0.5) * 2 * RADIUS;
            double z = player.z + (random.nextDouble() - 0.5) * 2 * RADIUS;
            double y = player.y - 1.0 + random.nextDouble() * HEIGHT;

            LavaRainSky.Opening opening = LavaRainSky.below(level, Mth.floor(x), y, Mth.floor(z));
            if (opening == null || y >= opening.roof()) continue;

            Vec3 wind = WindField.sample(level, prevailing, x, y, z, 0f).velocity().scale(DRIFT);
            level.addParticle(ParticleTypes.SMALL_FLAME, minimal, x, y, z,
                    wind.x, 0.02 + random.nextDouble() * 0.03, wind.z);
        }
    }
}
