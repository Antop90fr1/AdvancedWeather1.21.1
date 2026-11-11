package net.antopfr.advancedweather.content.block.archive;

import com.mojang.serialization.MapCodec;
import net.antopfr.advancedweather.content.block.AWBlocks;
import net.antopfr.advancedweather.content.advancement.AWAdvancements;
import net.antopfr.advancedweather.content.AWDataComponents;
import net.antopfr.advancedweather.content.item.CalibrationToolItem;
import net.antopfr.advancedweather.content.item.almanac.WeatherAlmanacItem;
import net.antopfr.advancedweather.content.WeatherRecord;
import net.antopfr.advancedweather.util.AWTooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WeatherArchiveBlock extends BaseEntityBlock {

    public static final MapCodec<WeatherArchiveBlock> CODEC = simpleCodec(WeatherArchiveBlock::new);

    public WeatherArchiveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }
    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return AWBlocks.WEATHER_ARCHIVE_BE.create(pos, state);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack held, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                       @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (held.getItem() instanceof CalibrationToolItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        if (held.getItem() instanceof WeatherAlmanacItem) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof WeatherArchiveBlockEntity archive) {
                List<WeatherRecord> toIngest = WeatherAlmanacItem.getRecords(held);
                if (toIngest.isEmpty()) {
                    level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6f, 1f);
                    player.displayClientMessage(Component.translatable(
                            "advancedweather.archive.almanac_empty"), true);
                } else {
                    int ok = 0, duplicates = 0, tooOld = 0, full = 0;
                    for (WeatherRecord r : toIngest) {
                        switch (archive.ingest(r)) {
                            case OK -> ok++;
                            case DUPLICATE -> duplicates++;
                            case TOO_OLD -> tooOld++;
                            case FULL -> full++;
                        }
                    }

                    if (ok > 0) {
                        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 0.9f);
                        ((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT,
                                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                                Math.min(6 + ok * 2, 30), 0.25, 0.15, 0.25, 0.4);
                        if (player instanceof ServerPlayer sp) AWAdvancements.grant(sp, "history_in_the_making");
                    } else {
                        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6f, 1f);
                    }

                    player.displayClientMessage(buildIngestSummary(ok, duplicates, tooOld, full), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        WeatherRecord record = held.get(AWDataComponents.WEATHER_RECORD.get());
        if (record == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof WeatherArchiveBlockEntity archive) {
            var result = archive.ingest(record);
            switch (result) {
                case OK -> {
                    held.consume(1, player);
                    level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 0.9f);
                    ((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT,
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.25, 0.15, 0.25, 0.4);
                    if (player instanceof ServerPlayer sp) AWAdvancements.grant(sp, "history_in_the_making");
                }
                case DUPLICATE -> deny(level, pos, player, "advancedweather.archive.duplicate");
                case TOO_OLD  -> deny(level, pos, player, "advancedweather.archive.too_old");
                case FULL     -> deny(level, pos, player, "advancedweather.archive.full");
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void deny(Level level, BlockPos pos, Player player, String key) {
        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6f, 1f);
        player.displayClientMessage(Component.translatable(key), true);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos,
                                                        @NotNull Player player, @NotNull BlockHitResult hit) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof WeatherArchiveBlockEntity archive
                    && level instanceof ServerLevel serverLevel) {
                archive.refreshPrediction(serverLevel);
                if (player instanceof ServerPlayer sp && !archive.getPredictedTop().isEmpty()) {
                    AWAdvancements.grant(sp, "prophet_of_pressure");
                }
            }
        } else if (level.getBlockEntity(pos) instanceof WeatherArchiveBlockEntity archive) {
            WeatherArchiveScreenOpener.open(archive);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        List<ItemStack> drops = super.getDrops(state, params);
        if (be instanceof WeatherArchiveBlockEntity archive && !archive.getRecords().isEmpty()) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof BlockItem bi && bi.getBlock() == this) {
                    drop.set(DataComponents.BLOCK_ENTITY_DATA,
                            CustomData.of(archive.saveWithoutMetadata(params.getLevel().registryAccess())));
                }
            }
        }
        return drops;
    }

    private static Component buildIngestSummary(int ok, int duplicates, int tooOld, int full) {
        MutableComponent summary = Component.empty();
        appendCount(summary, ok, "advancedweather.archive.ingest.archived", ChatFormatting.GREEN);
        appendCount(summary, duplicates, "advancedweather.archive.ingest.duplicates", ChatFormatting.GRAY);
        appendCount(summary, tooOld, "advancedweather.archive.ingest.too_old", ChatFormatting.GOLD);
        appendCount(summary, full, "advancedweather.archive.ingest.full", ChatFormatting.RED);
        return summary;
    }

    private static void appendCount(MutableComponent summary, int count, String key, ChatFormatting colour) {
        if (count <= 0) return;
        if (!summary.getSiblings().isEmpty()) summary.append(Component.literal(" · ").withStyle(ChatFormatting.GRAY));
        summary.append(Component.translatable(key, count).withStyle(colour));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        AWTooltips.append(tooltip, "advancedweather.block_tooltip.weather_archive", 3, null);
    }
}
