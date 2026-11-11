package net.antopfr.advancedweather.client.event;

import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.weather.BiomeFogData;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class FogHandler {

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float renderDistance = event.getRenderer().getRenderDistance();

        if (mc.level.dimension().equals(Level.END)) {
            float haze = ClientWeatherState.presence(WeatherTypes.CHORUS_GALE, (float) event.getPartialTick());
            event.setNearPlaneDistance(Mth.lerp(haze, renderDistance, 12f));
            event.setFarPlaneDistance(Mth.lerp(haze, renderDistance * 10f, Math.min(renderDistance, 160f)));
            event.setCanceled(true);
            return;
        }

        ClientWeatherState.fogDistanceLerp.update(
                ClientWeatherState.getPreviousWeather(),
                ClientWeatherState.getCurrentWeather(),
                ClientWeatherState.getSmoothedTransitionProgress((float) event.getPartialTick()),
                renderDistance);

        if (!ClientWeatherState.fogDistanceLerp.shouldRenderCustomFog()) return;

        event.setNearPlaneDistance(ClientWeatherState.fogDistanceLerp.getCurrentNear());
        event.setFarPlaneDistance(ClientWeatherState.fogDistanceLerp.getCurrentFar());
        event.setCanceled(true);
    }

    private static Vec3 smoothedFog = null;

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        WeatherTypes current  = ClientWeatherState.getCurrentWeather();
        WeatherTypes previous = ClientWeatherState.getPreviousWeather();

        Vec3 vanilla = new Vec3(event.getRed(), event.getGreen(), event.getBlue());

        BlockPos pos = BlockPos.containing(event.getCamera().getPosition());
        ResourceLocation biome = mc.level.getBiome(pos).unwrapKey()
                .map(ResourceKey::location).orElse(null);

        Vec3 from = previous.hasFog() ? toVec(BiomeFogData.getColor(mc.level, previous, biome)) : vanilla;
        Vec3 to   = current.hasFog()  ? toVec(BiomeFogData.getColor(mc.level, current, biome))  : vanilla;

        float t = ClientWeatherState.getSmoothedTransitionProgress((float) event.getPartialTick());
        Vec3 target = from.lerp(to, t);

        if (smoothedFog == null) {
            smoothedFog = target;
        } else {
            smoothedFog = smoothedFog.lerp(target, 0.03);
        }

        float darken = mc.level.getSkyDarken((float) event.getPartialTick());

        event.setRed((float) smoothedFog.x * darken);
        event.setGreen((float) smoothedFog.y * darken);
        event.setBlue((float) smoothedFog.z * darken);
    }

    private static Vec3 toVec(int rgb) {
        return new Vec3(((rgb >> 16) & 0xFF) / 255.0,
                ((rgb >> 8)  & 0xFF) / 255.0,
                ( rgb        & 0xFF) / 255.0);
    }
}
