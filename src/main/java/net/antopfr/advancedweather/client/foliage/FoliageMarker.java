package net.antopfr.advancedweather.client.foliage;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.mixin.client.BakedQuadAccessor;
import net.antopfr.advancedweather.util.AWLogger;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class FoliageMarker {

    private FoliageMarker() {}

    private static final int STRIDE = 8;
    private static final int Y = 1;
    private static final int COLOR = 3;

    private static final int ANCHORED = 253;
    private static final int STEPS = 16;

    private static final float LEAF_WEIGHT = 0.3f;

    private static final int VARIANT_SEEDS = 32;

    private static final Direction[] DIRECTIONS = Direction.values();

    @SubscribeEvent
    public static void onModelsBaked(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        Set<BakedQuad> marked = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Block block : BuiltInRegistries.BLOCK) {
            boolean leaves = block instanceof LeavesBlock;
            if (!leaves && !(block instanceof BushBlock)) continue;

            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (!leaves && !state.getFluidState().isEmpty()) continue;
                BakedModel model = models.get(BlockModelShaper.stateToModelLocation(state));
                if (model == null) continue;
                try {
                    mark(model, state, leaves, marked);
                } catch (RuntimeException e) {
                }
            }
        }
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : models.entrySet()) {
            ResourceLocation id = entry.getKey().id();
            if (!id.getNamespace().equals("eclipticseasons") || !id.getPath().contains("leaves")) continue;
            try {
                mark(entry.getValue(), null, true, marked);
            } catch (RuntimeException e) {
            }
        }
        AWLogger.AW.debug("[AdvancedWeather] Foliage sway: {} quads marked", marked.size());
    }

    private static void mark(BakedModel model, BlockState state, boolean leaves, Set<BakedQuad> marked) {
        int seeds = model instanceof WeightedBakedModel ? VARIANT_SEEDS : 1;
        for (int seed = 0; seed < seeds; seed++) {
            for (int side = -1; side < DIRECTIONS.length; side++) {
                Direction direction = side < 0 ? null : DIRECTIONS[side];
                for (BakedQuad quad : model.getQuads(state, direction, RandomSource.create(seed), ModelData.EMPTY, null)) {
                    if (marked.add(quad)) mark(quad, state, leaves);
                }
            }
        }
    }

    private static void mark(BakedQuad quad, BlockState state, boolean leaves) {
        int[] data = quad.getVertices().clone();
        for (int vertex = 0; vertex + STRIDE <= data.length; vertex += STRIDE) {
            float weight = leaves ? LEAF_WEIGHT : plantWeight(state, Float.intBitsToFloat(data[vertex + Y]));
            int alpha = ANCHORED - Math.round(Mth.clamp(weight, 0f, 1f) * STEPS);
            data[vertex + COLOR] = (data[vertex + COLOR] & 0x00FFFFFF) | (alpha << 24);
        }
        ((BakedQuadAccessor) quad).advancedweather$setVertices(data);
    }

    private static float plantWeight(BlockState state, float y) {
        if (!state.hasProperty(DoublePlantBlock.HALF)) return y;
        return state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? y * 0.5f : 0.5f + y * 0.5f;
    }
}
