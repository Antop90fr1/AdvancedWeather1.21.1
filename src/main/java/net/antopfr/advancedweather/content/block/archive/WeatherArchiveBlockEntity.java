package net.antopfr.advancedweather.content.block.archive;

import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.content.WeatherRecord;
import net.antopfr.advancedweather.weather.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WeatherArchiveBlockEntity extends BlockEntity {

    public static final int MAX_RECORDS = 500;

    private final List<WeatherRecord> records = new ArrayList<>();


    private static final long MIN_TREND_TICKS = 200;

    private final List<int[]> predictedTop = new ArrayList<>();
    private float confidence = 0f;

    public float getConfidence() {
        return confidence;
    }

    public List<int[]> getPredictedTop() {
        return records.size() >= AWCommonConfig.get().archiveMinRecordsForForecast
                ? predictedTop : List.of();
    }

    public WeatherArchiveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public enum IngestResult { OK, DUPLICATE, TOO_OLD, FULL }

    public IngestResult ingest(WeatherRecord record) {
        AWCommonConfig config = AWCommonConfig.get();
        if (records.size() >= config.archiveMaxRecords) return IngestResult.FULL;
        if (config.archiveRejectOlderReports
                && !records.isEmpty() && record.gameTime() < records.getFirst().gameTime())
            return IngestResult.TOO_OLD;

        boolean duplicate = records.stream().anyMatch(r ->
                r.gameTime() == record.gameTime() && r.pos().equals(record.pos())
                        && r.dimension().equals(record.dimension()));
        if (duplicate) return IngestResult.DUPLICATE;

        records.add(record);
        records.sort(Comparator.comparingLong(WeatherRecord::gameTime));
        setChanged();

        if (level instanceof ServerLevel serverLevel) {
            refreshPrediction(serverLevel);
        }
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return IngestResult.OK;
    }

    public List<WeatherRecord> getRecords() {
        return records;
    }

    private boolean fromHere(WeatherRecord record) {
        return level != null && record.dimension().equals(level.dimension().location());
    }

    private WeatherRecord lastHere() {
        for (int i = records.size() - 1; i >= 0; i--) {
            if (fromHere(records.get(i))) return records.get(i);
        }
        return null;
    }

    private WeatherRecord[] lastThreeWith(java.util.function.Predicate<WeatherRecord> has) {
        WeatherRecord a = null, b = null, c = null;
        for (int i = records.size() - 1; i >= 0; i--) {
            WeatherRecord r = records.get(i);
            if (!has.test(r) || !fromHere(r)) continue;
            if (c == null) c = r;
            else if (b == null) b = r;
            else { a = r; break; }
        }
        return a == null ? null : new WeatherRecord[]{a, b, c};
    }

    private static float extrapolateSeries(WeatherRecord[] abc,
                                           java.util.function.ToDoubleFunction<WeatherRecord> getter, long tNext) {
        WeatherRecord a = abc[0], b = abc[1], c = abc[2];
        long dt1 = Math.max(MIN_TREND_TICKS, b.gameTime() - a.gameTime());
        long dt2 = Math.max(MIN_TREND_TICKS, c.gameTime() - b.gameTime());
        float v1 = (float) getter.applyAsDouble(a);
        float v2 = (float) getter.applyAsDouble(b);
        float v3 = (float) getter.applyAsDouble(c);
        float slope = ((v2 - v1) / dt1 + 2f * (v3 - v2) / dt2) / 3f;
        return v3 + slope * (tNext - c.gameTime());
    }

    private float levelTemperature(WeatherRecord record) {
        return record.temperature() - LocalAtmosphere.temperatureShift(level, record.biome(), record.pos().getY());
    }

    private float levelPressure(WeatherRecord record) {
        return record.pressure() - LocalAtmosphere.pressureShift(record.pos().getY());
    }

    private float levelHumidity(WeatherRecord record) {
        return record.humidity() - LocalAtmosphere.humidityShift(level, record.biome());
    }

    private WeatherRecord projectLevelAir() {
        if (level == null || records.size() < AWCommonConfig.get().archiveMinRecordsForForecast) return null;

        WeatherRecord last = lastHere();
        if (last == null) return null;
        long dtNext = AWCommonConfig.get().archiveForecastHorizonTicks;
        long tNext = last.gameTime() + dtNext;

        int mask = 0;
        float temp = 0f, press = 0f, hum = 0f, wind = 0f;

        DimensionProfile profile = DimensionProfile.of(level);

        WeatherRecord[] t = lastThreeWith(WeatherRecord::hasTemperature);
        if (t != null) {
            mask |= WeatherRecord.MASK_TEMPERATURE;
            temp = Mth.clamp(extrapolateSeries(t, this::levelTemperature, tNext),
                    profile.tMin - 40f, profile.tMax + 40f);
        }

        WeatherRecord[] p = lastThreeWith(WeatherRecord::hasPressure);
        if (p != null) {
            mask |= WeatherRecord.MASK_PRESSURE;
            float margin = (profile.pMax - profile.pMin) * 1.5f + 50f;
            press = Mth.clamp(extrapolateSeries(p, this::levelPressure, tNext),
                    Math.max(0f, profile.pMin - margin), profile.pMax + margin);
        }

        WeatherRecord[] h = lastThreeWith(WeatherRecord::hasHumidity);
        if (h != null) {
            mask |= WeatherRecord.MASK_HUMIDITY;
            hum = Mth.clamp(extrapolateSeries(h, this::levelHumidity, tNext), 0f, 100f);
        }

        WeatherRecord[] w = lastThreeWith(WeatherRecord::hasWind);
        if (w != null) {
            mask |= WeatherRecord.MASK_WIND;
            wind = Mth.clamp(extrapolateSeries(w, WeatherRecord::windIntensity, tNext), 0f, 1f);
        }

        if (mask == 0) return null;

        return new WeatherRecord(tNext, last.dimension(), last.pos(), last.biome(),
                last.stationName(), last.weatherOrdinal(), mask, temp, press, hum, wind);
    }

    public WeatherRecord computeForecastPoint() {
        WeatherRecord air = projectLevelAir();
        if (air == null) return null;

        int y = air.pos().getY();
        return new WeatherRecord(air.gameTime(), air.dimension(), air.pos(), air.biome(), air.stationName(),
                air.weatherOrdinal(), air.sensorMask(),
                air.temperature() + LocalAtmosphere.temperatureShift(level, air.biome(), y),
                air.pressure() + LocalAtmosphere.pressureShift(y),
                Mth.clamp(air.humidity() + LocalAtmosphere.humidityShift(level, air.biome()), 0f, 100f),
                air.windIntensity());
    }

    public void refreshPrediction(ServerLevel serverLevel) {
        int needed = AWCommonConfig.get().archiveMinRecordsForForecast;
        if (records.size() < needed) {
            predictedTop.clear();
            return;
        }

        WeatherRecord projected = projectLevelAir();
        WeatherRecord last = lastHere();

        if (projected == null || !projected.hasPressure() || !projected.hasTemperature()) {
            predictedTop.clear();
            setChanged();
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            return;
        }

        List<WeatherTypes> here = records.stream()
                .filter(this::fromHere)
                .map(WeatherRecord::weatherType)
                .toList();
        List<WeatherTypes> measuredHistory = here.subList(Math.max(0, here.size() - 5), here.size());

        DimensionProfile profile = DimensionProfile.of(serverLevel);
        float normalizedP = Mth.clamp(
                (projected.pressure() - profile.pMin) / (profile.pMax - profile.pMin), 0f, 1f);

        float humidity = projected.hasHumidity() ? projected.humidity() : 50f;

        var prediction = WeatherTransitionGraph.mostLikelyNextFromMeasurements(
                last.weatherType(), normalizedP, projected.temperature(), humidity,
                DimensionProfile.getDimension(serverLevel), serverLevel, measuredHistory);

        float quality = dataQuality();

        var candidates = WeatherTransitionGraph.topCandidates(prediction, 3);
        int k = candidates.size();
        float uniform = k > 0 ? 1f / k : 0f;

        float[] tempered = new float[k];
        float sum = 0f;
        for (int i = 0; i < k; i++) {
            tempered[i] = Mth.lerp(quality, uniform, candidates.get(i).probability());
            sum += tempered[i];
        }
        confidence = k > 0 ? tempered[0] : 0f;

        predictedTop.clear();
        for (int i = 0; i < k; i++) {
            float p = sum > 0 ? tempered[i] / sum : uniform;
            predictedTop.add(new int[]{ candidates.get(i).type().ordinal(), Math.round(p * 1000f) });
        }

        setChanged();
        serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public long dataAge() {
        if (records.isEmpty() || level == null) return Long.MAX_VALUE;
        return level.getDayTime() - records.getLast().gameTime();
    }

    private float dataQuality() {
        int n = records.size();
        int floor = AWCommonConfig.get().archiveConfidenceFloorRecords;
        int sat = AWCommonConfig.get().archiveConfidenceSaturationRecords;
        if (n < floor) return 0f;
        float t = Mth.clamp((float)(n - floor) / (sat - floor), 0f, 1f);
        float curved = 1f - (1f - t) * (1f - t);        // ease-out
        return Mth.clamp(0.15f + curved * 0.80f, 0f, 0.95f);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (WeatherRecord r : records) {
            WeatherRecord.CODEC.encodeStart(NbtOps.INSTANCE, r)
                    .result().ifPresent(list::add);
        }
        tag.put("Records", list);
        int[] flat = new int[predictedTop.size() * 2];
        for (int i = 0; i < predictedTop.size(); i++) {
            flat[i * 2] = predictedTop.get(i)[0];
            flat[i * 2 + 1] = predictedTop.get(i)[1];
        }
        tag.putIntArray("PredictedTop", flat);
        tag.putFloat("Confidence", confidence);

    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        records.clear();
        ListTag list = tag.getList("Records", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            WeatherRecord.CODEC.parse(NbtOps.INSTANCE, t)
                    .result().ifPresent(records::add);
        }
        records.sort(Comparator.comparingLong(WeatherRecord::gameTime));
        predictedTop.clear();
        int[] loaded = tag.getIntArray("PredictedTop");
        for (int i = 0; i + 1 < loaded.length; i += 2) {
            predictedTop.add(new int[]{loaded[i], loaded[i + 1]});
        }
        confidence = tag.getFloat("Confidence");
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = new CompoundTag();

        int[] flat = new int[predictedTop.size() * 2];
        for (int i = 0; i < predictedTop.size(); i++) {
            flat[i * 2] = predictedTop.get(i)[0];
            flat[i * 2 + 1] = predictedTop.get(i)[1];
        }
        tag.putIntArray("PredictedTop", flat);
        tag.putFloat("Confidence", confidence);
        tag.putInt("TotalRecords", records.size());

        List<WeatherRecord> sample = sampleForSync(records, 1500);
        ListTag list = new ListTag();
        for (WeatherRecord r : sample) {
            WeatherRecord.CODEC.encodeStart(NbtOps.INSTANCE, r).result().ifPresent(list::add);
        }
        tag.put("Records", list);
        return tag;
    }

    private static List<WeatherRecord> sampleForSync(List<WeatherRecord> all, int maxCount) {
        if (all.size() <= maxCount) return all;
        List<WeatherRecord> out = new ArrayList<>(maxCount);
        double step = (double) all.size() / maxCount;
        for (double i = 0; i < all.size(); i += step) {
            out.add(all.get((int) i));
        }
        if (out.getLast() != all.getLast()) {
            out.add(all.getLast());
        }
        return out;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
