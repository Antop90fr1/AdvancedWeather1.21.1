package net.antopfr.advancedweather.client.render;

import net.antopfr.advancedweather.content.block.AWBlocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class ClientModelRegister {
    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        event.register(InstrumentItemRenderer.BAROMETER_BODY);
        event.register(InstrumentItemRenderer.BAROMETER_NEEDLE);
        event.register(InstrumentItemRenderer.THERMOMETER_BODY);
//        event.register(InstrumentRenderer.THERMOMETER_COLUMN);
        event.register(InstrumentItemRenderer.THERMOMETER_NEEDLE);
        event.register(InstrumentItemRenderer.HYGROMETER_BODY);
        event.register(InstrumentItemRenderer.HYGROMETER_NEEDLE);
        event.register(InstrumentItemRenderer.ANEMOMETER_BODY);
        event.register(InstrumentItemRenderer.ANEMOMETER_CUPS);

        event.register(SensorBlockRenderer.NEEDLE);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AWBlocks.BAROMETER_BE.get(), SensorBlockRenderer::new);
        event.registerBlockEntityRenderer(AWBlocks.THERMOMETER_BE.get(), SensorBlockRenderer::new);
        event.registerBlockEntityRenderer(AWBlocks.HYGROMETER_BE.get(), SensorBlockRenderer::new);
        event.registerBlockEntityRenderer(AWBlocks.ANEMOMETER_BE.get(), SensorBlockRenderer::new);
    }
}
