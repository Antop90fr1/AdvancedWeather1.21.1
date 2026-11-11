package net.antopfr.advancedweather.server.event;

import net.antopfr.advancedweather.content.entity.KiteEntity;
import net.antopfr.advancedweather.network.toclient.FlashPacket;
import net.antopfr.advancedweather.util.PlayerChecks;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@EventBusSubscriber(modid = "advancedweather")
public class LightningServerEvent {

    private static final Random random = new Random();
    private static final Map<UUID, Integer> lightningCooldowns = new HashMap<>();

    private static final float KITE_STRIKE_CHANCE = 0.08f;

    private static final int CLOSE_RANGE = 32;
    private static final float CLOSE_SHARE = 0.25f;
    private static final double FAR_MIN = 60.0;
    private static final double FAR_MAX = 200.0;
    private static final float DAZZLE_FULL = 24f;
    private static final float DAZZLE_NONE = 140f;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        WeatherTypes type = WeatherManager.get(level).getCurrentWeather(level);

        if (type == WeatherTypes.THUNDERSTORM || type == WeatherTypes.NETHERSTORM || type == WeatherTypes.ENDERSTORM) {
            tickLightning(player, level, type);
        }
    }

    private static void tickLightning(ServerPlayer player, ServerLevel level, WeatherTypes type) {
        UUID uuid = player.getUUID();
        int cooldown = lightningCooldowns.getOrDefault(uuid, 0);
        if (cooldown > 0) {
            lightningCooldowns.put(uuid, cooldown - 1);
            return;
        }
        lightningCooldowns.put(uuid, 100 + random.nextInt(200));

        KiteEntity kite = kiteInTheSky(level, player);
        if (kite != null && random.nextFloat() < KITE_STRIKE_CHANCE) {
            strike(level, player, kite.position(), false);
            return;
        }

        boolean far = type == WeatherTypes.THUNDERSTORM && random.nextFloat() >= CLOSE_SHARE;
        double x = 0.0;
        double z = 0.0;
        if (far) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = FAR_MIN + random.nextDouble() * (FAR_MAX - FAR_MIN);
            x = player.getX() + Math.cos(angle) * distance;
            z = player.getZ() + Math.sin(angle) * distance;
            far = level.isLoaded(BlockPos.containing(x, player.getY(), z));
        }
        if (!far) {
            x = player.getX() + (random.nextDouble() - 0.5) * CLOSE_RANGE;
            z = player.getZ() + (random.nextDouble() - 0.5) * CLOSE_RANGE;
        }

        BlockPos strikePos;
        boolean isNether = level.dimension().equals(Level.NETHER);

        if (isNether) {
            int startY = Math.min(player.getBlockY() + 15, 120);
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos((int) x, startY, (int) z);

            while (level.isEmptyBlock(mutablePos) && mutablePos.getY() > level.getMinBuildHeight()) {
                mutablePos.move(0, -1, 0);
            }
            strikePos = mutablePos.immutable();
        } else {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
            strikePos = new BlockPos((int) x, y, (int) z);

            if (level.dimension().equals(Level.OVERWORLD) && !level.canSeeSky(strikePos)) return;
        }

        strike(level, player, Vec3.atBottomCenterOf(strikePos), isNether || far);
    }

    private static void strike(ServerLevel level, ServerPlayer player, Vec3 at, boolean visualOnly) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.moveTo(at);

            if (visualOnly) {
                lightning.setVisualOnly(true);
            }

            level.addFreshEntity(lightning);
        }

        if (!PlayerChecks.isShielded(level, player.blockPosition())) {
            float away = (float) Math.sqrt(player.distanceToSqr(at));
            float dazzle = 1f - Mth.clamp((away - DAZZLE_FULL) / (DAZZLE_NONE - DAZZLE_FULL), 0f, 1f);
            if (dazzle > 0f) PacketDistributor.sendToPlayer(player, new FlashPacket(dazzle));
        }
    }

    private static KiteEntity kiteInTheSky(ServerLevel level, ServerPlayer player) {
        if (level.dimension().equals(Level.NETHER)) return null;

        for (KiteEntity kite : level.getEntitiesOfClass(KiteEntity.class, player.getBoundingBox().inflate(16.0))) {
            if (player.equals(kite.getOwnerPlayer()) && level.canSeeSky(kite.blockPosition())) return kite;
        }
        return null;
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        lightningCooldowns.remove(event.getEntity().getUUID());
    }
}
