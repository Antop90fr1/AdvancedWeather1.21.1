package net.antopfr.advancedweather.client.cloud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.antopfr.advancedweather.client.storm.DistantStorm;
import net.antopfr.advancedweather.client.storm.HailOmen;
import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.compat.iris.IrisCompat;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class CloudRenderer {

    private CloudRenderer() {}

    private static final ResourceLocation SHADER =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "clouds");

    private static final int EVICT_AFTER_FRAMES = 240;

    private static final Map<Long, CloudTile> TILES = new HashMap<>();
    private static final List<CloudTile> VISIBLE = new ArrayList<>();

    private static ClientLevel trackedLevel;
    private static long frame;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != trackedLevel) {
            releaseAll();
            trackedLevel = level;
        }
        if (level == null || IrisCompat.shaderPackInUse() || !CloudState.visible(level)) return;

        ShaderProgram program = VeilRenderSystem.renderer().getShaderManager().getShader(SHADER);
        if (program == null) return;

        frame++;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Vec3 camera = event.getCamera().getPosition();

        long gameTime = level.getGameTime();
        long keyframe = Math.floorDiv(gameTime, CloudDensity.KEYFRAME_TICKS);
        float morph = (Math.floorMod(gameTime, CloudDensity.KEYFRAME_TICKS) + partialTick) / CloudDensity.KEYFRAME_TICKS;

        double driftX = CloudState.driftX(partialTick);
        double driftZ = CloudState.driftZ(partialTick);
        float radius = AWClientConfig.get().cloudRenderDistance;

        collectVisibleTiles(event.getFrustum(), camera, driftX, driftZ, radius, keyframe, morph);
        evictStaleTiles();
        if (VISIBLE.isEmpty()) return;

        setFrameUniforms(program, CloudLighting.compute(level, camera, partialTick), radius);
        vec3(program, "FarFlash", DistantStorm.flashX(), DistantStorm.flashZ(), DistantStorm.flash(partialTick));
        float omen = HailOmen.yellowing(partialTick);
        if (omen > 0f) {
            vec3(program, "CloudTint",
                    CloudState.tintR() + (HailOmen.RED - CloudState.tintR()) * omen,
                    CloudState.tintG() + (HailOmen.GREEN - CloudState.tintG()) * omen,
                    CloudState.tintB() + (HailOmen.BLUE - CloudState.tintB()) * omen);
        }
        draw(program, camera, driftX, driftZ, keyframe, morph, event.getProjectionMatrix());
    }

    private static void collectVisibleTiles(Frustum frustum, Vec3 camera, double driftX, double driftZ,
                                            float radius, long keyframe, float morph) {
        VISIBLE.clear();

        double cloudX = camera.x - driftX;
        double cloudZ = camera.z - driftZ;
        int centerX = Mth.floor(cloudX / CloudTile.SPAN);
        int centerZ = Mth.floor(cloudZ / CloudTile.SPAN);
        int reach = Mth.ceil(radius / CloudTile.SPAN) + 1;
        double keep = radius + CloudTile.SPAN * 0.75;

        double base = CloudState.altitude();
        double top = CloudState.altitude() + CloudState.maxThickness() * 1.6;

        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                int tx = centerX + dx;
                int tz = centerZ + dz;

                double minX = tx * (double) CloudTile.SPAN;
                double minZ = tz * (double) CloudTile.SPAN;
                double centreDx = minX + CloudTile.SPAN * 0.5 - cloudX;
                double centreDz = minZ + CloudTile.SPAN * 0.5 - cloudZ;
                if (centreDx * centreDx + centreDz * centreDz > keep * keep) continue;

                CloudTile tile = TILES.computeIfAbsent(key(tx, tz), k -> new CloudTile(tx, tz));
                tile.lastSeenFrame = frame;
                tile.update(keyframe, morph, CloudState.threshold());

                double worldX = minX + driftX;
                double worldZ = minZ + driftZ;
                AABB bounds = new AABB(worldX, base, worldZ, worldX + CloudTile.SPAN, top, worldZ + CloudTile.SPAN);
                if (tile.renderable() && frustum.isVisible(bounds)) {
                    VISIBLE.add(tile);
                }
            }
        }
    }

    private static void draw(ShaderProgram program, Vec3 camera, double driftX, double driftZ,
                             long keyframe, float morph, Matrix4f projection) {
        ShaderInstance shader = VeilRenderBridge.toShaderInstance(program);
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
        float offsetY = (float) (CloudState.altitude() - camera.y);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(true);
        drawTiles(program, shader, modelView, projection, camera, driftX, driftZ, offsetY, keyframe, morph, false);

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(false);
        drawTiles(program, shader, modelView, projection, camera, driftX, driftZ, offsetY, keyframe, morph, true);

        VertexBuffer.unbind();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void drawTiles(ShaderProgram program, ShaderInstance shader, Matrix4f modelView,
                                  Matrix4f projection, Vec3 camera, double driftX, double driftZ,
                                  float offsetY, long keyframe, float morph, boolean colourPass) {
        for (CloudTile tile : VISIBLE) {
            float offsetX = (float) (tile.tileX * (double) CloudTile.SPAN + driftX - camera.x);
            float offsetZ = (float) (tile.tileZ * (double) CloudTile.SPAN + driftZ - camera.z);
            vec3(program, "TileOffset", offsetX, offsetY, offsetZ);
            uniform(program, "Morph", tile.morph(keyframe, morph));
            uniform(program, "TileFade", tile.fadeIn());
            tile.draw(modelView, projection, shader);
            if (colourPass) tile.countShownFrame();
        }
    }

    private static void setFrameUniforms(ShaderProgram program, CloudLighting light, float radius) {
        uniform(program, "Threshold", CloudState.threshold());
        uniform(program, "MinThickness", CloudState.minThickness());
        uniform(program, "MaxThickness", CloudState.maxThickness());
        uniform(program, "Puff", CloudState.puff());
        uniform(program, "HeightCurve", CloudState.heightCurve());
        uniform(program, "CellSize", CloudDensity.CELL_SIZE);
        uniform(program, "Opacity", CloudState.opacity());
        uniform(program, "FadeStart", radius * 0.45f);
        uniform(program, "FadeEnd", radius * 0.95f);
        uniform(program, "Flash", light.flash());
        vec3(program, "CloudTint", CloudState.tintR(), CloudState.tintG(), CloudState.tintB());
        vec3(program, "LightDir", light.lightDir());
        vec3(program, "LightColor", light.lightColor());
        vec3(program, "AmbientTop", light.ambientTop());
        vec3(program, "AmbientBottom", light.ambientBottom());

        float[] fog = RenderSystem.getShaderFogColor();
        vec3(program, "FogColor", fog[0], fog[1], fog[2]);
    }

    private static void evictStaleTiles() {
        Iterator<CloudTile> it = TILES.values().iterator();
        while (it.hasNext()) {
            CloudTile tile = it.next();
            if (frame - tile.lastSeenFrame > EVICT_AFTER_FRAMES) {
                tile.close();
                it.remove();
            }
        }
    }

    private static void releaseAll() {
        TILES.values().forEach(CloudTile::close);
        TILES.clear();
        VISIBLE.clear();
    }

    private static long key(int tileX, int tileZ) {
        return ((long) tileX << 32) ^ (tileZ & 0xFFFFFFFFL);
    }

    private static void uniform(ShaderProgram program, String name, float value) {
        var u = program.getUniform(name);
        if (u != null) u.setFloat(value);
    }

    private static void vec3(ShaderProgram program, String name, float x, float y, float z) {
        var u = program.getUniform(name);
        if (u != null) u.setVector(x, y, z);
    }

    private static void vec3(ShaderProgram program, String name, Vec3 v) {
        vec3(program, name, (float) v.x, (float) v.y, (float) v.z);
    }
}
