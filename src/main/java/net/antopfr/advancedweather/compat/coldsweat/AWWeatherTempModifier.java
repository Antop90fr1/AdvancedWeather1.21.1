package net.antopfr.advancedweather.compat.coldsweat;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

public class AWWeatherTempModifier extends TempModifier {

    private static final double CELSIUS_TO_MC = 25.0;

    public AWWeatherTempModifier() {
        this.tickRate(20);
    }

    @Override
    protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait) {
        if (!AWCommonConfig.get().coldSweatIntegration) return temp -> temp;
        if (!(entity.level() instanceof ServerLevel level)) return temp -> temp;

        float deltaC = ColdSweatCompat.weatherDeltaCelsius(level, entity.blockPosition());
        double deltaMc = deltaC / CELSIUS_TO_MC;
        return temp -> temp + deltaMc;
    }
}
