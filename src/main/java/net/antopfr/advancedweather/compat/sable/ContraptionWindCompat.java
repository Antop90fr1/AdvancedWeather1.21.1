package net.antopfr.advancedweather.compat.sable;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindSample;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class ContraptionWindCompat {

    private static final double DRAG_COEFF = 0.09;

    private static final double AREA_PER_SURFACE = 1.0;
    private static final double HULL_AREA_FACTOR = 0.15;

    public static void register() {
        SableEventPlatform.INSTANCE.onPhysicsTick(ContraptionWindCompat::onPhysicsTick);
    }

    private static void onPhysicsTick(SubLevelPhysicsSystem system, double timeStep) {
        if (!AWCommonConfig.get().windAffectsContraptions) return;

        ServerLevel level = system.getLevel();
        SubLevelContainer c = SubLevelContainer.getContainer(level);
        if (!(c instanceof ServerSubLevelContainer container)) return;

        if (WindField.baseIntensity(level) <= 0.01f) return;
        Vec3 prevailing = WindField.prevailingDirection(level, 0f);

        for (ServerSubLevel sub : container.getAllSubLevels()) {
            if (!isAerodynamic(sub)) continue;

            RigidBodyHandle handle = RigidBodyHandle.of(sub);
            if (!handle.isValid()) continue;

            MassData mass = sub.getMassTracker();
            if (mass.isInvalid()) continue;

            Vector3dc position = sub.logicalPose().position();
            WindSample air = WindField.sample(level, prevailing, position.x(), position.y(), position.z(), 0f);
            double speed = air.speedKmh() / 3.6;
            Vector3d windWorld = new Vector3d(air.direction().x * speed, 0.0, air.direction().z * speed);

            Vector3d vel = handle.getLinearVelocity(new Vector3d());
            Vector3d rel = new Vector3d(windWorld).sub(vel.x, 0.0, vel.z);
            double relSpeed = rel.length();
            if (relSpeed < 0.01) continue;

            double pressure = DimensionPhysicsData.getAirPressure(level, position);

            Vector3d localWind = sub.logicalPose().orientation()
                    .transformInverse(rel.normalize(new Vector3d()));

            double force = DRAG_COEFF * exposedArea(sub, localWind) * relSpeed * relSpeed * pressure
                    * AWCommonConfig.get().contraptionWindStrength;

            Vector3d localImpulse = new Vector3d(localWind).mul(force * timeStep);

            sub.getOrCreateQueuedForceGroup(AWForceGroups.WIND.get())
                    .applyAndRecordPointForce(centerOfPressure(sub), localImpulse);
        }
    }

    private static boolean isAerodynamic(ServerSubLevel sub) {
        ServerLevelPlot plot = sub.getPlot();

        if (!plot.getLiftProviders().isEmpty()) return true;

        if (sub.getFloatingBlockController().needsTicking()) return true;

        for (BlockEntitySubLevelActor actor : plot.getBlockEntityActors()) {
            if (actor instanceof BlockEntitySubLevelPropellerActor) return true;
        }
        return false;
    }

    private static double exposedArea(ServerSubLevel sub, Vector3d localWind) {
        int surfaces = sub.getPlot().getLiftProviders().size();
        BoundingBox3ic bounds = sub.getPlot().getBoundingBox();
        double sizeX = bounds.maxX() - bounds.minX() + 1;
        double sizeY = bounds.maxY() - bounds.minY() + 1;
        double sizeZ = bounds.maxZ() - bounds.minZ() + 1;
        double silhouette = Math.abs(localWind.x) * sizeY * sizeZ
                + Math.abs(localWind.y) * sizeX * sizeZ
                + Math.abs(localWind.z) * sizeX * sizeY;
        return surfaces * AREA_PER_SURFACE + silhouette * HULL_AREA_FACTOR;
    }

    private static Vector3d centerOfPressure(ServerSubLevel sub) {
        var providers = sub.getPlot().getLiftProviders();

        if (providers.isEmpty()) {
            Vector3dc com = sub.getMassTracker().getCenterOfMass();
            return com != null ? new Vector3d(com) : new Vector3d();
        }

        Vector3d sum = new Vector3d();
        for (var ctx : providers) {
            BlockPos p = ctx.pos();
            sum.add(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
        }
        return sum.div(providers.size());
    }
}
