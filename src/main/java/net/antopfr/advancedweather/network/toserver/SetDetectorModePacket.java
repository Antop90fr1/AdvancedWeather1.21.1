package net.antopfr.advancedweather.network.toserver;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.content.block.detector.WeatherDetectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetDetectorModePacket(BlockPos pos, WeatherDetectorBlock.DetectionMode mode, boolean early)
        implements CustomPacketPayload {

    public static final Type<SetDetectorModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "set_detector_mode"));

    public static final StreamCodec<FriendlyByteBuf, SetDetectorModePacket> CODEC = StreamCodec.of(
            (buf, pkt) -> { buf.writeBlockPos(pkt.pos); buf.writeEnum(pkt.mode); buf.writeBoolean(pkt.early); },
            buf -> new SetDetectorModePacket(buf.readBlockPos(),
                    buf.readEnum(WeatherDetectorBlock.DetectionMode.class), buf.readBoolean()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SetDetectorModePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player().level() instanceof ServerLevel level)) return;
            if (!pkt.pos().closerToCenterThan(ctx.player().position(), 8.0)) return;
            BlockState state = level.getBlockState(pkt.pos());
            if (!(state.getBlock() instanceof WeatherDetectorBlock)) return;

            level.setBlock(pkt.pos(), state.setValue(WeatherDetectorBlock.MODE, pkt.mode())
                    .setValue(WeatherDetectorBlock.EARLY, pkt.early()), 3);
            level.scheduleTick(pkt.pos(), state.getBlock(), 1);
        });
    }
}
