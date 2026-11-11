package net.antopfr.advancedweather.weather.wind;

public final class GustNoise {

    private GustNoise() {}

    public static final int PERIOD = 256;
    private static final int MASK = PERIOD - 1;

    public static float sample(double x, double z) {
        long fx = (long) Math.floor(x);
        long fz = (long) Math.floor(z);
        float tx = (float) (x - fx);
        float tz = (float) (z - fz);

        int x0 = (int) (fx & MASK);
        int z0 = (int) (fz & MASK);
        int x1 = (x0 + 1) & MASK;
        int z1 = (z0 + 1) & MASK;

        float u = fade(tx);
        float v = fade(tz);

        float a = lattice(x0, z0);
        float b = lattice(x1, z0);
        float c = lattice(x0, z1);
        float d = lattice(x1, z1);

        float top = a + (b - a) * u;
        float bottom = c + (d - c) * u;
        return top + (bottom - top) * v;
    }

    static float lattice(int ix, int iz) {
        int h = ix * 0x27D4EB2D ^ iz * 0x165667B1;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return (h >>> 8) * (1f / 16777216f);
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }
}
