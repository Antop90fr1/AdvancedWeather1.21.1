package net.antopfr.advancedweather.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.client.particle.AWParticles;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Unique
    private static final ResourceLocation FREEZING_RAIN_LOCATION =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/environment/freezing_rain.png");

    @Unique
    private static final ResourceLocation HAIL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/environment/hail.png");

    @Unique
    private static final ResourceLocation VANILLA_RAIN =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/rain.png");

    @WrapOperation(
            method = "renderSnowAndRain",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"
            )
    )
    private void redirectShaderTexture(int unit, ResourceLocation texture, Operation<Void> original) {
        WeatherTypes current  = ClientWeatherState.getCurrentWeather();
        WeatherTypes previous = ClientWeatherState.getPreviousWeather();
        float aw_partialTick = 0f;
        float t = ClientWeatherState.getSmoothedTransitionProgress(aw_partialTick);

        ResourceLocation currentTex  = advancedWeather1_21_1$resolveTexture(current,  texture);
        ResourceLocation previousTex = advancedWeather1_21_1$resolveTexture(previous, texture);

        boolean texturesMatch = currentTex.equals(previousTex);

        if (texturesMatch || t >= 1.0f) {
            original.call(unit, currentTex);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else if (t < 0.5f) {
            original.call(unit, previousTex);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f - (t * 2f));
        } else {
            original.call(unit, currentTex);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (t - 0.5f) * 2f);
        }
    }

    @Unique
    private ResourceLocation advancedWeather1_21_1$resolveTexture(WeatherTypes type, ResourceLocation vanillaTexture) {
        if (type == WeatherTypes.FREEZING_RAIN && vanillaTexture.equals(VANILLA_RAIN)) {
            return FREEZING_RAIN_LOCATION;
        } else if (type == WeatherTypes.HAIL && vanillaTexture.equals(VANILLA_RAIN)) {
            return HAIL_LOCATION;
        }
        return vanillaTexture;
    }

    @WrapOperation(
            method = "tickRain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
            )
    )
    private void redirectRainParticle(
            ClientLevel level,
            ParticleOptions options,
            double x, double y, double z,
            double dx, double dy, double dz,
            Operation<Void> original
    ) {
        WeatherTypes type = ClientWeatherState.getCurrentWeather();

        if (type == WeatherTypes.HAIL) return;

        if (type == WeatherTypes.FREEZING_RAIN && options == ParticleTypes.RAIN) {
            original.call(level, AWParticles.FREEZING_RAIN.get(), x, y, z, dx, dy, dz);
            return;
        }

        original.call(level, options, x, y, z, dx, dy, dz);
    }

    @Inject(
            method = "tickRain",
            at = @At("HEAD"),
            cancellable = true
    )
    private void suppressHailRainEffects(Camera camera, CallbackInfo ci) {
        if (ClientWeatherState.getCurrentWeather() != WeatherTypes.HAIL) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.cameraEntity == null) {
            ci.cancel();
            return;
        }

        BlockPos cameraPos = mc.cameraEntity.blockPosition();

        int count = 3 + level.random.nextInt(3);
        for (int i = 0; i < count; i++) {
            BlockPos pos = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING,
                    cameraPos.offset(
                            level.random.nextInt(25) - 12,
                            0,
                            level.random.nextInt(25) - 12
                    )
            );

            double x = pos.getX() + level.random.nextDouble();
            double y = pos.getY() + 0.1D;
            double z = pos.getZ() + level.random.nextDouble();

            level.addParticle(AWParticles.HAIL_DROP.get(), x, y, z, 0.0D, 0.0D, 0.0D);

            BlockPos impactPos = pos.below();
            BlockState state = level.getBlockState(impactPos);
            if (state.is(BlockTags.LEAVES)) {
                for (int j = 0; j < 2; j++) {
                    level.addParticle(
                            new BlockParticleOption(ParticleTypes.BLOCK, state),
                            x + (level.random.nextDouble() - 0.5) * 0.3,
                            y + 0.1,
                            z + (level.random.nextDouble() - 0.5) * 0.3,
                            (level.random.nextDouble() - 0.5) * 0.1,
                            0.05 + level.random.nextDouble() * 0.05,
                            (level.random.nextDouble() - 0.5) * 0.1
                    );
                }
            }

            SoundType soundType = level.getBlockState(impactPos).getSoundType();
            SoundEvent impactSound;
            float pitch = 2.5f + level.random.nextFloat() * 1.4f;
            float volume = 0.45f + level.random.nextFloat() * 0.1f;

            BlockState impactState = level.getBlockState(impactPos);

            if (level.random.nextInt(2) == 0) {
                if (impactState.is(BlockTags.LEAVES)) {
                    impactSound = SoundEvents.GRASS_HIT;
                    pitch = 2.1f + level.random.nextFloat() * 0.8f;
                    volume = 0.4f;
                } else if (impactState.is(BlockTags.WOOL) || impactState.is(BlockTags.WOOL_CARPETS)) {
                    impactSound = SoundEvents.WOOL_HIT;
                    pitch = 3.0f + level.random.nextFloat() * 0.5f;
                    volume = 0.32f;
                } else if (impactState.liquid()) {
                    impactSound = SoundEvents.GENERIC_SPLASH;
                    pitch = 3.0f + level.random.nextFloat();
                    volume = 0.45f;
                } else if (soundType == SoundType.GLASS) {
                    impactSound = SoundEvents.GLASS_BREAK;
                    pitch = 3.0f + level.random.nextFloat() * 0.5f;
                    volume = 0.48f;
                } else if (soundType == SoundType.METAL || soundType == SoundType.COPPER
                        || soundType == SoundType.NETHERITE_BLOCK) {
                    impactSound = SoundEvents.ANVIL_FALL;
                    pitch = 3.5f + level.random.nextFloat() * 0.5f;
                    volume = 0.31f;
                } else if (soundType == SoundType.STONE || soundType == SoundType.DEEPSLATE || soundType == SoundType.NETHER_BRICKS) {
                    impactSound = SoundEvents.STONE_HIT;
                    pitch = 2.8f + level.random.nextFloat();
                    volume = 0.38f;
                } else if (soundType == SoundType.WOOD || soundType == SoundType.CHERRY_WOOD
                        || soundType == SoundType.BAMBOO_WOOD) {
                    impactSound = SoundEvents.WOOD_HIT;
                    pitch = 2.6f + level.random.nextFloat() * 1.2f;
                    volume = 0.4f;
                } else if (impactState.is(BlockTags.SAND) || impactState.is(BlockTags.DIRT)) {
                    impactSound = SoundEvents.SAND_HIT;
                    pitch = 2.5f + level.random.nextFloat();
                    volume = 0.4f;
                } else {
                    impactSound = SoundEvents.CANDLE_BREAK;
                }

                level.playLocalSound(x, y, z, impactSound, SoundSource.WEATHER, volume, pitch, false);
            }
        }

        ci.cancel();
    }

    @ModifyVariable(
            method = "renderSnowAndRain",
            at = @At("STORE"),
            ordinal = 3
    )
    private float onSnowSpeed(float original) {
        WeatherTypes type = ClientWeatherState.getCurrentWeather();
        return switch (type) {
            case BLIZZARD -> original * 16.0f;
            case SNOW -> original * 1.5f;
            default -> original;
        };
    }
}