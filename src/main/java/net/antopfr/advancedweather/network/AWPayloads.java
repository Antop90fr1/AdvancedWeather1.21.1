package net.antopfr.advancedweather.network;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.network.toclient.*;
import net.antopfr.advancedweather.network.toserver.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class AWPayloads {
    public static void registerAllPayloads(PayloadRegistrar r) {
        registerClientPayloads(r);
        registerServerPayloads(r);
    }

    public static void registerClientPayloads(PayloadRegistrar r) {
        r.playToClient(WeatherSyncPacket.TYPE, WeatherSyncPacket.CODEC, WeatherSyncPacket::handle);
        r.playToClient(PressureSyncPacket.TYPE, PressureSyncPacket.CODEC, PressureSyncPacket::handle);
        r.playToClient(WindSyncPacket.TYPE, WindSyncPacket.CODEC, WindSyncPacket::handle);
        r.playToClient(RainbowSyncPacket.TYPE, RainbowSyncPacket.CODEC, RainbowSyncPacket::handle);
        r.playToClient(GroundCoverSyncPacket.TYPE, GroundCoverSyncPacket.CODEC, GroundCoverSyncPacket::handle);
        r.playToClient(StormApproachPacket.TYPE, StormApproachPacket.CODEC, StormApproachPacket::handle);
        r.playToClient(TemperatureSyncPacket.TYPE, TemperatureSyncPacket.CODEC, TemperatureSyncPacket::handle);
        r.playToClient(HumiditySyncPacket.TYPE, HumiditySyncPacket.CODEC, HumiditySyncPacket::handle);
        r.playToClient(EffectSyncPacket.TYPE, EffectSyncPacket.CODEC, EffectSyncPacket::handle);
        r.playToClient(HistorySyncPacket.TYPE, HistorySyncPacket.CODEC, HistorySyncPacket::handle);
        r.playToClient(TransitionProbabilitiesPacket.TYPE, TransitionProbabilitiesPacket.CODEC, TransitionProbabilitiesPacket::handle);
        r.playToClient(ToggleDebugPacket.TYPE, ToggleDebugPacket.CODEC, ToggleDebugPacket::handle);
        r.playToClient(FlashPacket.TYPE, FlashPacket.CODEC, FlashPacket::handle);
        r.playToClient(HighlightSensorsPacket.TYPE, HighlightSensorsPacket.CODEC, HighlightSensorsPacket::handle);
    }

    public static void registerServerPayloads(PayloadRegistrar r) {
        r.playToServer(SetDetectorModePacket.TYPE, SetDetectorModePacket.CODEC, SetDetectorModePacket::handle);
        r.playToServer(SetStationNamePacket.TYPE, SetStationNamePacket.CODEC, SetStationNamePacket::handle);
        r.playToServer(SetSamplerIntervalPacket.TYPE, SetSamplerIntervalPacket.CODEC, SetSamplerIntervalPacket::handle);
        r.playToServer(CommitCalibrationPacket.TYPE, CommitCalibrationPacket.CODEC, CommitCalibrationPacket::handle);
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(AdvancedWeather.MOD_ID);
        AWPayloads.registerAllPayloads(registrar);
    }
}
