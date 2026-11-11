package net.antopfr.advancedweather.weather.effect.types.lava_rain;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID)
public final class LavaRainBurn {

    private LavaRainBurn() {}

    public static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "lava_rain_immune"));

    public static final TagKey<Item> HELMETS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "lava_rain_helmets"));

    private static final int INTERVAL = 20;
    private static final float HIT_CHANCE = 0.35f;
    private static final float BURN_SECONDS = 3f;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.NETHER)) return;
        if (level.getGameTime() % INTERVAL != 0) return;
        if (!AWCommonConfig.get().lavaRainBurnsEntities) return;
        if (WeatherManager.get(level).getCurrentWeather(level) != WeatherTypes.LAVA_RAIN) return;

        level.getEntities(EntityTypeTest.forClass(LivingEntity.class), entity -> canBurn(entity)
                        && level.random.nextFloat() < HIT_CHANCE
                        && isExposed(level, entity))
                .forEach(entity -> entity.igniteForSeconds(BURN_SECONDS));
    }

    private static boolean canBurn(LivingEntity entity) {
        if (!entity.isAlive() || entity.fireImmune() || entity.hasEffect(MobEffects.FIRE_RESISTANCE)) return false;
        if (entity.getType().is(IMMUNE) || entity.getItemBySlot(EquipmentSlot.HEAD).is(HELMETS)) return false;
        return !(entity instanceof Player player) || !(player.isCreative() || player.isSpectator());
    }

    private static boolean isExposed(ServerLevel level, LivingEntity entity) {
        double head = entity.getBoundingBox().maxY;
        LavaRainSky.Opening opening = LavaRainSky.below(level, entity.getBlockX(), head, entity.getBlockZ());
        return opening != null && head < opening.roof();
    }
}
