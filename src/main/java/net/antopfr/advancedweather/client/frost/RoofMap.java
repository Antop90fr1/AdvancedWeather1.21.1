package net.antopfr.advancedweather.client.frost;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.function.Predicate;

public final class RoofMap {

    private RoofMap() {}

    public static final int UNIT = 15;
    public static final int SIZE = 256;

    private static final int WIDTH = SIZE * 2;
    private static final int ROWS_PER_FRAME = 4;
    private static final int MAX_DEPTH = 64;
    private static final int STACK_DEPTH = 4;
    private static final int GAP_DEPTH = 24;
    private static final float UNKNOWN = -1.0e9f;
    private static final float LAYERED = 4096f;
    private static final Predicate<BlockState> SHELTERS = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque();

    private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();
    private static final float[] NEAR_QUARTS = new float[SIZE / 4 + 3];
    private static final float[] FAR_QUARTS = new float[SIZE / 4 + 3];
    private static final float[] NEAR_PACES = new float[SIZE / 4 + 3];
    private static final float[] FAR_PACES = new float[SIZE / 4 + 3];
    private static FloatBuffer rows;
    private static int texture;
    private static int nextRow;
    private static ClientLevel trackedLevel;
    private static float roof;
    private static float ceiling;
    private static float floor;

    public static void update(ClientLevel level, double cameraX, double cameraZ) {
        if (texture == 0) create();
        if (level != trackedLevel) {
            trackedLevel = level;
            nextRow = 0;
            clear();
        }

        int baseX = Mth.floor(cameraX) - SIZE / 2;
        int baseZ = Mth.floor(cameraZ) - SIZE / 2;
        int first = nextRow;
        int firstQuartX = Math.floorDiv(baseX - 2, 4);
        boolean columns = GroundCover.columnsKnown();
        rows.clear();
        for (int r = 0; r < ROWS_PER_FRAME; r++) {
            int z = column(baseZ, first + r);
            int row = r * WIDTH * 4;
            int quartZ = Math.floorDiv(z - 2, 4);
            float alongZ = (z - 1.5f - 4 * quartZ) / 4f;
            if (columns) {
                for (int k = 0; k < NEAR_QUARTS.length; k++) {
                    quartSnow(level, firstQuartX + k, quartZ, NEAR_QUARTS, NEAR_PACES, k);
                    quartSnow(level, firstQuartX + k, quartZ + 1, FAR_QUARTS, FAR_PACES, k);
                }
            }
            LevelChunk chunk = null;
            int chunkX = Integer.MIN_VALUE;
            for (int i = 0; i < SIZE; i++) {
                int x = column(baseX, i);
                if (x >> 4 != chunkX) {
                    chunkX = x >> 4;
                    chunk = level.getChunkSource().getChunkNow(chunkX, z >> 4);
                }
                int here = row + i * 4;
                int gap = row + (SIZE + i) * 4;
                if (chunk == null) {
                    texel(here, 0f, UNKNOWN, UNKNOWN, 0f);
                    texel(gap, UNKNOWN, 0f, 0f, UNKNOWN);
                } else {
                    int blocking = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    boolean layered = chunk.getBlockState(CURSOR.set(x, blocking + 1, z)).getBlock() instanceof SnowLayerBlock;
                    scan(level, chunk, x, z, blocking);
                    texel(here, roof, Math.floorDiv(x, SIZE), Math.floorDiv(z, SIZE), blocking + 1 + (layered ? LAYERED : 0f));
                    float snow = 0f;
                    float pace = UNKNOWN;
                    if (columns) {
                        int quartX = Math.floorDiv(x - 2, 4);
                        int k = quartX - firstQuartX;
                        float alongX = (x - 1.5f - 4 * quartX) / 4f;
                        snow = Mth.lerp(alongZ, Mth.lerp(alongX, NEAR_QUARTS[k], NEAR_QUARTS[k + 1]),
                                Mth.lerp(alongX, FAR_QUARTS[k], FAR_QUARTS[k + 1]));
                        pace = Mth.lerp(alongZ, Mth.lerp(alongX, NEAR_PACES[k], NEAR_PACES[k + 1]),
                                Mth.lerp(alongX, FAR_PACES[k], FAR_PACES[k + 1]));
                    }
                    texel(gap, ceiling, floor, snow, pace);
                }
            }
        }
        nextRow = (first + ROWS_PER_FRAME) % SIZE;

        upload(first);
    }

