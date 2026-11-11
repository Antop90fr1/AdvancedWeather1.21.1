package net.antopfr.advancedweather.client.cloud;

import net.antopfr.advancedweather.util.OpenSimplex2;

import java.util.Arrays;

final class CloudDensity {

    private CloudDensity() {}

    static final float CELL_SIZE = 4f;

    static final int KEYFRAME_TICKS = 1200;

    private static final long SEED = 0xC10D5EEDL;
    private static final double SCALE = 1.0 / 80.0;
    private static final double LUMP_SCALE = 1.0 / 28.0;
    private static final double EVOLUTION = 0.22;

    private static final int CDF_SAMPLES = 16384;
    private static final float[] CDF = buildCdf();

    static float density(int cellX, int cellZ, long keyframe) {
        float raw = raw((cellX + 0.5) * CELL_SIZE, (cellZ + 0.5) * CELL_SIZE, keyframe * EVOLUTION);
        int index = Arrays.binarySearch(CDF, raw);
        if (index < 0) index = -index - 1;
        return index / (float) CDF_SAMPLES;
    }

    static float variation(int cellX, int cellZ) {
        double x = (cellX + 0.5) * CELL_SIZE * LUMP_SCALE;
        double z = (cellZ + 0.5) * CELL_SIZE * LUMP_SCALE;
        float smooth = OpenSimplex2.noise2(SEED + 71, x, z) * 0.5f + 0.5f;
        return Math.max(0f, Math.min(1f, smooth));
    }

    private static float raw(double blockX, double blockZ, double t) {
        double x = blockX * SCALE;
        double z = blockZ * SCALE;

        double wx = OpenSimplex2.noise3_ImproveXZ(SEED + 11, x * 0.45, t * 0.5, z * 0.45) * 0.55;
        double wz = OpenSimplex2.noise3_ImproveXZ(SEED + 23, x * 0.45, t * 0.5, z * 0.45) * 0.55;

        double broad = OpenSimplex2.noise3_ImproveXZ(SEED, x + wx, t, z + wz);
        double medium = OpenSimplex2.noise3_ImproveXZ(SEED + 37, (x + wx) * 2.13, t * 1.6, (z + wz) * 2.13);
        double fine = OpenSimplex2.noise3_ImproveXZ(SEED + 59, x * 4.41, t * 2.3, z * 4.41);
        return (float) (broad * 0.56 + medium * 0.29 + fine * 0.15);
    }

    private static float[] buildCdf() {
        float[] samples = new float[CDF_SAMPLES];
        for (int i = 0; i < CDF_SAMPLES; i++) {
            double x = unit(i * 0x5BD1E995 + 1) * 60000.0;
            double z = unit(i * 0x1B873593 + 2) * 60000.0;
            double t = unit(i * 0x68E31DA4 + 3) * 64 * EVOLUTION;
            samples[i] = raw(x, z, t);
        }
        Arrays.sort(samples);
        return samples;
    }

    private static float unit(int h) {
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return (h >>> 8) * (1f / 16777216f);
    }
}
