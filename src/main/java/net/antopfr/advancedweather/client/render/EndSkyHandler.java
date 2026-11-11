package net.antopfr.advancedweather.client.render;

import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.effect.types.void_storm.VoidPulse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class EndSkyHandler {
    private static float r = 1f, g = 1f, b = 1f;
    private static float tr = 1f, tg = 1f, tb = 1f;
    private static int retargetCooldown = 0;
    private static final RandomSource random = RandomSource.create();

    public static void tick() {
        boolean voidStorm = ClientWeatherState.getCurrentWeather() == WeatherTypes.VOID_STORM;

        if (voidStorm) {
            if (retargetCooldown <= 0) {
                retargetCooldown = 40 + random.nextInt(60);
                tr = 0.7f + random.nextFloat() * 0.9f;
                tg = 0.3f + random.nextFloat() * 0.4f;
                tb = 1.0f + random.nextFloat();
            }
            retargetCooldown--;
        } else {
            tr = 1f; tg = 1f; tb = 1f;
        }

        r = Mth.lerp(0.03f, r, tr);
        g = Mth.lerp(0.03f, g, tg);
        b = Mth.lerp(0.03f, b, tb);
    }

    private static float pulse() {
        if (ClientWeatherState.getCurrentWeather() != WeatherTypes.VOID_STORM) return 1f;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return 1f;

        double phase = VoidPulse.phase(level.getGameTime(), mc.getTimer().getGameTimeDeltaPartialTick(true));
        float slow = (float) Math.sin(phase) * 0.18f;
        float fast = (float) Math.sin(phase * 2.7) * 0.08f;
        return 1f + slow + fast;
    }
    public static float red()   { return r * pulse(); }
    public static float green() { return g * pulse(); }
    public static float blue()  { return b * pulse(); }
}