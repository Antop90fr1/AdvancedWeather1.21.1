package net.antopfr.advancedweather.client.cloud;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.antopfr.advancedweather.util.AWLogger;
import net.minecraft.Util;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

final class CloudTile {

    static final int CELLS = 32;
    static final float SPAN = CELLS * CloudDensity.CELL_SIZE;

    private static final long NONE = Long.MIN_VALUE;
    private static boolean failureLogged;
    private static final int FADE_IN_FRAMES = 40;

    private static final float FLOOR_MARGIN = 0.15f;
    private static final float REBUILD_MARGIN = 0.03f;
    private static final float NOTHING_BUILT = 2f;

    private static final int BLUR = 3;
    private static final int PADDED = CELLS + 2 * BLUR;

    private record Mesh(long keyframe, float floor, @Nullable MeshData data, ByteBufferBuilder memory) {
        void free() {
            if (data != null) data.close();
            memory.close();
        }
    }

    final int tileX;
    final int tileZ;
    long lastSeenFrame;

    private VertexBuffer current;
    private VertexBuffer next;
    private long currentKey = NONE;
    private long nextKey = NONE;
    private boolean currentEmpty;
    private boolean nextEmpty;
    private float currentFloor = NOTHING_BUILT;
    private float nextFloor = NOTHING_BUILT;
    private int framesShown;

    private CompletableFuture<Mesh> pending;

    CloudTile(int tileX, int tileZ) {
        this.tileX = tileX;
        this.tileZ = tileZ;
    }

    void update(long keyframe, float morph, float threshold) {
        if (pending != null && pending.isDone()) {
            CompletableFuture<Mesh> done = pending;
            pending = null;
            try {
                accept(done.join(), keyframe);
            } catch (CompletionException e) {
                if (!failureLogged) {
                    failureLogged = true;
                    AWLogger.AW.error("[AdvancedWeather] Cloud tile build failed", e.getCause());
                }
            }
        }

        if (nextKey != NONE && nextKey <= keyframe) {
            VertexBuffer spare = current;
            current = next;
            currentKey = nextKey;
            currentEmpty = nextEmpty;
            currentFloor = nextFloor;
            next = spare;
            nextKey = NONE;
            nextFloor = NOTHING_BUILT;
        }

        if (pending == null) {
            float floor = threshold - FLOOR_MARGIN;
            if (currentKey != keyframe || threshold - currentFloor < REBUILD_MARGIN) {
                schedule(keyframe, floor);
            } else if (morph > 0.5f && (nextKey != keyframe + 1 || threshold - nextFloor < REBUILD_MARGIN)) {
                schedule(keyframe + 1, floor);
            }
        }
    }

    private void schedule(long keyframe, float floor) {
        int tx = tileX;
        int tz = tileZ;
        pending = CompletableFuture.supplyAsync(() -> build(tx, tz, keyframe, floor), Util.backgroundExecutor());
    }

    private void accept(Mesh mesh, long frameKeyframe) {
        if (mesh.keyframe() > frameKeyframe) {
            if (next == null) next = new VertexBuffer(VertexBuffer.Usage.STATIC);
            nextEmpty = upload(next, mesh);
            nextKey = mesh.keyframe();
            nextFloor = mesh.floor();
        } else if (currentKey == NONE || mesh.keyframe() > currentKey
                || (mesh.keyframe() == currentKey && mesh.floor() < currentFloor)) {
            if (current == null) current = new VertexBuffer(VertexBuffer.Usage.STATIC);
            currentEmpty = upload(current, mesh);
            currentKey = mesh.keyframe();
            currentFloor = mesh.floor();
        } else {
            mesh.free();
        }
    }

    private static boolean upload(VertexBuffer target, Mesh mesh) {
        if (mesh.data() == null) {
            mesh.memory().close();
            return true;
        }
        target.bind();
        target.upload(mesh.data());
        VertexBuffer.unbind();
        mesh.memory().close();
        return false;
    }

    boolean renderable() {
        return currentKey != NONE && !currentEmpty;
    }

    float morph(long keyframe, float morph) {
        if (currentKey == keyframe) return morph;
        return currentKey < keyframe ? 1f : 0f;
    }

    float fadeIn() {
        return Math.min(1f, framesShown / (float) FADE_IN_FRAMES);
    }

    void draw(Matrix4f modelView, Matrix4f projection, ShaderInstance shader) {
        current.bind();
        current.drawWithShader(modelView, projection, shader);
    }

    void countShownFrame() {
        if (framesShown < FADE_IN_FRAMES) framesShown++;
    }

    void close() {
        if (current != null) current.close();
        if (next != null) next.close();
        current = next = null;
        if (pending != null) {
            pending.thenAccept(Mesh::free);
            pending = null;
        }
    }

    private static Mesh build(int tileX, int tileZ, long keyframe, float floor) {
        VertexFormat format = DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL;
        ByteBufferBuilder memory = new ByteBufferBuilder(256 * 24 * format.getVertexSize());
        BufferBuilder buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, format);

        float[] densityA = densities(tileX, tileZ, keyframe);
        float[] densityB = densities(tileX, tileZ, keyframe + 1);
        float[] bodyA = soften(densityA);
        float[] bodyB = soften(densityB);

