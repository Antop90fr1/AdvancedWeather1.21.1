package net.antopfr.advancedweather.content.entity;

import com.tterrag.registrate.util.entry.EntityEntry;
import net.antopfr.advancedweather.content.item.kite.KiteRenderer;
import net.antopfr.advancedweather.util.AWRegistrate;
import net.antopfr.advancedweather.weather.effect.types.chorus_plants.ChorusPlantEntity;
import net.antopfr.advancedweather.weather.effect.types.chorus_plants.ChorusPlantRenderer;
import net.antopfr.advancedweather.weather.effect.types.tumbleweeds.TumbleweedEntity;
import net.antopfr.advancedweather.weather.effect.types.tumbleweeds.TumbleweedRenderer;
import net.minecraft.world.entity.MobCategory;

public class AWEntities {

    public static final EntityEntry<TumbleweedEntity> TUMBLEWEED =
            AWRegistrate.get()
                    .<TumbleweedEntity>entity("tumbleweed", TumbleweedEntity::new, MobCategory.MISC)
                    .properties(b -> b
                            .sized(0.7f, 0.7f)
                            .clientTrackingRange(8)
                            .updateInterval(3))
                    .renderer(() -> TumbleweedRenderer::new)
                    .register();

    public static final EntityEntry<ChorusPlantEntity> CHORUS_PLANT =
            AWRegistrate.get()
                    .<ChorusPlantEntity>entity("chorus_plant", ChorusPlantEntity::new, MobCategory.MISC)
                    .properties(b -> b
                            .sized(0.7f, 0.7f)
                            .clientTrackingRange(8)
                            .updateInterval(3))
                    .renderer(() -> ChorusPlantRenderer::new)
                    .register();

    public static final EntityEntry<SeedingRocketEntity> SEEDING_ROCKET =
            AWRegistrate.get()
                    .<SeedingRocketEntity>entity("seeding_rocket", SeedingRocketEntity::new, MobCategory.MISC)
                    .properties(b -> b
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(45)
                            .updateInterval(1))
                    .renderer(() -> SeedingRocketRenderer::new)
                    .register();

    public static final EntityEntry<KiteEntity> KITE =
            AWRegistrate.get()
                    .<KiteEntity>entity("kite", KiteEntity::new, MobCategory.MISC)
                    .properties(b -> b
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(10)
                            .updateInterval(1))
                    .renderer(() -> KiteRenderer::new)
                    .register();


    public static void register() {}
}
