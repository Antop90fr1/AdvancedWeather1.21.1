package net.antopfr.advancedweather;

import net.antopfr.advancedweather.compat.create.AWDisplaySources;
import net.antopfr.advancedweather.compat.coldsweat.ColdSweatCompat;
import net.antopfr.advancedweather.compat.sable.SableIntegration;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.content.AWCreativeTab;
import net.antopfr.advancedweather.content.entity.AWEntitySerializers;
import net.antopfr.advancedweather.server.event.CauldronInteractionsEvent;
import net.antopfr.advancedweather.content.fluid.AWFluids;
import net.antopfr.advancedweather.content.AWRecipeSerializers;
import net.antopfr.advancedweather.content.block.AWBlocks;
import net.antopfr.advancedweather.content.AWDataComponents;
import net.antopfr.advancedweather.content.item.AWItems;
import net.antopfr.advancedweather.content.entity.AWEntities;
import net.antopfr.advancedweather.network.*;
import net.antopfr.advancedweather.client.particle.AWParticles;
import net.antopfr.advancedweather.client.sound.AWSounds;
import net.antopfr.advancedweather.server.command.AWCommand;
import net.neoforged.fml.loading.FMLEnvironment;
import net.antopfr.advancedweather.util.ModLoaded;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(AdvancedWeather.MOD_ID)
public class AdvancedWeather {
    public static final String MOD_ID = "advancedweather";

    public AdvancedWeather(IEventBus eb) {
        AWDataComponents.register(eb);
        AWRecipeSerializers.register(eb);
        AWEntitySerializers.register(eb);
        AWCreativeTab.register();
        AWItems.register();
        AWBlocks.register();
        AWEntities.register();
        AWFluids.register();
        AWParticles.register(eb);
        AWSounds.register(eb);

        if (ModLoaded.isCreateLoaded()) {
            eb.addListener(AWDisplaySources::onRegisterEvent);
        }
        if (ModLoaded.isSableLoaded()) {
            SableIntegration.init(eb);
        }

        NeoForge.EVENT_BUS.addListener(AWCommand::onRegisterCommands);
        eb.addListener(AWPayloads::onRegisterPayloads);
        eb.addListener(this::onCommonSetup);

        ModLoaded.logAllCompat();
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        if (FMLEnvironment.dist.isClient()) AWClientConfig.init();
        AWCommonConfig.init();

        event.enqueueWork(CauldronInteractionsEvent::register);

        if (ModLoaded.isCreateLoaded()) {
            event.enqueueWork(AWDisplaySources::associateWithBlock);
        }

        event.enqueueWork(ColdSweatCompat::register);
    }
}
