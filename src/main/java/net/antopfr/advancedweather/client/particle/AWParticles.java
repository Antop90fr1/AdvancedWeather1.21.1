package net.antopfr.advancedweather.client.particle;

import net.antopfr.advancedweather.AdvancedWeather;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AWParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, AdvancedWeather.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FREEZING_RAIN =
            PARTICLE_TYPES.register("freezing_rain", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLIZZARD_FLAKE =
            PARTICLE_TYPES.register("blizzard_flake", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FOG_WISP =
            PARTICLE_TYPES.register("fog_wisp", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GROUND_FOG =
            PARTICLE_TYPES.register("ground_fog", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SAND_PARTICLE =
            PARTICLE_TYPES.register("sand_particle", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RED_SAND_PARTICLE =
            PARTICLE_TYPES.register("red_sand_particle", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOUL_SAND_PARTICLE =
            PARTICLE_TYPES.register("soul_sand_particle", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BRIMSTONE_DUST =
            PARTICLE_TYPES.register("brimstone_dust", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LEAF =
            PARTICLE_TYPES.register("leaf", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HAIL_DROP =
            PARTICLE_TYPES.register("hail_drop", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ASH_PARTICLE =
            PARTICLE_TYPES.register("ash_particle", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MERCURY_BUBBLE_POP =
            PARTICLE_TYPES.register("mercury_bubble_pop", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHORUS_PETAL =
            PARTICLE_TYPES.register("chorus_petal", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LAVA_DROP =
            PARTICLE_TYPES.register("lava_drop", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOID_MOTE =
            PARTICLE_TYPES.register("void_mote", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOID_DROP =
            PARTICLE_TYPES.register("void_drop", () -> new SimpleParticleType(false));

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
