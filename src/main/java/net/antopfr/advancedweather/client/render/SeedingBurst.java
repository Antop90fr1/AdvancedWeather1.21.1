package net.antopfr.advancedweather.client.render;

import net.antopfr.advancedweather.weather.AtmosphericForcing;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class SeedingBurst {

    private static Vector3f[] paletteFor(AtmosphericForcing.Bias bias) {
        return switch (bias) {
            case HEATING -> new Vector3f[] { // fire crystal
                    rgb(0xD8534B), rgb(0xF59875), rgb(0xFFC342), rgb(0xF49855), rgb(0xF5667C)
            };
            case COOLING -> new Vector3f[] { // frost crystal
                    rgb(0x8FD4E8), rgb(0xB8ECF5), rgb(0x5FA8D8), rgb(0xE0F7FF), rgb(0x7FC4E0)
            };
            case SEEDING -> new Vector3f[] {
                    rgb(0x3D6BC4), rgb(0x5B8FD8), rgb(0x8FB8F0), rgb(0x2E4E8A), rgb(0xA8C8F5)
            };
            case DISSIPATING -> new Vector3f[] { // clear crystal
                    rgb(0xF5D98C), rgb(0xFFECB0), rgb(0xE0B860), rgb(0xFFF8D8), rgb(0xD8A848)
            };
            case NONE -> new Vector3f[] { rgb(0xCCCCCC), rgb(0xE8E8E8) };
        };
    }

    private static Vector3f rgb(int hex) {
        return new Vector3f(((hex >> 16) & 0xFF) / 255f,
                ((hex >> 8) & 0xFF) / 255f,
                (hex & 0xFF) / 255f);
    }

    private static final class Active {
        final Vec3 pos;
        final Vector3f[] palette;
        int age = 0;
        Active(Vec3 pos, Vector3f[] palette) { this.pos = pos; this.palette = palette; }
    }

    private static final List<Active> ACTIVE = new ArrayList<>();

    public static void spawn(Level level, Vec3 pos, AtmosphericForcing.Bias bias) {
        ACTIVE.add(new Active(pos, paletteFor(bias)));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ACTIVE.isEmpty()) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) { ACTIVE.clear(); return; }

        RandomSource r = level.random;
        ACTIVE.removeIf(b -> {
            step(level, r, b);
            return ++b.age > 8;
        });
    }

    private static Vector3f pick(Vector3f[] palette, RandomSource r) {
        return palette[r.nextInt(palette.length)];
    }

    private static void step(Level level, RandomSource r, Active b) {
        Vec3 c = b.pos;

        if (b.age == 0) {
            level.addParticle(ParticleTypes.FLASH, c.x, c.y, c.z, 0, 0, 0);

            int shards = 48;
            int pointsPerShard = 16;
            float baseLen = 6.0f;

            for (int i = 0; i < shards; i++) {
                double y = 1.0 - (i / (double)(shards - 1)) * 2.0;
                double radius = Math.sqrt(1.0 - y * y);
                double phi = i * 2.399963;
                double dx = Math.cos(phi) * radius;
                double dz = Math.sin(phi) * radius;

                float len = baseLen * (0.55f + r.nextFloat() * 0.75f);
                Vector3f col = pick(b.palette, r);
                float size = 1.4f + r.nextFloat() * 0.8f;
                DustParticleOptions dust = new DustParticleOptions(col, size);

                for (int j = 1; j <= pointsPerShard; j++) {
                    double t = (j / (double) pointsPerShard) * len;
                    if (j > pointsPerShard / 2 && r.nextFloat() < 0.3f) continue;
                    double vel = 0.03;
                    level.addParticle(dust,
                            c.x + dx * t, c.y + y * t, c.z + dz * t,
                            dx * vel, y * vel, dz * vel);
                }
            }
        } else if (b.age == 1) {
            for (int i = 0; i < 60; i++) {
                double theta = r.nextDouble() * Math.PI * 2;
                double phi = Math.acos(2 * r.nextDouble() - 1);
                double speed = 0.1 + r.nextDouble() * 0.15;
                Vector3f col = pick(b.palette, r);
                level.addParticle(new DustParticleOptions(col, 2.4f),
                        c.x, c.y, c.z,
                        Math.sin(phi) * Math.cos(theta) * speed,
                        Math.cos(phi) * speed,
                        Math.sin(phi) * Math.sin(theta) * speed);
            }
        } else if (b.age == 2) {
            for (int i = 0; i < 40; i++) {
                double d = 2.0 + r.nextDouble() * 3.0;
                double theta = r.nextDouble() * Math.PI * 2;
                double phi = Math.acos(2 * r.nextDouble() - 1);
                double px = c.x + Math.sin(phi) * Math.cos(theta) * d;
                double py = c.y + Math.cos(phi) * d;
                double pz = c.z + Math.sin(phi) * Math.sin(theta) * d;
                level.addParticle(ParticleTypes.END_ROD, px, py, pz,
                        (r.nextDouble() - 0.5) * 0.02,
                        (r.nextDouble() - 0.5) * 0.02,
                        (r.nextDouble() - 0.5) * 0.02);
            }
        } else if (b.age >= 4 && b.age % 2 == 0) {
            for (int i = 0; i < 8; i++) {
                Vector3f col = pick(b.palette, r);
                level.addParticle(new DustParticleOptions(col, 1.2f),
                        c.x + (r.nextDouble() - 0.5) * 4,
                        c.y + (r.nextDouble() - 0.5) * 3,
                        c.z + (r.nextDouble() - 0.5) * 4,
                        0, -0.04, 0);
            }
        }
    }
}