    public static void bind() {
        if (texture == 0) return;
        int previous = GlStateManager._getActiveTexture();
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL13.glActiveTexture(previous);
    }

    private static int column(int base, int index) {
        return base + Math.floorMod(index - base, SIZE);
    }

    private static void quartSnow(ClientLevel level, int quartX, int quartZ, float[] bases, float[] paces, int k) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(quartX >> 2, quartZ >> 2);
        if (chunk == null) {
            bases[k] = GroundCover.snow(1f);
            paces[k] = 0f;
            return;
        }
        int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, quartX * 4 + 2, quartZ * 4 + 2);
        GroundCover.Glide glide = GroundCover.glideOf(chunk.getNoiseBiome(quartX, QuartPos.fromBlock(top), quartZ));
        bases[k] = glide.base();
        paces[k] = glide.pace();
    }

    private static void texel(int index, float r, float g, float b, float a) {
        rows.put(index, r).put(index + 1, g).put(index + 2, b).put(index + 3, a);
    }

    private static void scan(ClientLevel level, LevelChunk chunk, int x, int z, int top) {
        int bottom = Math.max(level.getMinBuildHeight(), top - MAX_DEPTH);
        roof = level.getMinBuildHeight();
        ceiling = UNKNOWN;
        floor = 0f;
        int y = top;
        BlockState state = null;
        for (; y >= bottom; y--) {
            state = chunk.getBlockState(CURSOR.set(x, y, z));
            if (SHELTERS.test(state)) break;
        }
        if (y < bottom) return;
        roof = y + height(level, state);
        int stackEnd = Math.max(bottom, y - STACK_DEPTH);
        int under = y - 1;
        while (under >= stackEnd && SHELTERS.test(chunk.getBlockState(CURSOR.set(x, under, z)))) under--;
        if (under < stackEnd) return;
        ceiling = under + 1;
        int gapEnd = Math.max(level.getMinBuildHeight(), under - GAP_DEPTH);
        for (int g = under - 1; g >= gapEnd; g--) {
            BlockState below = chunk.getBlockState(CURSOR.set(x, g, z));
            if (SHELTERS.test(below)) {
                floor = g + height(level, below);
                return;
            }
        }
        floor = gapEnd;
    }

    private static float height(ClientLevel level, BlockState state) {
        VoxelShape shape = state.getCollisionShape(level, CURSOR);
        return shape.isEmpty() ? 1f : (float) shape.max(Direction.Axis.Y);
    }

    private static void create() {
        rows = MemoryUtil.memAllocFloat(WIDTH * ROWS_PER_FRAME * 4);
        texture = GL11.glGenTextures();
        int previous = GlStateManager._getActiveTexture();
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, WIDTH, SIZE, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) null);
        GL13.glActiveTexture(previous);
        clear();
    }

    private static void clear() {
        FloatBuffer blank = MemoryUtil.memAllocFloat(WIDTH * SIZE * 4);
        try {
            for (int r = 0; r < SIZE; r++) {
                for (int i = 0; i < SIZE; i++) blank.put(0f).put(UNKNOWN).put(UNKNOWN).put(0f);
                for (int i = 0; i < SIZE; i++) blank.put(UNKNOWN).put(0f).put(0f).put(UNKNOWN);
            }
            blank.flip();
            send(0, SIZE, blank);
        } finally {
            MemoryUtil.memFree(blank);
        }
    }

    private static void upload(int firstRow) {
        send(firstRow, ROWS_PER_FRAME, rows);
    }

    private static final int[] UNPACK = {
            GL11.GL_UNPACK_ROW_LENGTH, GL11.GL_UNPACK_SKIP_PIXELS, GL11.GL_UNPACK_SKIP_ROWS, GL11.GL_UNPACK_ALIGNMENT
    };
    private static final int[] UNPACK_VALUES = {0, 0, 0, 4};

    private static void send(int firstRow, int count, FloatBuffer data) {
        int previous = GlStateManager._getActiveTexture();
        int unpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int[] kept = new int[UNPACK.length];
        for (int i = 0; i < UNPACK.length; i++) {
            kept[i] = GL11.glGetInteger(UNPACK[i]);
            GlStateManager._pixelStore(UNPACK[i], UNPACK_VALUES[i]);
        }
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, firstRow, WIDTH, count, GL11.GL_RGBA, GL11.GL_FLOAT, data);
        GL13.glActiveTexture(previous);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
        for (int i = 0; i < UNPACK.length; i++) GlStateManager._pixelStore(UNPACK[i], kept[i]);
    }
}