        float s = CloudDensity.CELL_SIZE;
        for (int i = 0; i < CELLS; i++) {
            for (int j = 0; j < CELLS; j++) {
                float a = densityA[(j + BLUR) * PADDED + i + BLUR];
                float b = densityB[(j + BLUR) * PADDED + i + BLUR];
                if (Math.max(a, b) < floor) continue;
                float swell = CloudDensity.variation(tileX * CELLS + i, tileZ * CELLS + j);
                column(buffer, i * s, j * s, (i + 1) * s, (j + 1) * s,
                        Math.round(a * 255f), Math.round(b * 255f),
                        Math.round(bodyA[j * CELLS + i] * 255f), Math.round(bodyB[j * CELLS + i] * 255f), swell);
            }
        }
        return new Mesh(keyframe, floor, buffer.build(), memory);
    }

    private static float[] densities(int tileX, int tileZ, long keyframe) {
        float[] grid = new float[PADDED * PADDED];
        for (int j = 0; j < PADDED; j++) {
            for (int i = 0; i < PADDED; i++) {
                grid[j * PADDED + i] = CloudDensity.density(tileX * CELLS + i - BLUR, tileZ * CELLS + j - BLUR, keyframe);
            }
        }
        return grid;
    }

    private static float[] soften(float[] grid) {
        float norm = 1f / ((BLUR + 1) * (BLUR + 1));

        float[] rows = new float[PADDED * CELLS];
        for (int j = 0; j < PADDED; j++) {
            for (int i = 0; i < CELLS; i++) {
                float sum = 0f;
                for (int k = -BLUR; k <= BLUR; k++) {
                    sum += (BLUR + 1 - Math.abs(k)) * grid[j * PADDED + i + BLUR + k];
                }
                rows[j * CELLS + i] = sum * norm;
            }
        }

        float[] out = new float[CELLS * CELLS];
        for (int j = 0; j < CELLS; j++) {
            for (int i = 0; i < CELLS; i++) {
                float sum = 0f;
                for (int k = -BLUR; k <= BLUR; k++) {
                    sum += (BLUR + 1 - Math.abs(k)) * rows[(j + BLUR + k) * CELLS + i];
                }
                out[j * CELLS + i] = sum * norm;
            }
        }
        return out;
    }

    private static void column(BufferBuilder b, float x0, float z0, float x1, float z1,
                               int da, int db, int ba, int bb, float var) {
        vertex(b, x0, 1, z0, 1, 1, da, db, ba, bb, var, 0, 1, 0);
        vertex(b, x0, 1, z1, 1, -1, da, db, ba, bb, var, 0, 1, 0);
        vertex(b, x1, 1, z1, -1, -1, da, db, ba, bb, var, 0, 1, 0);
        vertex(b, x1, 1, z0, -1, 1, da, db, ba, bb, var, 0, 1, 0);
        vertex(b, x0, 0, z0, 1, 1, da, db, ba, bb, var, 0, -1, 0);
        vertex(b, x1, 0, z0, -1, 1, da, db, ba, bb, var, 0, -1, 0);
        vertex(b, x1, 0, z1, -1, -1, da, db, ba, bb, var, 0, -1, 0);
        vertex(b, x0, 0, z1, 1, -1, da, db, ba, bb, var, 0, -1, 0);
        vertex(b, x0, 0, z0, 1, 1, da, db, ba, bb, var, 0, 0, -1);
        vertex(b, x0, 1, z0, 1, 1, da, db, ba, bb, var, 0, 0, -1);
        vertex(b, x1, 1, z0, -1, 1, da, db, ba, bb, var, 0, 0, -1);
        vertex(b, x1, 0, z0, -1, 1, da, db, ba, bb, var, 0, 0, -1);
        vertex(b, x1, 0, z1, -1, -1, da, db, ba, bb, var, 0, 0, 1);
        vertex(b, x1, 1, z1, -1, -1, da, db, ba, bb, var, 0, 0, 1);
        vertex(b, x0, 1, z1, 1, -1, da, db, ba, bb, var, 0, 0, 1);
        vertex(b, x0, 0, z1, 1, -1, da, db, ba, bb, var, 0, 0, 1);
        vertex(b, x0, 0, z1, 1, -1, da, db, ba, bb, var, -1, 0, 0);
        vertex(b, x0, 1, z1, 1, -1, da, db, ba, bb, var, -1, 0, 0);
        vertex(b, x0, 1, z0, 1, 1, da, db, ba, bb, var, -1, 0, 0);
        vertex(b, x0, 0, z0, 1, 1, da, db, ba, bb, var, -1, 0, 0);
        vertex(b, x1, 0, z0, -1, 1, da, db, ba, bb, var, 1, 0, 0);
        vertex(b, x1, 1, z0, -1, 1, da, db, ba, bb, var, 1, 0, 0);
        vertex(b, x1, 1, z1, -1, -1, da, db, ba, bb, var, 1, 0, 0);
        vertex(b, x1, 0, z1, -1, -1, da, db, ba, bb, var, 1, 0, 0);
    }

    private static void vertex(BufferBuilder b, float x, float y, float z, float towardsX, float towardsZ,
                               int densityA, int densityB, int bodyA, int bodyB, float swell,
                               float nx, float ny, float nz) {
        b.addVertex(x, y, z)
                .setUv(towardsX * (1f + swell), towardsZ)
                .setColor(densityA, densityB, bodyA, bodyB)
                .setNormal(nx, ny, nz);
    }
}
